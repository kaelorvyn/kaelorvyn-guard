package com.kael.guard.memory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kael.guard.KaelorvynGuard;
import com.kael.guard.ai.AiVerdict;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MemoryStore {

    private static final Gson GSON = new Gson();
    private static final Type PLAYER_TYPE = new TypeToken<Map<String, PlayerRecord>>() {
    }.getType();
    private static final Type AB_TYPE = new TypeToken<Map<String, double[]>>() {
    }.getType();

    private final KaelorvynGuard plugin;
    private final File playerFile;
    private final File antibodyFile;
    private final Map<String, PlayerRecord> players = new HashMap<>();
    private final Map<String, double[]> antibodies = new HashMap<>();
    private volatile int cachedBannedCount = -1;
    private long bannedCountCachedAt;

    public MemoryStore(KaelorvynGuard plugin) {
        this.plugin = plugin;
        File dir = new File(plugin.getDataFolder(), "memory");
        dir.mkdirs();
        playerFile = new File(dir, "players.json");
        antibodyFile = new File(dir, "antibodies.json");
        load();
    }

    private void load() {
        if (playerFile.exists()) {
            try (FileReader r = new FileReader(playerFile)) {
                Map<String, PlayerRecord> loaded = GSON.fromJson(r, PLAYER_TYPE);
                if (loaded != null) players.putAll(loaded);
            } catch (IOException e) {
                plugin.getLogger().warning("无法读取 players.json：" + e.getMessage());
            }
        }
        if (antibodyFile.exists()) {
            try (FileReader r = new FileReader(antibodyFile)) {
                Map<String, double[]> loaded = GSON.fromJson(r, AB_TYPE);
                if (loaded != null) antibodies.putAll(loaded);
            } catch (IOException e) {
                plugin.getLogger().warning("无法读取 antibodies.json：" + e.getMessage());
            }
        }
    }

    public synchronized void flush() {
        try (FileWriter w = new FileWriter(playerFile)) {
            GSON.toJson(players, w);
        } catch (IOException e) {
            plugin.getLogger().warning("无法保存 players.json：" + e.getMessage());
        }
        try (FileWriter w = new FileWriter(antibodyFile)) {
            GSON.toJson(antibodies, w);
        } catch (IOException e) {
            plugin.getLogger().warning("无法保存 antibodies.json：" + e.getMessage());
        }
    }

    private PlayerRecord record(UUID uuid) {
        return players.computeIfAbsent(uuid.toString(), k -> new PlayerRecord());
    }

    public synchronized int warnings(UUID uuid) {
        if (plugin.banBridge().available()) {
            int db = plugin.banBridge().dbWarnings(uuid.toString());
            if (db >= 0) return db;
        }
        PlayerRecord r = players.get(uuid.toString());
        if (r == null) return 0;
        long expireAfter = System.currentTimeMillis() - plugin.settings().warningExpireDays() * 86400000L;
        if (r.lastWarning < expireAfter) {
            r.warnings = 0;
            return 0;
        }
        return r.warnings;
    }

    public synchronized int addWarning(UUID uuid) {
        if (plugin.banBridge().available()) {
            plugin.banBridge().dbAddWarning(uuid.toString());
            return warnings(uuid);
        }
        PlayerRecord r = record(uuid);
        r.warnings++;
        r.lastWarning = System.currentTimeMillis();
        return r.warnings;
    }

    public int trust(UUID uuid) {
        if (plugin.banBridge().available()) {
            return plugin.banBridge().dbTrust(uuid.toString(), plugin.settings().trustDefault());
        }
        PlayerRecord r = players.get(uuid.toString());
        return r == null ? plugin.settings().trustDefault() : r.trust;
    }

    public synchronized void setTrust(UUID uuid, int level) {
        if (plugin.banBridge().available()) {
            plugin.banBridge().dbSetTrust(uuid.toString(), "", level);
            return;
        }
        PlayerRecord r = record(uuid);
        r.trust = Math.max(0, Math.min(5, level));
        flush();
    }

    public synchronized void addVerdict(UUID uuid, AiVerdict verdict) {
        plugin.banBridge().dbAddVerdict(uuid.toString(), verdict.suspected(), verdict.confidence(),
                String.join(",", verdict.categories()), verdict.reason(), verdict.memoryUpdate());
        PlayerRecord r = record(uuid);
        r.verdicts.add(verdict.reason());
        while (r.verdicts.size() > 50) r.verdicts.remove(0);
        for (String c : verdict.categories()) {
            r.verdictCounts.merge(c, 1, Integer::sum);
        }
        if (!verdict.memoryUpdate().isBlank()) r.memoryNotes.add(verdict.memoryUpdate());
        while (r.memoryNotes.size() > 20) r.memoryNotes.remove(0);
        flush();
    }

    public void addEvent(UUID uuid, String category, double confidence, String reason) {
        plugin.banBridge().dbAddEvent(uuid.toString(), category, confidence, reason);
    }

    public Map<String, Integer> verdictCounts(UUID uuid) {
        PlayerRecord r = players.get(uuid.toString());
        return r == null ? Map.of() : r.verdictCounts;
    }

    public List<String> memoryNotes(UUID uuid) {
        PlayerRecord r = players.get(uuid.toString());
        return r == null ? List.of() : r.memoryNotes;
    }

    public synchronized Map<String, double[]> antibodies() {
        Map<String, double[]> copy = new HashMap<>();
        antibodies.forEach((k, v) -> copy.put(k, v.clone()));
        return copy;
    }

    public synchronized void saveAntibody(String family, double[] vector) {
        antibodies.put(family, vector.clone());
        flush();
    }

    public int bannedCount() {
        long now = System.currentTimeMillis();
        if (cachedBannedCount >= 0 && now - bannedCountCachedAt < 300000) return cachedBannedCount;
        int count = plugin.banBridge().bannedCount();
        cachedBannedCount = count;
        bannedCountCachedAt = now;
        return cachedBannedCount;
    }

    public static final class PlayerRecord {
        int warnings;
        long lastWarning;
        int trust = 3;
        List<String> verdicts = new ArrayList<>();
        Map<String, Integer> verdictCounts = new HashMap<>();
        List<String> memoryNotes = new ArrayList<>();
    }
}
