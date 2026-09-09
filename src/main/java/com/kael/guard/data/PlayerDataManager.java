package com.kael.guard.data;

import com.kael.guard.KaelorvynGuard;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.util.BoundingBox;

public final class PlayerDataManager {

    private final KaelorvynGuard plugin;
    private final Map<UUID, PlayerData> dataMap = new ConcurrentHashMap<>();
    private final Map<Integer, Deque<BoxSnapshot>> boxHistory = new ConcurrentHashMap<>();
    private int taskId = -1;

    public PlayerDataManager(KaelorvynGuard plugin) {
        this.plugin = plugin;
    }

    public void start() {
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::tickAll, 1L, 1L).getTaskId();
    }

    public void stop() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
    }

    public PlayerData get(Player player) {
        return dataMap.computeIfAbsent(player.getUniqueId(), uuid -> new PlayerData(uuid, player.getName()));
    }

    public void remove(UUID uuid) {
        dataMap.remove(uuid);
    }

    public Map<UUID, PlayerData> all() {
        return dataMap;
    }

    private void tickAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData d = get(player);
            d.ping = player.getPing();
            d.walkSpeed = player.getWalkSpeed();
            d.flying = player.isFlying();
            d.gliding = player.isGliding();
            d.vehicle = player.isInsideVehicle();
            d.water = player.isInWater();
            Block feet = player.getLocation().getBlock();
            if (d.water || feet.isLiquid()
                    || feet.getRelative(0, -1, 0).isLiquid()
                    || feet.getRelative(0, 1, 0).isLiquid()) {
                d.lastWaterTime = System.currentTimeMillis();
            }
            d.creative = player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR;
            d.sprinting = player.isSprinting();
            d.sneaking = player.isSneaking();
            d.climbing = player.isClimbing();
            d.usingItem = player.isHandRaised();
            d.speedAmp = amp(player, PotionEffectType.SPEED);
            d.slownessAmp = amp(player, PotionEffectType.SLOWNESS);
            d.jumpAmp = amp(player, PotionEffectType.JUMP_BOOST);
            d.slowFalling = player.hasPotionEffect(PotionEffectType.SLOW_FALLING);
            d.swimming = player.isSwimming();
            d.attacksThisTick = 0;
            d.swingsThisTick = 0;
            d.breaksThisTick = 0;
            d.clicksThisTick = 0;
            d.placesThisTick = 0;
            d.lookPacketsThisTick = 0;
            if (d.pingSamples.size() > plugin.settings().pingSamples()) d.pingSamples.removeFirst();
            d.pingSamples.addLast(d.ping);
            long cutoff = System.currentTimeMillis() - plugin.settings().targetWindowMs();
            d.targetTimes.entrySet().removeIf(e -> e.getValue() < cutoff);
            recordBox(player.getEntityId(), player.getBoundingBox());
        }
    }

    public BoundingBox getHitbox(int entityId, long pingMs) {
        Deque<BoxSnapshot> history = boxHistory.get(entityId);
        if (history == null || history.isEmpty()) return null;
        long target = System.currentTimeMillis() - pingMs;
        BoxSnapshot best = null;
        long bestDiff = Long.MAX_VALUE;
        for (BoxSnapshot s : history) {
            long diff = Math.abs(s.time - target);
            if (diff < bestDiff) {
                bestDiff = diff;
                best = s;
            }
        }
        return best == null || bestDiff > plugin.settings().hitboxHistoryMs() ? null : best.box;
    }

    private void recordBox(int entityId, BoundingBox box) {
        long now = System.currentTimeMillis();
        Deque<BoxSnapshot> history = boxHistory.computeIfAbsent(entityId, k -> new ArrayDeque<>());
        history.addLast(new BoxSnapshot(now, box.clone()));
        while (!history.isEmpty() && now - history.getFirst().time > plugin.settings().hitboxHistoryMs()) {
            history.removeFirst();
        }
        if (history.size() > plugin.settings().hitboxMaxSnapshots()) history.removeFirst();
    }

    private static final class BoxSnapshot {
        final long time;
        final BoundingBox box;
        BoxSnapshot(long time, BoundingBox box) {
            this.time = time;
            this.box = box;
        }
    }

    private int amp(Player player, PotionEffectType type) {
        var effect = player.getPotionEffect(type);
        return effect == null ? -1 : effect.getAmplifier();
    }
}
