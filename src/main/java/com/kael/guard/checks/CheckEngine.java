package com.kael.guard.checks;

import com.kael.guard.KaelorvynGuard;
import com.kael.guard.Settings;
import com.kael.guard.data.PlayerData;
import com.kael.guard.data.PlayerDataManager;
import com.kael.guard.physics.MovementSimulator;
import com.kael.guard.physics.VanillaPhysics;
import com.kael.guard.stats.StatsUtil;
import com.kael.guard.util.RayTracer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CheckEngine {

    public static final Set<String> ORES = Set.of(
            "DIAMOND_ORE", "DEEP_SLATE_DIAMOND_ORE", "EMERALD_ORE", "DEEP_SLATE_EMERALD_ORE",
            "GOLD_ORE", "DEEP_SLATE_GOLD_ORE", "IRON_ORE", "DEEP_SLATE_IRON_ORE",
            "COPPER_ORE", "DEEP_SLATE_COPPER_ORE", "LAPIS_ORE", "DEEP_SLATE_LAPIS_ORE",
            "REDSTONE_ORE", "DEEP_SLATE_REDSTONE_ORE", "COAL_ORE", "DEEP_SLATE_COAL_ORE",
            "NETHER_QUARTZ_ORE", "NETHER_GOLD_ORE", "ANCIENT_DEBRIS");

    private static final int[][] FACE_OFFSETS = {
            {0, 1, 0}, {0, -1, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}
    };

    private final KaelorvynGuard plugin;
    private final PlayerDataManager manager;
    private final Settings s;
    private int moveCounter;

    public CheckEngine(KaelorvynGuard plugin, PlayerDataManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.s = plugin.settings();
    }

    public void handleMove(Player player, PlayerData d, double x, double y, double z,
                           float yaw, float pitch, boolean onGround, boolean hasPos, boolean hasLook, long now) {
        long interval = 0;
        if (d.lastMoveTime > 0) {
            interval = now - d.lastMoveTime;
            if (interval > 0 && interval < 500) d.moveIntervalsMs.addLast(interval);
            while (d.moveIntervalsMs.size() > 200) d.moveIntervalsMs.removeFirst();
        }
        // 第一个移动包没有上一帧基准，直接建立坐标并跳过全部判定
        if (d.lastMoveTime == 0) {
            if (hasPos) {
                d.lastX = x;
                d.lastY = y;
                d.lastZ = z;
            }
            if (hasLook) {
                d.lastYaw = yaw;
                d.lastPitch = pitch;
            }
            d.lastMoveTime = now;
            return;
        }
        // 进服宽限期：只同步坐标，不做任何移动判定，避免出生点/传送包误报
        if (s.joinGraceMs() > 0 && now - d.joinTime < s.joinGraceMs()) {
            if (hasPos) {
                d.lastX = x;
                d.lastY = y;
                d.lastZ = z;
            }
            if (hasLook) {
                d.lastYaw = yaw;
                d.lastPitch = pitch;
            }
            d.lastMoveTime = now;
            return;
        }
        if (hasLook) {
            d.lookPacketsThisTick++;
            float dyaw = normalize(yaw - d.lastYaw);
            float dpitch = pitch - d.lastPitch;
            if (Math.abs(dyaw) > 0.001f) d.yawDeltas.addLast(dyaw);
            if (Math.abs(dpitch) > 0.001f) d.yawDeltas.addLast(dpitch);
            while (d.yawDeltas.size() > 300) d.yawDeltas.removeFirst();
            // 思路来源: GrimAC AimModulo360 (GPL-3.0, DefineOutside) — 静默旋转 %360 大跳变
            if (s.activeDetectionEnabled() && s.aimSnapEnabled() && Math.abs(dyaw) > s.snapYaw()
                    && Math.abs(d.lastYawDelta) < s.snapPrior()
                    && !recentlyTeleported(d)) {
                emit(player, d, "AIM_SNAP", 0.8, "旋转瞬移（疑似静默瞄准）", Map.of("dyaw", dyaw));
            }
            d.lastYawDelta = dyaw;
            d.lastYaw = yaw;
            d.lastPitch = pitch;
        }
        double dx = hasPos ? x - d.lastX : 0;
        double dz = hasPos ? z - d.lastZ : 0;
        double dy = hasPos ? y - d.lastY : 0;
        double horizontal = Math.hypot(dx, dz);

        // 主动检测关闭时只保留轻量原始行为采集，举报复核可读取这些数据。
        // 这里提前返回也避免 PacketEvents 网络线程继续访问 Bukkit 世界对象。
        if (!s.activeDetectionEnabled()) {
            recordPassiveMove(d, x, y, z, onGround, hasPos, now, interval, dy, horizontal);
            return;
        }

        // 按每 tick 速度判断“传送级位移”：鞘翅/冰船/激流等官方长距离移动不会超过阈值；
        // 只有服务器授权传送（lastTeleport/重生/击退/载具/滑翔/坐标接近服务器位置）才豁免
        double jumpSpeed = Math.max(Math.abs(perTick(dy, interval)), perTick(horizontal, interval));
        if (jumpSpeed > s.teleportJumpBlocks()) {
            plugin.signatureEngine().onMove(player, d, dy, onGround, interval);
            Location server = player.getLocation();
            boolean nearServer = hasPos && server.distanceSquared(
                    new Location(player.getWorld(), x, y, z))
                    <= s.teleportJumpBlocks() * s.teleportJumpBlocks();
            if (recentlyTeleported(d) || recentlyRespawned(d) || recentlyKnocked(d)
                    || d.vehicle || d.gliding || nearServer) {
                d.lastTeleport = now;
                if (hasPos) {
                    d.lastX = x;
                    d.lastY = y;
                    d.lastZ = z;
                }
            } else {
                if (hasPos) {
                    d.lastX = server.getX();
                    d.lastY = server.getY();
                    d.lastZ = server.getZ();
                }
                plugin.onCandidate(player, CheckResult.of("TELEPORT", 0.85,
                        "客户端位置跳变（非服务器传送）",
                        Map.of("dy", dy, "horizontal", horizontal)));
            }
            d.lastMoveTime = now;
            return;
        }
        d.lastDy = dy;
        d.recentMoveDeltas.addLast(new double[]{now, interval > 0 ? perTick(dy, interval) : dy, onGround ? 1.0 : 0.0});
        while (!d.recentMoveDeltas.isEmpty() && now - d.recentMoveDeltas.getFirst()[0] > 200) {
            d.recentMoveDeltas.removeFirst();
        }

        d.claimedOnGround = onGround;
        if (onGround) {
            d.groundTicks++;
            d.airTicks = 0;
            d.slowFallTicks = 0;
        } else {
            d.airTicks++;
            d.groundTicks = 0;
        }
        // 水中/游泳不算空气 tick；刚出水瞬间仍受水的浮力影响
        boolean inLiquid = player.isInWater()
                || player.getLocation().getBlock().isLiquid()
                || player.getLocation().getBlock().getRelative(0, -1, 0).isLiquid()
                || player.getLocation().getBlock().getRelative(0, 1, 0).isLiquid()
                || player.getEyeLocation().getBlock().isLiquid();
        if (inLiquid || d.water || d.swimming) {
            d.lastWaterTime = now;
            d.airTicks = 0;
            d.slowFallTicks = 0;
        }
        // 梯子/藤蔓/脚手架是合法攀爬，不算悬空，也不累计空气 tick
        if (d.climbing || isClimbing(player)) {
            d.airTicks = 0;
            d.slowFallTicks = 0;
            d.ascentTicks = 0;
            d.ascentTotal = 0;
        }
        d.flyingPacketsSinceTickEnd++;
        d.lastHorizontal = horizontal;

        plugin.signatureEngine().onMove(player, d, dy, onGround, interval);

        // 思路来源: Wurst StepHack 包序 — 同 tick 双上升包
        if (s.stepEnabled() && d.recentMoveDeltas.size() >= 2) {
            double[][] arr = d.recentMoveDeltas.toArray(new double[0][]);
            if (arr.length >= 2) {
                double[] prev = arr[arr.length - 2];
                double[] cur = arr[arr.length - 1];
            if (now - prev[0] < s.stepMaxIntervalMs() && cur[1] > s.stepMinDy() && prev[1] > s.stepMinDy()
                    && cur[1] + prev[1] > s.stepMinSum()
                    && (!s.stepRequireGround() || (prev[2] > 0.5 && cur[2] > 0.5))
                    && !recentlyTeleported(d) && !recentlyRespawned(d)
                    && !d.vehicle && !d.gliding && !d.flying
                    && !d.water && !d.swimming && !d.climbing && !player.isInWater()) {
                emit(player, d, "STEP", 0.85, "同 tick 双上升包（Step 包序）",
                        Map.of("dy1", prev[1], "dy2", cur[1]));
            }
            }
        }

        // 注意：Timer 检查必须使用上一帧时间，所以 lastMoveTime 最后再更新
        if (s.flightEnabled()) checkFlight(player, d, dy, onGround, horizontal, now, interval);
        if (s.slowFallEnabled()) checkSlowFall(player, d, dy, interval);
        if (s.speedEnabled()) checkSpeed(player, d, horizontal, interval);
        if (s.noFallEnabled()) checkNoFall(player, d);
        if (s.phaseEnabled()) checkPhase(player, d);
        if (s.timerEnabled()) checkTimer(player, d, now);
        if (s.aimEnabled()) checkAim(player, d);
        if (s.elytraEnabled()) checkElytra(player, d, horizontal, dy, interval);
        if (s.vehicleEnabled()) checkVehicle(player, d, horizontal, interval);
        if (s.climbEnabled()) checkClimb(player, d, dy, interval);
        if (s.fastFallEnabled()) checkFastFall(player, d, dy, interval);
        if (s.blinkEnabled()) checkBlink(player, d, interval);
        if (s.knockbackEnabled()) checkKnockback(player, d, horizontal);
        if (s.airJumpEnabled()) checkAirJump(player, d, dy, onGround, now, interval);
        if (s.spiderEnabled()) checkSpider(player, d, dy, horizontal, interval);
        if (s.jesusEnabled()) checkJesus(player, d, dy, interval, now);
        if (s.noSlowdownEnabled()) checkNoSlowdown(player, d, horizontal, interval);
        // 1.21+ 攻击冷却会让玩家快速连点时产生大量挥动包但没有攻击包，
        // 因此洪水统计只看真实攻击/背包点击/挖掘包，挥动包不再计入。
        if (s.packetSpamEnabled() && d.attacksThisTick + d.clicksThisTick + d.breaksThisTick > s.packetSpamMax()) {
            emit(player, d, "PACKET_SPAM", 0.8, "单 tick 封包洪水",
                    Map.of("flood", d.attacksThisTick + d.clicksThisTick + d.breaksThisTick));
        }
        if (s.lookPacketsEnabled() && d.lookPacketsThisTick > s.lookPacketsMax()) {
            emit(player, d, "BAD_PACKETS", 0.6, "单 tick 多次旋转包", Map.of("looks", d.lookPacketsThisTick));
        }
        // 思路来源: Intave 攻击包延迟复检 — 下一帧用新命中盒重新验证
        if (s.attackRecheckEnabled() && d.queuedAttackEntityId != -1 && now - d.queuedAttackTime >= s.attackRecheckDelayMs()) {
            checkReach(player, d, d.queuedAttackEntityId, true);
            d.queuedAttackEntityId = -1;
        }
        if (hasLook && Math.abs(pitch) > 90 && !recentlyTeleported(d)) {
            emit(player, d, "BAD_PACKETS", 0.7, "非法视角 pitch", Map.of("pitch", pitch));
        }

        if (hasPos) {
            d.lastX = x;
            d.lastY = y;
            d.lastZ = z;
        }
        d.lastMoveTime = now;

        if (++moveCounter % s.antibodyScanEveryMoves() == 0 && s.antibodyEnabled()) {
            CheckResult match = plugin.antibodyLibrary().match(d.featureVector());
            if (match != null) plugin.onCandidate(player, match);
        }
        // 思路来源: Intave backtrack 延迟 z-score — 延迟尖峰疑似实体位置回滚
        if (s.backtrackEnabled() && moveCounter % s.backtrackScanEveryMoves() == 0
                && d.pingSamples.size() >= s.backtrackMinSamples()) {
            List<Integer> pings = List.copyOf(d.pingSamples);
            double mean = StatsUtil.mean(pings);
            double std = StatsUtil.stdDev(pings, mean);
            if (std > s.backtrackMinStd()) {
                double zScore = (d.ping - mean) / std;
                if (zScore > s.backtrackZScore()) {
                    // ping 尖峰受网络波动影响大，只作低置信记录，不作为踢出依据
                    emit(player, d, "BACKTRACK", 0.5, "延迟异常尖峰（疑似回滚）", Map.of("z", zScore, "ping", d.ping));
                }
            }
        }
    }

    // ---------- 移动类 ----------

    private void checkFlight(Player player, PlayerData d, double dy, boolean onGround,
                             double horizontal, long now, long interval) {
        if (interval <= 0) return;
        if (d.flying || d.gliding || d.vehicle || d.creative || d.water
                || d.climbing || isClimbing(player)) return;
        // 浮在水面/半身入水会长时间垂直不动，不是悬空外挂
        if (player.isInWater() || player.getLocation().getBlock().isLiquid()
                || player.getLocation().getBlock().getRelative(0, -1, 0).isLiquid()
                || player.getEyeLocation().getBlock().isLiquid()) return;
        // 刚出水瞬间仍受水的浮力影响，1 秒内不判缓降
        if (System.currentTimeMillis() - d.lastWaterTime < s.slowFallWaterExitGraceMs()) return;
        if (recentlyTeleported(d) || recentlyRespawned(d) || recentlyKnocked(d)) return;
        if (now - d.lastFlightEmit < s.flightEmitCooldownMs()) return;
        double dyTick = perTick(dy, interval);
        double hTick = perTick(horizontal, interval);
        double maxJump = MovementSimulator.jumpVelocity(d.jumpAmp);
        // 原版玩家走上台阶/楼梯时单 tick 合法位移约 0.5-0.6（步高 0.6），
        // 上限取“配置的最大上升”和“跳跃极限”的较大值，避免楼梯误报
        double maxUp = Math.max(s.maxUpSpeed(), maxJump * s.flightUpRatio());
        if (dyTick > maxUp) {
            emit(player, d, "FLIGHT", 0.9, "上升速度超出跳跃极限", Map.of("dy", dyTick, "max", maxUp));
            d.lastFlightEmit = now;
            return;
        }
        if (onGround) {
            d.ascentTicks = 0;
            d.ascentTotal = 0;
            return;
        }
        if (d.airTicks <= s.minAirTicks()) return;
        if (dyTick > s.flightAscentMinDy()) {
            d.ascentTicks++;
            d.ascentTotal += dyTick;
            if (d.ascentTicks >= s.flightAscentMinTicks()) {
                emit(player, d, "FLIGHT", 0.9, "持续上升无飞行权限",
                        Map.of("ticks", d.ascentTicks, "avgDy", d.ascentTotal / d.ascentTicks));
                d.ascentTicks = 0;
                d.ascentTotal = 0;
                d.lastFlightEmit = now;
            }
        } else if (dyTick < s.flightAscentResetDy()) {
            d.ascentTicks = 0;
            d.ascentTotal = 0;
        }
        if (d.airTicks > s.minAirTicks() + s.flightAirMoveExtra()
                && hTick > s.flightAirMoveMinSpeed() && dyTick > s.flightAirMoveMinDy()) {
            emit(player, d, "FLIGHT", 0.85, "持续空中悬浮移动（无飞行权限）",
                    Map.of("airTicks", d.airTicks, "speed", hTick));
            d.lastFlightEmit = now;
        }
        if (Math.abs(dyTick) < s.flightHoverMaxDy() && d.airTicks > s.minAirTicks() + s.flightHoverExtra()) {
            emit(player, d, "FLIGHT", 0.75, "长时间悬空", Map.of("airTicks", d.airTicks));
            d.lastFlightEmit = now;
        }
    }

    // 思路来源: Wurst Glide — 非缓降药水缓慢下落
    private void checkSlowFall(Player player, PlayerData d, double dy, long interval) {
        if (interval <= 0) return;
        if (d.gliding || d.vehicle || d.water || d.swimming || d.climbing
                || isClimbing(player) || d.flying || d.creative) {
            d.slowFallTicks = 0;
            return;
        }
        // 半身入水/浮在水面也会减速下落，不能当缓降外挂
        if (player.isInWater() || player.getLocation().getBlock().isLiquid()
                || player.getLocation().getBlock().getRelative(0, -1, 0).isLiquid()
                || player.getLocation().getBlock().getRelative(0, 1, 0).isLiquid()
                || player.getEyeLocation().getBlock().isLiquid()) {
            d.lastWaterTime = System.currentTimeMillis();
            d.slowFallTicks = 0;
            return;
        }
        // 刚出水瞬间仍受水的浮力影响，宽限期内不判缓降
        if (System.currentTimeMillis() - d.lastWaterTime < s.slowFallWaterExitGraceMs()) {
            d.slowFallTicks = 0;
            return;
        }
        if (recentlyTeleported(d) || recentlyRespawned(d) || recentlyKnocked(d)) return;
        double dyTick = perTick(dy, interval);
        if (d.airTicks <= s.slowFallMinAirTicks() || d.slowFalling) {
            d.slowFallTicks = 0;
            return;
        }
        if (dyTick >= s.slowFallMaxDy() || dyTick < s.slowFallMinDy()) {
            d.slowFallTicks = 0;
            return;
        }
        d.slowFallTicks++;
        if (d.slowFallTicks < s.slowFallMinTicks()) return;
        long now = System.currentTimeMillis();
        if (now - d.lastSlowFallEmit < s.slowFallEmitCooldownMs()) return;
        emit(player, d, "SLOW_FALL", 0.8, "缓慢下落（非缓降药水）",
                Map.of("dy", dyTick, "airTicks", d.airTicks, "consecutiveTicks", d.slowFallTicks));
        d.lastSlowFallEmit = now;
        d.slowFallTicks = 0;
    }

    private void checkSpeed(Player player, PlayerData d, double horizontal, long interval) {
        if (interval <= 0 || interval > s.speedIntervalMaxMs()) return; // 卡顿合并位移不算
        if (d.flying || d.gliding || d.vehicle || d.creative) return;
        if (recentlyTeleported(d) || recentlyRespawned(d) || recentlyKnocked(d)) return;
        double hTick = perTick(horizontal, interval);
        double limit = MovementSimulator.horizontalLimit(d, s.speedTolerance());
        if (!d.claimedOnGround) limit *= s.speedAirMultiplier(); // 空中动量容差，防疾跑跳误报
        if (hTick > limit && hTick > s.speedMinHorizontal()) {
            emit(player, d, "SPEED", Math.min(0.95, 0.55 + (hTick - limit) / limit),
                    "水平速度超出移动包络", Map.of("speed", hTick, "limit", limit));
        }
    }

    private void checkNoFall(Player player, PlayerData d) {
        if (!d.claimedOnGround) return;
        if (d.flying || d.gliding || d.vehicle || d.creative) return;
        if (recentlyTeleported(d) || recentlyRespawned(d) || recentlyKnocked(d)) return;
        if (MovementSimulator.isNearGround(player)) return;
        float fall = player.getFallDistance();
        if (fall > s.maxFallDistance()) {
            emit(player, d, "NO_FALL", 0.85, "下落中谎报落地", Map.of("fallDistance", fall));
        }
    }

    private void checkPhase(Player player, PlayerData d) {
        if (d.vehicle || d.creative || d.flying || d.gliding) return;
        if (recentlyTeleported(d) || recentlyRespawned(d) || recentlyKnocked(d)) return;
        Block b = player.getLocation().getBlock();
        // 只用完整实心方块判断穿墙，光源/台阶/楼梯等不算
        boolean solid = s.phaseOnlyFullBlocks() ? b.getType().isOccluding() : b.getType().isCollidable();
        if (solid && !b.isLiquid()) {
            emit(player, d, "PHASE", 0.8, "位置与方块碰撞重叠", Map.of("block", b.getType().name()));
        }
    }

    // 思路来源: GrimAC ElytraA + Meteor ElytraBoost/ElytraFly
    private void checkElytra(Player player, PlayerData d, double horizontal, double dy, long interval) {
        if (interval <= 0) return;
        if (!d.gliding) return;
        double hTick = perTick(horizontal, interval);
        double dyTick = perTick(dy, interval);
        ItemStack chest = player.getInventory().getChestplate();
        boolean hasElytra = chest != null && chest.getType() == Material.ELYTRA;
        if (!hasElytra) {
            boolean empty = chest == null || chest.getType().isAir();
            if (empty || s.strictNoItem()) {
                emit(player, d, "ELYTRA_NO_ITEM", 0.95, "无鞘翅装备却进入滑翔",
                        Map.of("chest", chest == null ? "none" : chest.getType().name()));
            }
            // 兼容 ElytraFusion/ArmoredElytra：非空但非原版鞘翅的胸甲不判违规
            if (!s.strictNoItem()) return;
        }
        if (hTick > s.elytraMaxSpeed()) {
            emit(player, d, "ELYTRA_FLY", 0.85, "鞘翅速度超出极限", Map.of("speed", hTick));
        }
        if (dyTick > s.elytraBoostMax()) {
            emit(player, d, "ELYTRA_BOOST", 0.8, "鞘翅上升速度异常（疑似无烟花加速）", Map.of("dy", dyTick));
        }
    }

    private void checkVehicle(Player player, PlayerData d, double horizontal, long interval) {
        if (interval <= 0) return;
        if (!d.vehicle) return;
        double hTick = perTick(horizontal, interval);
        if (hTick <= s.vehicleMaxSpeed()) return;
        emit(player, d, "VEHICLE_FLY", 0.8, "载具速度异常", Map.of("speed", hTick));
    }

    private void checkClimb(Player player, PlayerData d, double dy, long interval) {
        if (interval <= 0) return;
        if (!d.climbing) return;
        double dyTick = perTick(dy, interval);
        if (dyTick <= s.climbMaxSpeed()) return;
        emit(player, d, "FAST_CLIMB", 0.8, "攀爬速度异常", Map.of("dy", dyTick));
    }

    private void checkFastFall(Player player, PlayerData d, double dy, long interval) {
        if (interval <= 0 || interval > s.fastFallMaxIntervalMs()) return; // 卡顿合并位移不算
        if (recentlyTeleported(d) || recentlyRespawned(d) || recentlyKnocked(d)) return;
        if (d.gliding || d.vehicle || d.water || d.climbing || isClimbing(player) || d.flying) return;
        if (d.airTicks <= s.fastFallMinAirTicks()) return;
        double fallTick = perTick(-dy, interval);
        if (fallTick < s.fastFallMax()) return;
        emit(player, d, "FAST_FALL", 0.8, "下落速度异常",
                Map.of("dy", dy, "intervalMs", interval, "blocksPerTick", fallTick));
    }

    // 思路来源: GrimAC Timer/TickTimer (GPL-3.0, DefineOutside) — 事务/时钟式 Timer
    private void checkTimer(Player player, PlayerData d, long now) {
        if (d.lastMoveTime <= 0) return;
        long expected = d.lastMoveTime + 50;
        if (now < expected - s.timerDriftMs()) {
            d.timerBalance++;
            d.countEvent("timer");
            if (d.timerBalance >= s.timerBalanceThreshold()) {
                emit(player, d, "TIMER", 0.8, "移动包频率超过 20tps", Map.of("balance", d.timerBalance));
                d.timerBalance = 0;
            }
        } else {
            d.timerBalance = Math.max(0, d.timerBalance - 1);
        }
        long interval = now - d.lastMoveTime;
        if (interval < s.timerMinIntervalMs()) {
            d.burstPackets++;
            if (d.burstPackets > s.burstPackets()) {
                emit(player, d, "TIMER", 0.75, "封包突发", Map.of("burst", d.burstPackets));
                d.burstPackets = 0;
            }
        } else {
            d.burstPackets = Math.max(0, d.burstPackets - 1);
        }
    }

    // 思路来源: GrimAC AimDuplicateLook/Aim (GPL-3.0, DefineOutside) + AnGuard GCD 特征
    private void checkAim(Player player, PlayerData d) {
        if (d.yawDeltas.size() < s.aimMinSamples()) return;
        List<Float> samples = List.copyOf(d.yawDeltas);
        double gcd = StatsUtil.gcd(samples);
        double mean = StatsUtil.mean(samples);
        double std = StatsUtil.stdDev(samples, mean);
        if (gcd < s.aimGcdMax() && std < s.aimStdMax()) {
            emit(player, d, "AIM", 0.7, "旋转 GCD 异常（疑似锁头）", Map.of("gcd", gcd, "std", std));
        }
    }

    // 思路来源: GrimAC Blink / FakeLag 对抗 — 战斗期间长时间静默后突然恢复
    private void checkBlink(Player player, PlayerData d, long interval) {
        if (interval < s.blinkMinSilenceMs()) return;
        if (System.currentTimeMillis() - d.lastAttackTime > s.blinkAttackRecentMs()) return;
        long now = System.currentTimeMillis();
        if (now - d.lastBlinkEmit < s.blinkEmitCooldownMs()) return;
        emit(player, d, "BLINK", 0.65, "战斗期间长时间无移动包后恢复", Map.of("silenceMs", interval));
        d.lastBlinkEmit = now;
    }

    // 思路来源: GrimAC KnockbackHandler (GPL-3.0) — 收到击退后必须产生位移
    private void checkKnockback(Player player, PlayerData d, double horizontal) {
        if (d.expectedKnockbackUntil <= 0) return;
        long now = System.currentTimeMillis();
        while (!d.externalImpulseTimes.isEmpty() && now - d.externalImpulseTimes.getFirst() > 750) {
            d.externalImpulseTimes.removeFirst();
        }
        // 爆炸、旋风人和多个实体同时攻击会叠加冲量，不能套用单次击退模型。
        if (!d.externalImpulseTimes.isEmpty()) {
            d.kbWeakTicks = 0;
            return;
        }
        if (now <= d.expectedKnockbackUntil || now - d.lastVelocityTime > s.knockbackTimeoutMs()) return;
        if (d.flying || d.vehicle || d.gliding) return;
        if (d.expectedKbSpeed < s.knockbackMinKbSpeed()) return; // 击退力太弱不判
        double resistance = 0;
        try {
            var attr = player.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
            if (attr != null) resistance = attr.getValue();
        } catch (Exception ignored) {
        }
        // 服务器下发的击退速度已含抗性减免，再按官方阻力换算实际应位移
        double drag = d.claimedOnGround ? VanillaPhysics.GROUND_FRICTION
                : VanillaPhysics.AIR_DRAG_HORIZONTAL;
        double expectedMove = d.expectedKbSpeed * drag;
        double minExpected = Math.max(s.knockbackMinMove(),
                expectedMove * s.knockbackMinRatio());
        if (d.airTicks >= 2 && horizontal < minExpected) {
            d.kbWeakTicks++;
            if (d.kbWeakTicks >= s.knockbackWeakTicks()) {
                emit(player, d, "NO_VELOCITY", 0.7, "收到击退后位移不足",
                        Map.of("airTicks", d.airTicks, "minExpected", minExpected,
                                "knockbackResistance", resistance, "drag", drag));
                d.expectedKnockbackUntil = 0;
                d.kbWeakTicks = 0;
            }
        } else {
            d.kbWeakTicks = 0;
        }
    }

    // 思路来源: Meteor AirJump — 空中连续跳跃脉冲
    private void checkAirJump(Player player, PlayerData d, double dy, boolean onGround,
                              long now, long interval) {
        if (interval <= 0) return;
        if (onGround) {
            d.airJumpPulses = 0;
            return;
        }
        if (d.water || d.climbing || isClimbing(player) || d.flying || d.gliding || d.vehicle) return;
        if (d.airTicks <= s.airJumpMinAirTicks()) return;
        double dyTick = perTick(dy, interval);
        double jump = MovementSimulator.jumpVelocity(d.jumpAmp);
        if (dyTick > jump * s.airJumpRatio()) {
            d.airJumpPulses++;
            d.lastAirJumpPulse = now;
            if (d.airJumpPulses > s.airJumpPulses() && now - d.lastAirJumpPulse < s.airJumpPulseWindowMs()) {
                emit(player, d, "AIR_JUMP", 0.85, "空中二次跳跃", Map.of("pulses", d.airJumpPulses));
                d.airJumpPulses = 0;
            }
        }
    }

    // 思路来源: Wurst Spider — 贴墙持续上升
    private void checkSpider(Player player, PlayerData d, double dy, double horizontal, long interval) {
        if (interval <= 0) return;
        if (recentlyTeleported(d) || recentlyRespawned(d) || recentlyKnocked(d)) return;
        if (!d.claimedOnGround && !d.water && !d.climbing && !isClimbing(player)
                && !d.flying && !d.gliding && !d.vehicle) {
            double hTick = perTick(horizontal, interval);
            double dyTick = perTick(dy, interval);
            if (hTick < s.spiderMaxHorizontal() && dyTick > s.spiderMinDy() && dyTick < s.spiderMaxDy()) {
                d.spiderTicks++;
                if (d.spiderTicks >= s.spiderTicks()) {
                    emit(player, d, "SPIDER", 0.8, "贴墙持续上升", Map.of("ticks", d.spiderTicks));
                    d.spiderTicks = 0;
                }
            } else {
                d.spiderTicks = 0;
            }
        }
    }

    // 思路来源: Wurst Jesus — 液面上连续跳跃 / 站在液体上谎报落地
    private void checkJesus(Player player, PlayerData d, double dy, long interval, long now) {
        if (interval <= 0) return;
        if (d.vehicle || d.flying || d.gliding || d.creative || d.swimming) return;
        // 眼睛已没入液体 = 潜水/沉底，不是水面行走
        if (player.getEyeLocation().getBlock().isLiquid()) {
            d.jesusPulses = 0;
            return;
        }
        // 身体已泡在水里 = 正常游泳/液面连跳，不是 Jesus 外挂
        if (d.water) {
            d.jesusPulses = 0;
            return;
        }
        if (recentlyTeleported(d) || recentlyRespawned(d) || recentlyKnocked(d)) return;
        Block feet = player.getLocation().getBlock();
        Block below = feet.getRelative(0, -1, 0);
        if (!feet.isLiquid() && !below.isLiquid()) {
            d.jesusPulses = 0;
            return;
        }
        if (below.getType().isSolid() || below.getType().isOccluding()) {
            d.jesusPulses = 0; // 浅水正常走路
            return;
        }
        if (d.claimedOnGround && d.airTicks > s.jesusAirTicks()) {
            d.jesusPulses = 0;
            emit(player, d, "JESUS", 0.85, "站在液体上谎报落地", Map.of("block", feet.getType().name()));
            return;
        }
        if (d.airTicks <= s.jesusAirTicks()) return;
        double jump = MovementSimulator.jumpVelocity(d.jumpAmp);
        double dyTick = perTick(dy, interval);
        if (dyTick <= jump * s.jesusJumpRatio()) return;
        if (d.jesusPulses == 0 || now - d.jesusFirstPulseTime > s.jesusPulseWindowMs()) {
            d.jesusPulses = 1;
            d.jesusFirstPulseTime = now;
        } else {
            d.jesusPulses++;
        }
        if (d.jesusPulses >= s.jesusPulses()) {
            emit(player, d, "JESUS", 0.8, "液面连续跳跃（Jesus 包序）",
                    Map.of("pulses", d.jesusPulses, "airTicks", d.airTicks));
            d.jesusPulses = 0;
        }
    }

    // 思路来源: Wurst NoSlowdown — 使用物品时仍全速移动
    private void checkNoSlowdown(Player player, PlayerData d, double horizontal, long interval) {
        if (interval <= 0) return;
        if (!d.usingItem || !d.sprinting || d.vehicle || d.flying || d.gliding) return;
        double hTick = perTick(horizontal, interval);
        double limit = MovementSimulator.horizontalLimit(d, s.speedTolerance());
        if (hTick > limit * s.noSlowdownRatio()) {
            emit(player, d, "NO_SLOWDOWN", 0.7, "使用物品时未减速", Map.of("speed", hTick, "limit", limit));
        }
    }

    // ---------- 战斗类 ----------

    public void handleAttack(Player player, PlayerData d, int entityId, long now) {
        d.attacksThisTick++;
        d.countEvent("attack");
        if (d.lastAttackTime > 0) {
            long interval = now - d.lastAttackTime;
            if (interval > 1 && interval < 1000) d.attackIntervals.addLast(interval);
            while (d.attackIntervals.size() > 200) d.attackIntervals.removeFirst();
        }
        d.attackTimes.addLast(now);
        while (!d.attackTimes.isEmpty() && now - d.attackTimes.getFirst() > 5000) {
            d.attackTimes.removeFirst();
        }
        d.lastAttackTime = now;
        d.targetTimes.put(entityId, now);

        if (!s.activeDetectionEnabled()) return;

        // 思路来源: Intave AttackReduceIgnoreHeuristic — 挖矿中攻击
        if (s.attackWhileMiningEnabled() && now - d.lastDigStartTime < s.attackWhileMiningMs()) {
            emit(player, d, "ATTACK_WHILE_MINING", 0.55, "挖矿开始后立即攻击",
                    Map.of("ms", now - d.lastDigStartTime));
        }
        // 盾牌格挡是原版合法操作（block-hitting），必须排除
        if (d.usingItem && !player.isBlocking()) {
            emit(player, d, "BAD_PACKETS", 0.55, "使用物品期间攻击", Map.of());
        }
        if (s.attackRecheckEnabled()) {
            d.queuedAttackEntityId = entityId;
            d.queuedAttackTime = now;
        }

        // 思路来源: Wurst Criticals 包序 — 攻击前微偏移移动包
        if (s.criticalsEnabled() && player.isOnGround()
                && d.lastHorizontal < s.criticalsMaxHorizontal()) {
            long cutoff = now - s.criticalsWindowMs();
            int tiny = 0;
            for (double[] m : d.recentMoveDeltas) {
                if (m[0] >= cutoff && Math.abs(m[1]) < s.criticalsTinyMaxDy()) tiny++;
            }
            if (tiny >= s.criticalsMoves()) {
                // 包序只是低置信线索，最终以服务端 isCritical 实际伤害判定为准
                emit(player, d, "CRITICALS", 0.5, "攻击前微偏移移动包（Criticals 包序）", Map.of("tinyMoves", tiny));
            }
        }

        // 思路来源: Meteor KillAura maxTargets — 短时间内攻击多个目标
        if (s.multiTargetEnabled() && d.targetTimes.size() > s.multiTargetMax()) {
            emit(player, d, "MULTI_AURA", 0.75, "短时间内攻击多个目标", Map.of("targets", d.targetTimes.size()));
        }

        // 思路来源: Intave NoSwingHeuristic — 攻击无挥动
        if (now - d.lastSwingTime > s.noSwingAttackWindowMs()) {
            d.noSwingAttacks++;
            if (s.noSwingEnabled() && d.noSwingAttacks > s.noSwingAttackThreshold()) {
                emit(player, d, "NO_SWING", 0.6, "多次攻击无挥动", Map.of("count", d.noSwingAttacks));
            }
        }

        // 思路来源: Intave ToolSwitchHeuristic / AutoWeapon 相关性
        if (d.lastHeldSlotTime > 0 && now - d.lastHeldSlotTime < s.autoSwitchWindowMs()) {
            d.countEvent("autoswitch");
            int auto = d.eventCount("autoswitch");
            if (auto >= s.autoSwitchEmitEvery() && auto % s.autoSwitchEmitEvery() == 0) {
                emit(player, d, "AUTO_SWITCH", 0.65, "攻击瞬间自动切刀", Map.of("count", auto));
            }
        }

        if (s.reachEnabled()) checkReach(player, d, entityId, false);
        if (s.auraEnabled()) checkAura(player, d, entityId);
        if (s.clickerEnabled()) checkClicker(player, d);
    }

    // 思路来源: GrimAC Reach (GPL-3.0) + ALICE 命中盒历史回放
    private void checkReach(Player player, PlayerData d, int entityId, boolean recheck) {
        Entity target = findEntity(player, entityId);
        if (target == null) return;
        double allowance = Math.min(d.ping * s.pingFactor(), s.pingCap());
        BoundingBox box = manager.getHitbox(entityId, d.ping);
        if (box == null) box = target.getBoundingBox();
        box = box.expand(allowance);
        Location eye = player.getEyeLocation();
        Vector closest = closestPoint(eye.toVector(), box);
        double distance = eye.toVector().distance(closest);
        d.reachSamples.addLast(distance);
        while (d.reachSamples.size() > 100) d.reachSamples.removeFirst();
        double max = s.baseReach() + s.reachMargin() + allowance;
        if (d.protocolVersion > 0 && d.protocolVersion < 107) max += s.legacyReachMargin();
        if (distance > max) {
            d.countEvent("reach");
            emit(player, d, "REACH", 0.85, "攻击距离超出合法上限" + (recheck ? "（延迟复检）" : ""),
                    Map.of("distance", distance, "max", max));
        } else if (s.throughWallsEnabled() && distance <= max + s.reachThroughWallsMargin()
                && RayTracer.blocked(player.getWorld(), eye.toVector(), target.getLocation().toVector())) {
            emit(player, d, "THROUGH_WALLS", 0.9, "隔墙攻击", Map.of("distance", distance));
        }
    }

    private void checkAura(Player player, PlayerData d, int entityId) {
        Entity target = findEntity(player, entityId);
        if (target == null) return;
        Vector look = player.getEyeLocation().getDirection().normalize();
        Vector to = target.getLocation().toVector().subtract(player.getEyeLocation().toVector()).normalize();
        double angle = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, look.dot(to)))));
        if (angle > s.auraMaxAngle()) {
            emit(player, d, "KILL_AURA", 0.7, "攻击角度与朝向严重不符", Map.of("angle", angle));
        }
    }

    // 思路来源: Intave ClickPatterns (Deviation/Entropy/Repetitive) + ALICE 统计引擎
    private void checkClicker(Player player, PlayerData d) {
        analyzeClicks(player, d, "AUTO_CLICKER", d.attackIntervals, d.attackTimes);
        analyzeClicks(player, d, "AUTO_CLICKER_RIGHT", d.useTimes, d.useTimesStamps);
    }

    private void analyzeClicks(Player player, PlayerData d, String category,
                               Deque<Long> intervals, Deque<Long> times) {
        long cutoff = System.currentTimeMillis() - s.clickerWindowMs();
        long cps = times.stream().filter(t -> t >= cutoff).count();
        if (cps < s.clickerMinSamples()) return;
        List<Long> recentIntervals = intervals.stream()
                .filter(t -> t >= s.clickerMinIntervalMs() && t <= 1000).toList();
        double mean = StatsUtil.mean(recentIntervals);
        double std = StatsUtil.stdDev(recentIntervals, mean);
        double cv = mean > 0 ? std / mean : 0;
        double entropy = StatsUtil.entropy(recentIntervals, s.clickerBinMs());
        int repeat = maxRepeatBucket(intervals, s.clickerBinMs());
        if (cps > s.clickerMaxCps()) {
            emit(player, d, category, 0.75, "点击节奏异常（疑似连点器）",
                    Map.of("cps", cps, "cv", cv, "entropy", entropy, "repeat", repeat));
        }
    }

    private int maxRepeatBucket(Deque<Long> intervals, int bin) {
        Map<Long, Integer> freq = new HashMap<>();
        for (long t : intervals) freq.merge(Math.round(t / (double) bin), 1, Integer::sum);
        return freq.values().stream().max(Integer::compareTo).orElse(0);
    }

    public void handleSwing(Player player, PlayerData d, long now) {
        d.swingsThisTick++;
        d.countEvent("swing");
        d.lastSwingTime = now;
        d.noSwingAttacks = 0;
        // 挥动包数量本身不是异常：高版本攻击冷却下正常连点也会产生大量挥动包。
    }

    public void handleUse(Player player, PlayerData d, long now) {
        if (d.lastUseTime > 0) {
            long interval = now - d.lastUseTime;
            if (interval > 1 && interval < 1000) d.useTimes.addLast(interval);
            while (d.useTimes.size() > 200) d.useTimes.removeFirst();
        }
        d.useTimesStamps.addLast(now);
        while (!d.useTimesStamps.isEmpty() && now - d.useTimesStamps.getFirst() > 5000) {
            d.useTimesStamps.removeFirst();
        }
        d.lastUseTime = now;
    }

    // ---------- 世界类 ----------

    public void handleHeldSlot(Player player, PlayerData d, int slot) {
        d.lastHeldSlotTime = System.currentTimeMillis();
        if (!s.activeDetectionEnabled()) {
            if (slot >= 0 && slot <= 8) d.lastHeldSlot = slot;
            return;
        }
        if (slot < 0 || slot > 8) {
            emit(player, d, "BAD_PACKETS", 0.7, "非法热键栏槽位", Map.of("slot", slot));
            return;
        }
        if (slot == d.lastHeldSlot) {
            emit(player, d, "BAD_PACKETS", 0.6, "重复发送相同槽位", Map.of("slot", slot));
        }
        d.lastHeldSlot = slot;
    }

    public void handleDigStart(Player player, PlayerData d, double x, double y, double z, long now) {
        d.breaksThisTick++;
        d.countEvent("break");
        d.breakStartTime = now;
        d.breakStartX = x;
        d.breakStartY = y;
        d.breakStartZ = z;
        d.lastDigStartTime = now;
        if (!s.activeDetectionEnabled()) return;
        // 思路来源: GrimAC PacketOrder — 放置后立即挖掘同位置（放置宏）
        if (d.lastPlaceTime > 0 && now - d.lastPlaceTime < s.placeDigWindowMs()
                && (int) Math.floor(x) == d.lastPlaceX && (int) Math.floor(y) == d.lastPlaceY
                && (int) Math.floor(z) == d.lastPlaceZ) {
            emit(player, d, "PACKET_ORDER", 0.6, "放置后立即挖掘同位置（放置宏）", Map.of());
        }
        // 思路来源: GrimAC Breaking.AirLiquidBreak
        if (s.airLiquidBreakEnabled()) {
            Block target = player.getWorld().getBlockAt((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
            if (target.getType().isAir() || target.isLiquid()) {
                emit(player, d, "AIR_LIQUID_BREAK", 0.8, "挖掘空气/液体方块", Map.of("block", target.getType().name()));
            }
        }
        if (s.multiBreakEnabled() && d.breaksThisTick > s.multiBreakMax()) {
            emit(player, d, "MULTI_BREAK", 0.8, "单 tick 多方块破坏", Map.of("breaks", d.breaksThisTick));
        }
    }

    // 思路来源: GrimAC Breaking (FastBreak/NoSwing) + XRay 行为统计
    public void handleDigEnd(Player player, PlayerData d, long now) {
        if (d.breakStartTime <= 0) return;
        long duration = now - d.breakStartTime;
        d.breakTimes.addLast(duration);
        while (d.breakTimes.size() > 100) d.breakTimes.removeFirst();
        // 挖掘速度/是否挖空气等不再用客户端挖掘包判定，
        // 实际破坏结果由 ResultCheckListener 的 BlockBreakEvent 统计。
        d.breakStartTime = -1;
    }

    // 思路来源: GrimAC Scaffolding + Meteor Scaffold (airPlace/脚下放置)
    public void handlePlace(Player player, PlayerData d, long now) {
        handlePlace(player, d, now, (int) Math.floor(d.lastX), (int) Math.floor(d.lastY), (int) Math.floor(d.lastZ), "UP");
    }

    public void handlePlace(Player player, PlayerData d, long now, int x, int y, int z, String face) {
        d.countEvent("place");
        handleUse(player, d, now);
        d.placesThisTick++;
        if (s.multiPlaceEnabled() && d.placesThisTick > s.multiPlaceMax()) {
            emit(player, d, "MULTI_PLACE", 0.7, "单 tick 多次放置", Map.of("places", d.placesThisTick));
        }
        d.lastPlaceTime = now;
        d.lastPlaceX = x;
        d.lastPlaceY = y;
        d.lastPlaceZ = z;
        d.placeTimes.addLast(now);
        while (d.placeTimes.size() > 100) d.placeTimes.removeFirst();
        if (!s.activeDetectionEnabled()) return;
        long cutoff = now - s.placeWindowMs();
        long total = d.placeTimes.stream().filter(t -> t >= cutoff).count();
        boolean nearFeet = y < d.lastY && Math.abs(x - d.lastX) < s.scaffoldNearDistance()
                && Math.abs(z - d.lastZ) < s.scaffoldNearDistance();
        if (nearFeet) {
            d.scaffoldPlaces.addLast(now);
            while (d.scaffoldPlaces.size() > 100) d.scaffoldPlaces.removeFirst();
            long near = d.scaffoldPlaces.stream().filter(t -> t >= cutoff).count();
            if (s.scaffoldEnabled() && near > s.scaffoldMaxPlaces()) {
                emit(player, d, "SCAFFOLD", 0.8, "脚下连续放置", Map.of("placesPerSecond", near));
                return;
            }
        }
        if (s.scaffoldEnabled() && total > s.scaffoldMaxPlaces()) {
            emit(player, d, "SCAFFOLD", 0.8, "放置速率异常", Map.of("placesPerSecond", total));
        }
        if (s.fastPlaceEnabled() && total > s.fastPlaceMax()) {
            emit(player, d, "FAST_PLACE", 0.75, "快速放置", Map.of("placesPerSecond", total));
        }
        if (s.airPlaceEnabled()) {
            // 思路来源: Wurst AirPlace — 射线落空时把空气方块当支撑面发包。
            // 目标方块可替换（空气/液体）时，只要六个方向任一相邻是实心方块
            // 就视为有支撑（柱子顶、柱子旁、水下沙土都合法），全空才判 AirPlace。
            Block target = player.getWorld().getBlockAt(x, y, z);
            if (target.getType().isAir() || target.isLiquid()) {
                boolean supported = false;
                for (int[] off : FACE_OFFSETS) {
                    Block neighbor = player.getWorld().getBlockAt(x + off[0], y + off[1], z + off[2]);
                    if (!neighbor.getType().isAir() && !neighbor.isLiquid()) {
                        supported = true;
                        break;
                    }
                }
                if (!supported) {
                    emit(player, d, "AIR_PLACE", 0.8, "无支撑面放置方块",
                            Map.of("block", target.getType().name(), "face", face));
                }
            }
        }
    }

    // ---------- 协议/时序类 ----------

    public void handleTickEnd(Player player, PlayerData d, long now) {
        if (!s.activeDetectionEnabled()) {
            d.flyingPacketsSinceTickEnd = 0;
            d.lastTickEnd = now;
            return;
        }
        // 思路来源: GrimAC TickTimer (GPL-3.0) — 1.21 CLIENT_TICK_END
        if (d.flyingPacketsSinceTickEnd > s.tickEndPackets()) {
            emit(player, d, "TICK_TIMER", 0.7, "tick 内移动包过多", Map.of("packets", d.flyingPacketsSinceTickEnd));
        }
        d.flyingPacketsSinceTickEnd = 0;
        d.lastTickEnd = now;
    }

    public void handleClickWindow(Player player, PlayerData d, String type) {
        d.clicksThisTick++;
        d.countEvent("window");
        long now = System.currentTimeMillis();
        d.windowClicks.addLast(now);
        while (!d.windowClicks.isEmpty() && now - d.windowClicks.getFirst() > s.windowClickWindowMs()) {
            d.windowClicks.removeFirst();
        }
        if (!s.activeDetectionEnabled()) return;
        if (d.clicksThisTick > s.badPacketsMaxClicks()) {
            emit(player, d, "INVENTORY_MACRO", 0.7, "背包点击频率异常", Map.of("clicks", d.clicksThisTick));
        }
        // 思路来源: GrimAC Crash 检查族 — 单秒背包点击洪水
        if (s.crashEnabled() && d.windowClicks.size() > s.crashWindowClicks()) {
            emit(player, d, "CRASH_A", 0.9, "背包点击洪水", Map.of("perSecond", d.windowClicks.size()));
        }
        // 思路来源: GrimAC PacketOrderA — 拾取后立即快速移动，背包宏常见包序
        if (d.lastWindowClickType != null && now - d.lastWindowClickTime < s.windowPacketOrderMs()
                && type.contains("QUICK_MOVE") && d.lastWindowClickType.contains("PICKUP")) {
            emit(player, d, "PACKET_ORDER", 0.6, "背包点击顺序异常（拾取后立即快速移动）",
                    Map.of("last", d.lastWindowClickType, "now", type));
        }
        d.lastWindowClickType = type;
        d.lastWindowClickTime = now;
    }

    public void handleTransaction(Player player, PlayerData d) {
        d.lastTransaction = System.currentTimeMillis();
    }

    // ---------- 豁免与工具 ----------

    private void recordPassiveMove(PlayerData d, double x, double y, double z,
                                   boolean onGround, boolean hasPos, long now,
                                   long interval, double dy, double horizontal) {
        d.lastDy = dy;
        d.recentMoveDeltas.addLast(new double[]{now, interval > 0 ? perTick(dy, interval) : dy,
                onGround ? 1.0 : 0.0});
        while (!d.recentMoveDeltas.isEmpty() && now - d.recentMoveDeltas.getFirst()[0] > 200) {
            d.recentMoveDeltas.removeFirst();
        }
        d.claimedOnGround = onGround;
        if (onGround) {
            d.groundTicks++;
            d.airTicks = 0;
            d.slowFallTicks = 0;
        } else {
            d.airTicks++;
            d.groundTicks = 0;
        }
        if (d.water || d.swimming) {
            d.lastWaterTime = now;
            d.airTicks = 0;
            d.slowFallTicks = 0;
        }
        if (d.climbing) {
            d.airTicks = 0;
            d.slowFallTicks = 0;
        }
        d.flyingPacketsSinceTickEnd++;
        d.lastHorizontal = horizontal;
        if (hasPos) {
            d.lastX = x;
            d.lastY = y;
            d.lastZ = z;
        }
        d.lastMoveTime = now;
    }

    private boolean recentlyTeleported(PlayerData d) {
        return System.currentTimeMillis() - d.lastTeleport < s.exemptionTeleportMs();
    }

    private boolean recentlyRespawned(PlayerData d) {
        return System.currentTimeMillis() - d.lastRespawn < s.exemptionRespawnMs();
    }

    private boolean recentlyKnocked(PlayerData d) {
        return System.currentTimeMillis() - d.lastVelocityTime < s.exemptionVelocityMs();
    }

    private void emit(Player player, PlayerData d, String category, double confidence, String reason, Map<String, Object> evidence) {
        d.countEvent(category);
        d.addEvent(category + " " + reason);
        plugin.onCandidate(player, CheckResult.of(category, confidence, reason, evidence));
    }

    private static boolean isClimbing(Player player) {
        if (player == null) return false;
        try {
            if (player.isClimbing()) return true;
            Material feet = player.getLocation().getBlock().getType();
            if (isClimbBlock(feet)) return true;
            Block base = player.getLocation().getBlock();
            if (isClimbBlock(base.getRelative(0, -1, 0).getType())) return true;
            if (isClimbBlock(base.getRelative(0, 1, 0).getType())) return true;
            return isClimbBlock(player.getEyeLocation().getBlock().getType());
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isClimbBlock(Material material) {
        if (material == null) return false;
        String name = material.name();
        return name.equals("LADDER") || name.equals("SCAFFOLDING") || name.contains("VINE");
    }

    private Entity findEntity(Player player, int entityId) {
        try {
            for (Entity e : player.getWorld().getEntities()) {
                if (e.getEntityId() == entityId) return e;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Vector closestPoint(Vector point, BoundingBox box) {
        double x = Math.max(box.getMinX(), Math.min(point.getX(), box.getMaxX()));
        double y = Math.max(box.getMinY(), Math.min(point.getY(), box.getMaxY()));
        double z = Math.max(box.getMinZ(), Math.min(point.getZ(), box.getMaxZ()));
        return new Vector(x, y, z);
    }

    // 官方 tick 归一：间隔 < 50ms 不再放大（客户端可能一 tick 多发包），
    // 间隔 > 50ms 按平均速度折算，避免子 tick 高频包造成跳跃/速度误报
    private static double perTick(double value, long intervalMs) {
        return value * 50.0 / Math.max(50, intervalMs);
    }

    private float normalize(float delta) {
        float n = delta % 360;
        if (n > 180) n -= 360;
        else if (n < -180) n += 360;
        return n;
    }

}
