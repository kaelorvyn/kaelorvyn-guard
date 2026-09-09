package com.kael.guardproxy;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelRegistrar;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Plugin(
        id = "kaelorvynguardproxy",
        name = "KaelorvynGuardProxy",
        version = "1.0.0",
        authors = {"Kaelorvyn"}
)
public final class KaelorvynGuardProxy {

    private static final String KICK_MARKER = "[KG]";
    private static final String LOBBY = "lobby";

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<UUID, String> pendingKicks = new ConcurrentHashMap<>();
    private String lobbyName = LOBBY;
    private String noticeMessage = "<gold><b>[Kaelorvyn 安全中心]</b></gold>\n"
            + "<white>请珍惜自己的账号。</white>\n"
            + "<white>本服务器对开挂行为零容忍。</white>\n"
            + "<white>本服务器已累计封禁 <red><b>{banned}</b></red> 人。</white>";
    private String dbUrl = "";
    private String dbUser = "";
    private String dbPassword = "";
    private int noticeDelaySeconds = 5;

    @Inject
    public KaelorvynGuardProxy(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        loadConfig();
        ChannelRegistrar registrar = server.getChannelRegistrar();
        registrar.register(MinecraftChannelIdentifier.create("kael", "guard"));
        logger.info("KaelorvynGuardProxy 已注册 kael:guard 通道");
    }

    private void loadConfig() {
        try {
            Files.createDirectories(dataDirectory);
            Path file = dataDirectory.resolve("config.properties");
            Properties props = new Properties();
            if (Files.exists(file)) {
                try (InputStream in = Files.newInputStream(file);
                     InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    props.load(reader);
                }
            }
            lobbyName = props.getProperty("lobby", LOBBY);
            String msg = props.getProperty("notice-message", "");
            if (!msg.isBlank()) noticeMessage = msg;
            noticeDelaySeconds = Math.max(1, Integer.parseInt(
                    props.getProperty("notice-delay-seconds", "5")));

            String host = props.getProperty("database.host", "");
            String port = props.getProperty("database.port", "3306");
            String name = props.getProperty("database.name", "");
            dbUser = props.getProperty("database.user", "");
            dbPassword = props.getProperty("database.password", "");
            if (host.isBlank() || name.isBlank()) {
                // 回退读取 KaelorvynBan 配置
                Path kban = dataDirectory.getParent().resolve("KaelorvynBan").resolve("config.properties");
                if (Files.exists(kban)) {
                    Properties kb = new Properties();
                    try (InputStream in = Files.newInputStream(kban);
                         InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                        kb.load(reader);
                    }
                    host = kb.getProperty("database.host", "localhost");
                    port = kb.getProperty("database.port", "3306");
                    name = kb.getProperty("database.name", "kaerban");
                    dbUser = kb.getProperty("database.user", "");
                    dbPassword = kb.getProperty("database.password", "");
                }
            }
            if (!host.isBlank() && !name.isBlank()) {
                dbUrl = "jdbc:mariadb://" + host + ":" + port + "/" + name;
                logger.info("KaelorvynGuardProxy 已连接封禁库：{}", dbUrl);
            } else {
                logger.warn("KaelorvynGuardProxy 未配置封禁数据库，通知人数将显示 ?");
            }
        } catch (IOException e) {
            logger.warn("无法读取 KaelorvynGuardProxy 配置：{}", e.getMessage());
        }
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        server.getScheduler().buildTask(this, () -> {
            if (!player.isActive()) return;
            int banned = bannedCount();
            String msg = noticeMessage.replace("{banned}", banned >= 0 ? String.valueOf(banned) : "?");
            player.sendMessage(mm.deserialize(msg));
        }).delay(noticeDelaySeconds, TimeUnit.SECONDS).schedule();
    }

    private int bannedCount() {
        if (dbUrl.isBlank()) return -1;
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT COUNT(DISTINCT target_uuid) FROM ban_logs WHERE active=1")) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        } catch (Exception e) {
            logger.warn("查询封禁人数失败：{}", e.getMessage());
            return -1;
        }
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().getId().equals("kael:guard")) return;
        if (!(event.getSource() instanceof ServerConnection connection)) return;
        Player player = connection.getPlayer();
        if (player == null) return;
        pendingKicks.put(player.getUniqueId(), new String(event.getData(), StandardCharsets.UTF_8));
        logger.info("已收到后端踢出消息：{}", player.getUsername());
        event.setResult(PluginMessageEvent.ForwardResult.handled());
    }

    @Subscribe
    public void onKickedFromServer(KickedFromServerEvent event) {
        String pending = pendingKicks.remove(event.getPlayer().getUniqueId());
        if (pending != null) {
            logger.info("正在为 {} 显示断线原因界面", event.getPlayer().getUsername());
            event.setResult(KickedFromServerEvent.DisconnectPlayer.create(
                    LegacyComponentSerializer.legacySection().deserialize(pending)));
            return;
        }
        Optional<Component> reason = event.getServerKickReason();
        if (reason.isEmpty()) return;
        String text = PlainTextComponentSerializer.plainText().serialize(reason.get());
        if (!text.contains(KICK_MARKER)) return;
        if (event.getServer().getServerInfo().getName().equals(lobbyName)) return;
        Optional<RegisteredServer> lobby = server.getServer(lobbyName);
        if (lobby.isEmpty()) return;
        event.setResult(KickedFromServerEvent.RedirectPlayer.create(lobby.get()));
        logger.info("玩家 {} 被 KaelorvynGuard 踢出，已重定向到大厅。", event.getPlayer().getUsername());
    }
}
