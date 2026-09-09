package com.kael.guard.memory;

import com.kael.guard.KaelorvynGuard;
import com.kael.guard.Settings;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class KaelorvynBanBridge {

    private final KaelorvynGuard plugin;
    private final String url;
    private final String user;
    private final String password;
    private final String schema;
    private volatile boolean available;

    public KaelorvynBanBridge(KaelorvynGuard plugin) {
        this.plugin = plugin;
        Settings s = plugin.settings();
        String u = s.dbUrl();
        String usr = s.dbUser();
        String pwd = s.dbPassword();
        if ((u == null || u.isBlank()) && s.kaelorvynConfigFile() != null && !s.kaelorvynConfigFile().isBlank()) {
            File cfg = new File(s.kaelorvynConfigFile());
            if (cfg.exists()) {
                Properties props = new Properties();
                try (FileInputStream in = new FileInputStream(cfg)) {
                    props.load(in);
                    usr = props.getProperty("database.user", usr);
                    pwd = props.getProperty("database.password", pwd);
                    u = "jdbc:mariadb://" + props.getProperty("database.host", "localhost")
                            + ":" + props.getProperty("database.port", "3306")
                            + "/" + props.getProperty("database.name", s.kaelorvynSchema());
                } catch (Exception e) {
                    plugin.getLogger().warning("读取 KaelorvynBan 配置失败：" + e.getMessage());
                }
            }
        }
        this.url = u == null ? "" : u;
        this.user = usr == null ? "" : usr;
        this.password = pwd == null ? "" : pwd;
        this.schema = s.kaelorvynSchema();
        test();
    }

    private void test() {
        if (url.isBlank()) {
            plugin.getLogger().info("KaelorvynBan 数据库未配置，封禁将回退到 Bukkit 封禁名单。");
            return;
        }
        try (Connection conn = connect(); Statement st = conn.createStatement()) {
            st.execute("SELECT 1");
            available = true;
            ensureTables(st);
            plugin.getLogger().info("KaelorvynBan 数据库连接成功：" + url);
        } catch (Exception e) {
            plugin.getLogger().warning("KaelorvynBan 数据库连接失败：" + e.getMessage());
        }
    }

    private void ensureTables(Statement st) throws Exception {
        st.executeUpdate("CREATE TABLE IF NOT EXISTS " + schema + ".kg_warnings (" +
                "uuid VARCHAR(36) PRIMARY KEY, warnings INT NOT NULL DEFAULT 0, last_warning BIGINT NOT NULL DEFAULT 0)");
        st.executeUpdate("CREATE TABLE IF NOT EXISTS " + schema + ".kg_players (" +
                "uuid VARCHAR(36) PRIMARY KEY, name VARCHAR(64), trust INT NOT NULL DEFAULT 3, last_seen BIGINT NOT NULL DEFAULT 0)");
        st.executeUpdate("CREATE TABLE IF NOT EXISTS " + schema + ".kg_ai_verdicts (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), time BIGINT, suspected TINYINT, " +
                "confidence DOUBLE, categories TEXT, reason TEXT, memory_update TEXT)");
        st.executeUpdate("CREATE TABLE IF NOT EXISTS " + schema + ".kg_events (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), time BIGINT, category VARCHAR(64), " +
                "confidence DOUBLE, reason TEXT)");
    }

    public int dbWarnings(String uuid) {
        if (!available) return -1;
        try (Connection conn = connect(); Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT warnings FROM " + schema + ".kg_warnings WHERE uuid='" + uuid + "'")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            return -1;
        }
    }

    public void dbAddWarning(String uuid) {
        if (!available) return;
        try (Connection conn = connect(); Statement st = conn.createStatement()) {
            st.executeUpdate("INSERT INTO " + schema + ".kg_warnings (uuid, warnings, last_warning) VALUES ('"
                    + uuid + "', 1, " + System.currentTimeMillis() + ") ON DUPLICATE KEY UPDATE "
                    + "warnings=warnings+1, last_warning=" + System.currentTimeMillis());
        } catch (Exception ignored) {
        }
    }

    public int dbTrust(String uuid, int def) {
        if (!available) return def;
        try (Connection conn = connect(); Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT trust FROM " + schema + ".kg_players WHERE uuid='" + uuid + "'")) {
            return rs.next() ? rs.getInt(1) : def;
        } catch (Exception e) {
            return def;
        }
    }

    public void dbSetTrust(String uuid, String name, int level) {
        if (!available) return;
        try (Connection conn = connect(); Statement st = conn.createStatement()) {
            st.executeUpdate("INSERT INTO " + schema + ".kg_players (uuid, name, trust, last_seen) VALUES ('"
                    + uuid + "', '" + escape(name) + "', " + level + ", " + System.currentTimeMillis()
                    + ") ON DUPLICATE KEY UPDATE trust=" + level + ", name='" + escape(name) + "'");
        } catch (Exception ignored) {
        }
    }

    public void dbAddVerdict(String uuid, boolean suspected, double confidence, String categories, String reason, String memory) {
        if (!available) return;
        try (Connection conn = connect(); Statement st = conn.createStatement()) {
            st.executeUpdate("INSERT INTO " + schema + ".kg_ai_verdicts (uuid, time, suspected, confidence, categories, reason, memory_update) VALUES ('"
                    + uuid + "', " + System.currentTimeMillis() + ", " + (suspected ? 1 : 0) + ", "
                    + confidence + ", '" + escape(categories) + "', '" + escape(reason) + "', '" + escape(memory) + "')");
        } catch (Exception ignored) {
        }
    }

    public void dbAddEvent(String uuid, String category, double confidence, String reason) {
        if (!available) return;
        try (Connection conn = connect(); Statement st = conn.createStatement()) {
            st.executeUpdate("INSERT INTO " + schema + ".kg_events (uuid, time, category, confidence, reason) VALUES ('"
                    + uuid + "', " + System.currentTimeMillis() + ", '" + escape(category) + "', "
                    + confidence + ", '" + escape(reason) + "')");
        } catch (Exception ignored) {
        }
    }

    private static String escape(String text) {
        return text == null ? "" : text.replace("'", "''");
    }

    private Connection connect() throws Exception {
        Class.forName("org.mariadb.jdbc.Driver");
        return DriverManager.getConnection(url, user, password);
    }

    public boolean available() {
        return available;
    }

    public int bannedCount() {
        if (!available) return -1;
        try (Connection conn = connect(); Statement st = conn.createStatement()) {
            try (ResultSet rs = st.executeQuery(
                    "SELECT COUNT(DISTINCT target_uuid) FROM " + schema + ".ban_logs WHERE active=1")) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("查询封禁人数失败：" + e.getMessage());
        }
        return -1;
    }

    public boolean banPlayer(String uuid, String name, String reason, long durationHours) {
        if (!available) return false;
        try (Connection conn = connect(); Statement st = conn.createStatement()) {
            List<String> cols = new ArrayList<>();
            try (ResultSet rs = st.executeQuery("SHOW COLUMNS FROM " + schema + ".ban_logs")) {
                while (rs.next()) cols.add(rs.getString(1).toLowerCase());
            }
            long start = System.currentTimeMillis();
            long end = start + durationHours * 3600000L;
            StringBuilder sql = new StringBuilder("INSERT INTO ").append(schema).append(".ban_logs (");
            List<String> names = new ArrayList<>();
            for (String c : List.of("target_uuid", "target_name", "reason", "source", "operator",
                    "start_time", "end_time", "active")) {
                if (cols.contains(c)) names.add(c);
            }
            sql.append(String.join(",", names)).append(") VALUES (");
            for (int i = 0; i < names.size(); i++) sql.append("?,");
            sql.setLength(sql.length() - 1);
            sql.append(")");
            try (var ps = conn.prepareStatement(sql.toString())) {
                int idx = 1;
                for (String c : names) {
                    switch (c) {
                        case "target_uuid" -> ps.setString(idx++, uuid);
                        case "target_name" -> ps.setString(idx++, name);
                        case "reason" -> ps.setString(idx++, reason);
                        case "source" -> ps.setString(idx++, "KaelorvynGuard");
                        case "operator" -> ps.setString(idx++, "KaelorvynGuard");
                        case "start_time" -> ps.setLong(idx++, start);
                        case "end_time" -> ps.setLong(idx++, end);
                        case "active" -> ps.setInt(idx++, 1);
                        default -> ps.setString(idx++, "");
                    }
                }
                ps.executeUpdate();
            }
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("写入封禁记录失败：" + e.getMessage());
            return false;
        }
    }
}
