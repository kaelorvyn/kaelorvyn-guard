package com.kael.guard.punish;

import com.kael.guard.KaelorvynGuard;
import com.kael.guard.Settings;
import com.kael.guard.checks.CheckResult;
import com.kael.guard.memory.KaelorvynBanBridge;
import com.kael.guard.memory.MemoryStore;
import com.kael.guard.util.Chat;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

public final class PunishService {

    private final KaelorvynGuard plugin;
    private final MemoryStore store;
    private final KaelorvynBanBridge bridge;
    private final Map<UUID, Long> lastKick = new ConcurrentHashMap<>();
    private final Set<UUID> locked = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Location> lockedLocations = new ConcurrentHashMap<>();

    public PunishService(KaelorvynGuard plugin, MemoryStore store, KaelorvynBanBridge bridge) {
        this.plugin = plugin;
        this.store = store;
        this.bridge = bridge;
    }

    public void alert(Player player, CheckResult result) {
        if (!player.isOnline()) return;
        plugin.memoryStore().addEvent(player.getUniqueId(), result.category(), result.confidence(), result.reason());
        plugin.appendLog(player, result, logLevel(result));

        // 这是处罚层的最后一道保险，防止未来新增调用方绕过 KaelorvynGuard.onCandidate。
        // 关闭主动检测时，普通游戏行为和自动 AI 只能留证；人工举报、模组审计、管理员操作仍可处置。
        if (!plugin.settings().activeDetectionEnabled()
                && result.source() != CheckResult.Source.MANUAL_REVIEW
                && result.source() != CheckResult.Source.MOD_AUDIT
                && result.source() != CheckResult.Source.ADMIN) {
            plugin.getLogger().info("主动行为检测已关闭，仅记录不处罚：" + player.getName()
                    + " " + result.category() + "（来源=" + result.source() + "）");
            return;
        }
        if (plugin.settings().alertsEnabled()) {
            String msg = plugin.settings().alertsPrefix()
                    + " " + player.getName()
                    + " 疑似: " + result.category()
                    + " 置信度 " + String.format("%.2f", result.confidence())
                    + " 理由: " + result.reason();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.hasPermission("kg.alerts")) online.sendMessage(Chat.color(msg));
            }
            plugin.getLogger().warning(player.getName() + " 被判定为 " + result.category() + "：" + result.reason());
        }

        Settings s = plugin.settings();
        // AI 复核确认前不立即处置（强指纹和管理员手动警告除外）
        if (s.requireAiConfirm() && !result.signatureOrAntibody() && !result.aiConfirmed()) {
            return;
        }
        if (!s.kick() && !s.ban()) return;
        plugin.autoLearnAntibody(player, result.family(), result.confidence(), result.signatureOrAntibody());
        String serverReason = Chat.categoryName(result.category()) + " " + result.serverReason();
        String aiReason = result.aiConfirmed() ? result.reason() : null;
        int count = store.warnings(player.getUniqueId());
        if (s.ban() && count + 1 >= s.warningsBeforeBan()) {
            ban(player, serverReason);
        } else {
            kick(player, serverReason, aiReason);
        }
    }

    public void manualWarn(Player player, String category, String reason) {
        alert(player, CheckResult.adminWarning(category, 1.0, "管理员手动警告：" + reason));
    }

    public void kick(Player player, String reason, String aiReason) {
        scheduleKick(player, reason, aiReason, true);
    }

    public void kickNoWarning(Player player, String reason) {
        plugin.appendLog(player, CheckResult.modAudit("CLIENT_MOD", 1.0, reason), "mod-audit-kick");
        scheduleKick(player, reason, reason, false);
    }

    private void scheduleKick(Player player, String reason, String aiReason, boolean countWarning) {
        Settings s = plugin.settings();
        long now = System.currentTimeMillis();
        Long last = lastKick.get(player.getUniqueId());
        if (last != null && now - last < s.punishCooldownSeconds() * 1000L) {
            plugin.getLogger().info("踢出冷却中，跳过 " + player.getName() + " 的重复踢出。");
            return;
        }
        int count = countWarning ? store.addWarning(player.getUniqueId()) : 0;
        int left = countWarning ? Math.max(0, s.warningsBeforeBan() - count) : 0;
        lastKick.put(player.getUniqueId(), now);
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin,
                    () -> kickMain(player, reason, aiReason, count, left, countWarning));
            return;
        }
        kickMain(player, reason, aiReason, count, left, countWarning);
    }

    private void kickMain(Player player, String reason, String aiReason, int count, int left,
                          boolean countWarning) {
        Settings s = plugin.settings();
        if (!player.isOnline()) return;
        int delaySeconds = aiReason != null ? s.aiPreKickDelaySeconds() : s.preKickDelaySeconds();
        plugin.getLogger().info("已调度踢出 " + player.getName() + "（" + delaySeconds + " 秒后）");
        if (delaySeconds > 0) {
            openKickBook(player, reason, aiReason, count, left, countWarning);
            player.sendTitle(Chat.color(s.kickTitle()), Chat.color(s.kickSubtitle().replace("{reason}", reason)),
                    10, delaySeconds * 20, 10);
            player.sendActionBar(Chat.color(actionBar(countWarning, count, left)));
            lock(player, delaySeconds * 1000L);
        }
        sendKickToProxy(player, reason, aiReason, count, left, countWarning);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            unlock(player.getUniqueId());
            String screen = buildKickScreen(reason, aiReason, count, left, countWarning);
            try {
                plugin.getLogger().info("执行踢出：" + player.getName());
                // [KG] 标记让代理端 SafeReturn 等插件识别这是反作弊踢出，不重定向回大厅
                player.kickPlayer(Chat.color(screen + "\n[KG]"));
            } catch (Exception e) {
                plugin.getLogger().warning("踢出执行失败：" + player.getName() + " " + e.getMessage());
            }
        }, Math.max(1, delaySeconds) * 20L);
    }

    private void openKickBook(Player player, String reason, String aiReason, int count, int left,
                              boolean countWarning) {
        try {
            ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
            BookMeta meta = (BookMeta) book.getItemMeta();
            meta.setTitle("Kaelorvyn 安全中心");
            meta.setAuthor("Kaelorvyn");
            String page = Chat.color(plugin.settings().kickScreen().stream()
                    .filter(line -> !line.contains("{ai_reason}") || aiReason != null)
                    .map(line -> line.replace("{reason}", reason)
                            .replace("{ai_reason}", aiReason == null ? "" : aiReason)
                            .replace("{warning_line}", warningLine(countWarning, count, left)))
                    .reduce("", (a, b) -> a + "\n" + b));
            meta.addPage(page);
            book.setItemMeta(meta);
            player.openBook(book);
        } catch (Exception ignored) {
            // 客户端不支持书界面时仍保留标题/副标题提示
        }
    }

    private void sendKickToProxy(Player player, String reason, String aiReason, int count, int left,
                                 boolean countWarning) {
        try {
            String screen = Chat.color(buildKickScreen(reason, aiReason, count, left, countWarning));
            player.sendPluginMessage(plugin, "kael:guard", screen.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
            // 代理端不可用时仍走后端踢出/重定向
        }
    }

    public boolean isLocked(Player player) {
        return locked.contains(player.getUniqueId());
    }

    public Location lockedLocation(Player player) {
        return lockedLocations.get(player.getUniqueId());
    }

    public void lock(Player player, long millis) {
        locked.add(player.getUniqueId());
        lockedLocations.put(player.getUniqueId(), player.getLocation().clone());
        player.closeInventory();
        Bukkit.getScheduler().runTaskLater(plugin, () -> unlock(player.getUniqueId()), Math.max(1, millis / 50L));
    }

    public void unlock(UUID uuid) {
        locked.remove(uuid);
        lockedLocations.remove(uuid);
    }

    public void ban(Player player, String reason) {
        ban(player, reason, plugin.settings().banDurationHours());
    }

    public void banPlayer(Player player, String reason) {
        ban(player, reason);
    }

    public void banPlayer(Player player, String reason, int durationHours) {
        ban(player, reason, Math.max(1, durationHours));
    }

    private void ban(Player player, String reason, int durationHours) {
        Settings s = plugin.settings();
        store.addWarning(player.getUniqueId());
        boolean ok = bridge.available() && bridge.banPlayer(
                player.getUniqueId().toString(), player.getName(), reason, durationHours);
        if (!ok) {
            Bukkit.getBanList(BanList.Type.NAME).addBan(player.getName(), reason,
                    new Date(System.currentTimeMillis() + durationHours * 3600000L), "KaelorvynGuard");
        }
        if (s.broadcastBan()) {
            Bukkit.broadcastMessage(Chat.color("&c" + player.getName() + " 因作弊被 Kaelorvyn 服务器封禁。"));
        }
        String expire = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(
                new Date(System.currentTimeMillis() + durationHours * 3600000L));
        String screen = s.banScreen().stream()
                .map(line -> line.replace("{reason}", reason).replace("{expire}", expire))
                .reduce("", (a, b) -> a + "\n" + b);
        player.kickPlayer(Chat.color(screen));
        plugin.getLogger().warning("已封禁 " + player.getName() + "：" + reason);
    }

    public void testKick(Player player) {
        scheduleKick(player, "测试警告", null, false);
    }

    private String buildKickScreen(String reason, String aiReason, int count, int left,
                                   boolean countWarning) {
        return plugin.settings().kickScreen().stream()
                .filter(line -> !line.contains("{ai_reason}") || aiReason != null)
                .map(line -> line.replace("{reason}", reason)
                        .replace("{ai_reason}", aiReason == null ? "" : aiReason)
                        .replace("{warning_line}", warningLine(countWarning, count, left)))
                .reduce("", (a, b) -> a + "\n" + b);
    }

    private static String warningLine(boolean countWarning, int count, int left) {
        return countWarning
                ? "&7这是第 &e" + count + "&7 次警告，还剩 &e" + left + "&7 次将被封禁"
                : "&7本次为模组违规，不计入警告次数，请移除作弊模组后重新进入";
    }

    private String actionBar(boolean countWarning, int count, int left) {
        if (countWarning) {
            return plugin.settings().kickActionbar()
                    .replace("{count}", String.valueOf(count))
                    .replace("{left}", String.valueOf(left));
        }
        return "&c&l检测到作弊模组，请移除后重新进入";
    }

    private static String logLevel(CheckResult result) {
        return switch (result.source()) {
            case MOD_AUDIT -> "mod-audit";
            case MANUAL_REVIEW -> "manual-review";
            case ADMIN -> "admin-action";
            case AUTOMATIC_AI -> "ai";
            case BEHAVIOR -> "alert";
        };
    }
}
