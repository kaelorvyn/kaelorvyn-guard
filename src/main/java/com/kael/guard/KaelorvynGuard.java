package com.kael.guard;

import com.google.gson.Gson;
import com.kael.guard.ai.AnalysisService;
import com.kael.guard.ai.AiClient;
import com.kael.guard.checks.AntibodyLibrary;
import com.kael.guard.checks.AuraBotManager;
import com.kael.guard.checks.ChatCheck;
import com.kael.guard.checks.CriticalHitListener;
import com.kael.guard.checks.DamageMitigationListener;
import com.kael.guard.checks.CheckEngine;
import com.kael.guard.checks.CheckResult;
import com.kael.guard.checks.SignatureEngine;
import com.kael.guard.checks.ResultCheckListener;
import com.kael.guard.command.AdminCommand;
import com.kael.guard.data.PlayerData;
import com.kael.guard.data.PlayerDataManager;
import com.kael.guard.listener.PacketListener;
import com.kael.guard.listener.PunishLockListener;
import com.kael.guard.listener.TeleportListener;
import com.kael.guard.memory.KaelorvynBanBridge;
import com.kael.guard.memory.MemoryStore;
import com.kael.guard.notice.NoticeService;
import com.kael.guard.punish.PunishService;
import com.kael.guard.util.Chat;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class KaelorvynGuard extends JavaPlugin {

    // 日志文件按 JSONL 写入必须一条记录一行；举报查询同时兼容旧版多行 JSON。
    public static final Gson GSON = new Gson();

    private Settings settings;
    private PlayerDataManager playerDataManager;
    private CheckEngine checkEngine;
    private SignatureEngine signatureEngine;
    private AntibodyLibrary antibodyLibrary;
    private AuraBotManager auraBotManager;
    private MemoryStore memoryStore;
    private KaelorvynBanBridge banBridge;
    private AnalysisService analysisService;
    private PunishService punishService;
    private NoticeService noticeService;
    private PacketListener packetListener;
    private PacketListenerCommon packetHandle;
    private boolean packetMode;
    private final Map<UUID, String> trusted = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getMessenger().registerOutgoingPluginChannel(this, "kael:guard");
        settings = new Settings(this);
        loadTrusted();

        memoryStore = new MemoryStore(this);
        banBridge = new KaelorvynBanBridge(this);
        antibodyLibrary = new AntibodyLibrary(this, memoryStore);
        auraBotManager = new AuraBotManager(this);
        playerDataManager = new PlayerDataManager(this);
        signatureEngine = new SignatureEngine(this);
        checkEngine = new CheckEngine(this, playerDataManager);
        punishService = new PunishService(this, memoryStore, banBridge);
        noticeService = new NoticeService(this);

        if (settings.aiEnabled() && !settings.apiKey().isBlank()) {
            AiClient aiClient = new AiClient(this);
            analysisService = new AnalysisService(this, aiClient);
            analysisService.start();
        } else {
            getLogger().warning("AI 复核未启用：请在 config.yml 配置 api-key 或环境变量 AGNES_API_KEY。");
        }

        if (settings.packetEnabled() && getServer().getPluginManager().getPlugin("packetevents") != null) {
            try {
                packetListener = new PacketListener(this, checkEngine);
                packetHandle = packetListener.register();
                packetMode = true;
                getLogger().info("PacketEvents 封包监听已启用。");
            } catch (Throwable e) {
                getLogger().warning("PacketEvents 启用失败：" + e.getMessage());
            }
        } else {
            getLogger().warning("未检测到 PacketEvents，仅使用 Bukkit 事件采集。");
        }

        playerDataManager.start();
        noticeService.start();
        getServer().getPluginManager().registerEvents(new ChatCheck(this), this);
        getServer().getPluginManager().registerEvents(new CriticalHitListener(this), this);
        getServer().getPluginManager().registerEvents(new DamageMitigationListener(this), this);
        getServer().getPluginManager().registerEvents(new ResultCheckListener(this), this);
        getServer().getPluginManager().registerEvents(new TeleportListener(this), this);
        getServer().getPluginManager().registerEvents(new PunishLockListener(this), this);

        AdminCommand command = new AdminCommand(this);
        getCommand("kg").setExecutor(command);
        getCommand("kg").setTabCompleter(command);
        getCommand("kinform").setExecutor(new com.kael.guard.command.InformCommand(this));
        getLogger().info("KaelorvynGuard 已启用：游戏内主动行为检测=关闭，被动证据采集=开启，行为自动处罚=关闭。");
        getLogger().info("客户端模组审计由 SurvivalSplit 独立控制；/kinform 仅对被举报玩家提交一次人工复核。");
    }

    @Override
    public void onDisable() {
        if (analysisService != null) analysisService.stop();
        if (playerDataManager != null) playerDataManager.stop();
        if (noticeService != null) noticeService.stop();
        if (packetListener != null && packetHandle != null) {
            try {
                packetListener.unregister(packetHandle);
            } catch (Throwable ignored) {
            }
        }
        memoryStore.flush();
        getLogger().info("KaelorvynGuard 已禁用。");
    }

    public boolean isBypass(Player player) {
        if (!player.isOp() && player.hasPermission("kg.bypass")) return true;
        return player.isOp() && trusted.containsKey(player.getUniqueId());
    }

    public void onCandidate(Player player, CheckResult result) {
        // 被动模式只保留候选证据，供举报查询使用，不触发 AI、告警或处罚。
        if (!settings.activeDetectionEnabled()) {
            appendLog(player, result, "passive-candidate");
            return;
        }
        if (isBypass(player)) return;
        PlayerData data = playerDataManager.get(player);
        data.lastCandidateReason = result.reason();
        if (result.confidence() >= 0.75 && Set.of(
                "AIM", "AUTO_CLICKER", "AUTO_CLICKER_RIGHT", "KILL_AURA", "REACH", "ANTIBODY")
                .contains(result.category())) {
            auraBotManager.maybeSpawn(player, data);
        }
        if (result.signatureOrAntibody()) {
            punishService.alert(player, result);
            if (analysisService != null) analysisService.submitCandidate(player, data, result);
            return;
        }
        double scaled = result.confidence() * settings.trustMultiplier(data.trustLevel);
        double confidence = data.observeCheck(result.category(), scaled);
        boolean submitAi = analysisService != null && confidence >= settings.aiMinSubmitConfidence();
        if (confidence >= settings.alertConfidence()) {
            if (settings.requireAiConfirm() && submitAi) {
                // 先记录，立即交 AI 复盘，AI 认定后再处置
                appendLog(player, result, "pending-ai");
                showWarning(player, "&f检测到：" + Chat.categoryName(result.category()) + " "
                        + result.reason() + " &f| AI 正在复核", 3);
            } else {
                punishService.alert(player, result);
            }
        } else {
            appendLog(player, result, "candidate");
        }
        if (submitAi) analysisService.submitCandidate(player, data, result);
    }

    public void onModAudit(Player player, String category, double confidence, String reason) {
        if (isBypass(player)) return;
        if (punishService == null) return;
        punishService.alert(player, CheckResult.modAudit(category, confidence, reason));
    }

    public void onModAuditKick(Player player, String reason) {
        if (player == null || !player.isOnline()) return;
        if (punishService == null) return;
        getLogger().warning("客户端模组审计要求无警告踢出：" + player.getName() + "：" + reason);
        punishService.kickNoWarning(player, reason);
    }

    public void showWarning(Player player, String subtitle, int seconds) {
        if (!player.isOnline()) return;
        player.sendTitle(Chat.color(settings().kickTitle()),
                Chat.color(subtitle), 5, Math.max(1, seconds) * 20, 10);
    }

    public void confirmAntibody(UUID uuid, String family) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;
        PlayerData data = playerDataManager.get(player);
        antibodyLibrary.learn(family, data.featureVector());
        getLogger().info("已确认抗体：" + family + " <- " + player.getName());
    }

    public void autoLearnAntibody(Player player, String family, double confidence, boolean strong) {
        if (!settings.antibodyAutoLearn()) return;
        if (family == null || family.isBlank() || family.equalsIgnoreCase("ANTIBODY")) return;
        // 只自动学习配置允许的家族；其余家族用 /kg antibody 手动确认
        String f = family.toLowerCase(java.util.Locale.ROOT);
        if (!settings.antibodyLearnFamilies().contains(f)) return;
        if (!strong || confidence < settings.antibodyLearnMin()) return;
        if (isBypass(player)) return;
        PlayerData data = playerDataManager.get(player);
        antibodyLibrary.learnOrMerge(family, data.featureVector());
        getLogger().info("自动学习抗体：" + family + " <- " + player.getName());
    }

    public void appendLog(Player player, CheckResult result, String level) {
        try {
            File logs = new File(getDataFolder(), "logs");
            Files.createDirectories(logs.toPath());
            File file = new File(logs, LocalDate.now() + ".jsonl");
            Map<String, Object> obj = new java.util.LinkedHashMap<>();
            obj.put("time", System.currentTimeMillis());
            obj.put("level", level);
            obj.put("player", player.getName());
            obj.put("uuid", player.getUniqueId().toString());
            obj.put("source", result.source().name());
            obj.put("category", result.category());
            obj.put("confidence", result.confidence());
            obj.put("reason", result.reason());
            obj.put("evidence", result.evidence());
            try (FileWriter writer = new FileWriter(file, true)) {
                writer.write(GSON.toJson(obj) + System.lineSeparator());
            }
        } catch (IOException e) {
            getLogger().warning("无法写入反作弊日志：" + e.getMessage());
        }
    }

    private void loadTrusted() {
        trusted.clear();
        File file = new File(getDataFolder(), "trusted-uuids.yml");
        if (!file.exists()) {
            saveResource("trusted-uuids.yml", false);
            return;
        }
        try {
            for (String line : Files.readAllLines(file.toPath())) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\\s+");
                try {
                    trusted.put(UUID.fromString(parts[0]), parts.length > 1 ? parts[1] : "");
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (IOException e) {
            getLogger().warning("无法读取 trusted-uuids.yml：" + e.getMessage());
        }
    }

    public void addTrusted(UUID uuid, String name) {
        trusted.put(uuid, name);
        File file = new File(getDataFolder(), "trusted-uuids.yml");
        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write("# OP + UUID 绑定名单，只有同时在列表且为 OP 才豁免\n");
            for (Map.Entry<UUID, String> e : trusted.entrySet()) {
                writer.write(e.getKey() + " " + e.getValue() + "\n");
            }
        } catch (IOException e) {
            getLogger().warning("无法保存 trusted-uuids.yml：" + e.getMessage());
        }
    }

    public Settings settings() { return settings; }
    public PlayerDataManager playerDataManager() { return playerDataManager; }
    public CheckEngine checkEngine() { return checkEngine; }
    public SignatureEngine signatureEngine() { return signatureEngine; }
    public AntibodyLibrary antibodyLibrary() { return antibodyLibrary; }
    public AuraBotManager auraBotManager() { return auraBotManager; }
    public MemoryStore memoryStore() { return memoryStore; }
    public KaelorvynBanBridge banBridge() { return banBridge; }
    public AnalysisService analysisService() { return analysisService; }
    public PunishService punishService() { return punishService; }
    public NoticeService noticeService() { return noticeService; }
    public boolean packetMode() { return packetMode; }
}
