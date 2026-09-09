package com.kael.guard.ai;

import com.kael.guard.KaelorvynGuard;
import com.kael.guard.Settings;
import com.kael.guard.checks.CheckResult;
import com.kael.guard.data.PlayerData;
import com.kael.guard.util.Chat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicInteger;

public final class AnalysisService {

    private final KaelorvynGuard plugin;
    private final AiClient aiClient;
    private final AtomicInteger failures = new AtomicInteger();
    private volatile long pausedUntil;
    private volatile boolean running;
    private int taskId = -1;
    private final java.util.concurrent.atomic.AtomicInteger verdictCount = new java.util.concurrent.atomic.AtomicInteger();

    public AnalysisService(KaelorvynGuard plugin, AiClient aiClient) {
        this.plugin = plugin;
        this.aiClient = aiClient;
    }

    public void start() {
        if (running) return;
        if (!plugin.settings().activeDetectionEnabled()) {
            plugin.getLogger().info("游戏内主动检测已关闭，AI 周期复盘未启动；仅接受人工举报复核。 ");
            return;
        }
        running = true;
        Settings s = plugin.settings();
        taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::runWindow,
                s.intervalSeconds() * 20L, s.intervalSeconds() * 20L).getTaskId();
    }

    public void syncAutomatic() {
        if (plugin.settings().activeDetectionEnabled()) {
            start();
        } else {
            stop();
        }
    }

    public void stop() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        running = false;
    }

    private void runWindow() {
        if (!plugin.settings().activeDetectionEnabled()) return;
        if (System.currentTimeMillis() < pausedUntil) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.isBypass(player)) continue;
            PlayerData d = plugin.playerDataManager().get(player);
            if (d.recentEvents.isEmpty()) continue;
            submit(player, d, "周期复盘");
        }
    }

    public void submitCandidate(Player player, PlayerData d, CheckResult result) {
        if (!plugin.settings().activeDetectionEnabled()) return;
        if (!plugin.settings().aiEnabled()) return;
        long now = System.currentTimeMillis();
        if (now - d.lastCandidateSubmit < plugin.settings().cooldownSeconds() * 1000L) return;
        d.lastCandidateSubmit = now;
        submit(player, d, result.category() + " " + result.reason());
    }

    public void submitManualReview(Player player, PlayerData d, String period) {
        if (!plugin.settings().aiEnabled()) return;
        submit(player, d, "玩家举报复核（最近 " + period + "）", true);
    }

    private void submit(Player player, PlayerData d, String trigger) {
        submit(player, d, trigger, false);
    }

    private void submit(Player player, PlayerData d, String trigger, boolean manual) {
        if (System.currentTimeMillis() < pausedUntil) return;
        String snapshot = SnapshotBuilder.build(d, plugin.memoryStore());
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> aiClient.review(snapshot)
                .thenAccept(verdict -> handle(player, d, verdict, trigger, manual)));
    }

    private void handle(Player player, PlayerData d, AiVerdict verdict, String trigger, boolean manual) {
        verdictCount.incrementAndGet();
        plugin.getLogger().info("AI 复核：" + player.getName()
                + " 可疑=" + verdict.suspected()
                + " 置信度=" + String.format("%.2f", verdict.confidence())
                + " 类别=" + verdict.categories()
                + " 理由=" + verdict.reason());
        plugin.appendLog(player, CheckResult.of("AI_REVIEW", verdict.confidence(),
                "AI 复核：" + verdict.reason()), "ai");
        if (!manual && !plugin.settings().activeDetectionEnabled()) {
            plugin.getLogger().info("主动检测已关闭，忽略已返回的自动 AI 复核结果：" + player.getName());
            return;
        }
        if (!verdict.suspected() && verdict.confidence() < 0.5) {
            failures.set(0);
            return;
        }
        failures.set(0);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            plugin.memoryStore().addVerdict(d.uuid, verdict);
            if (!verdict.suspected()) {
                plugin.showWarning(player, "&e经 AI 复核确认是误判，抱歉打扰", 3);
                return;
            }
            if (verdict.confidence() >= plugin.settings().aiMinConfidence()) {
                String category = verdict.categories().isEmpty() ? "AI_REVIEW" : verdict.categories().get(0);
                plugin.showWarning(player, "&cAI 确认违规：" + Chat.categoryName(category)
                        + "，即将移除", plugin.settings().aiPreKickDelaySeconds());
                CheckResult result = manual
                        ? CheckResult.manualReview(category, verdict.confidence(),
                        "AI 复核：" + verdict.reason(), d.lastCandidateReason)
                        : CheckResult.aiConfirmed(category, verdict.confidence(),
                        "AI 复核：" + verdict.reason(), d.lastCandidateReason);
                plugin.punishService().alert(player, result);
            }
        });
    }

    public String status() {
        long remaining = Math.max(0, pausedUntil - System.currentTimeMillis());
        return "AI=" + (plugin.settings().aiEnabled() ? "启用" : "关闭")
                + "，自动周期=" + (running ? "启用" : "关闭")
                + "，人工举报=" + (plugin.settings().aiEnabled() ? "启用" : "关闭")
                + "，连续失败=" + failures.get()
                + "，熔断=" + (remaining > 0 ? remaining / 1000 + "秒" : "无");
    }

    public int verdictCount() {
        return verdictCount.get();
    }
}
