package com.kael.guard.command;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonStreamParser;
import com.kael.guard.KaelorvynGuard;
import com.kael.guard.util.Chat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class InformCommand implements CommandExecutor {

    private static final Pattern PERIOD = Pattern.compile("^([1-9]\\d*)([mhd])$", Pattern.CASE_INSENSITIVE);
    private final KaelorvynGuard plugin;

    public InformCommand(KaelorvynGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("kg.inform")) {
            sender.sendMessage(Chat.color("&c你没有权限提交玩家举报。"));
            return true;
        }
        if (args.length != 2) {
            sender.sendMessage(Chat.color("&c用法：/kinform <玩家> <1m-3d>，例如 /kinform Steve 3d"));
            return true;
        }
        long period = parsePeriod(args[1]);
        if (period < 0) {
            sender.sendMessage(Chat.color("&c时间必须是 1m 到 3d，例如 1m、2h、3d。"));
            return true;
        }
        long since = System.currentTimeMillis() - period;
        List<JsonObject> records = readRecords(args[0], since);
        sender.sendMessage(Chat.color("&bKaelorvyn 举报查询 &7玩家=" + args[0] + "，时间=" + args[1]
                + "，记录=" + records.size()));
        if (records.isEmpty()) {
            sender.sendMessage(Chat.color("&a该时间段没有异常行为记录。"));
        } else {
            records.stream().limit(30).forEach(record -> sender.sendMessage(Chat.color(
                    "&7[" + formatTime(record.get("time").getAsLong()) + "] &c"
                            + text(record, "category", "未知") + " &f" + text(record, "reason", "未知原因")
                            + " &8(来源 " + sourceName(text(record, "source", text(record, "level", "未知")))
                            + "，置信度 " + String.format(Locale.ROOT, "%.0f%%", number(record, "confidence") * 100) + ")")));
            if (records.size() > 30) sender.sendMessage(Chat.color("&7仅显示最新 30 条，完整记录保存在插件 logs 目录。"));
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target != null && plugin.analysisService() != null) {
            plugin.analysisService().submitManualReview(target, plugin.playerDataManager().get(target), args[1]);
            sender.sendMessage(Chat.color("&e已提交人工举报复核：" + target.getName()
                    + "。主动检测关闭期间不会全服自动扫描，仅复核这一次举报。"));
        } else if (target == null) {
            sender.sendMessage(Chat.color("&7目标玩家当前不在线，本次仅查询历史记录，未提交 AI 复核。"));
        } else {
            sender.sendMessage(Chat.color("&cAI 未启用，本次仅查询历史记录。"));
        }
        return true;
    }

    private List<JsonObject> readRecords(String player, long since) {
        File dir = new File(plugin.getDataFolder(), "logs");
        if (!dir.isDirectory()) return List.of();
        List<JsonObject> result = new ArrayList<>();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".jsonl"));
        if (files == null) return result;
        for (File file : files) {
            try {
                String raw = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                JsonStreamParser parser = new JsonStreamParser(new StringReader(raw));
                while (true) {
                    try {
                        if (!parser.hasNext()) break;
                        JsonElement element = parser.next();
                        if (!element.isJsonObject()) continue;
                        JsonObject object = element.getAsJsonObject();
                        if (!object.has("time") || !object.has("player")) continue;
                        long time = object.get("time").getAsLong();
                        if (time >= since && player.equalsIgnoreCase(text(object, "player", ""))) result.add(object);
                    } catch (RuntimeException ignored) {
                        // 兼容旧日志或损坏记录：跳过当前文件剩余内容，不影响其它日期。
                        break;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        result.sort(Comparator.comparingLong(o -> -o.get("time").getAsLong()));
        return result;
    }

    private static long parsePeriod(String value) {
        if (value.regionMatches(true, 0, "time:", 0, 5)) {
            value = value.substring(5);
        }
        Matcher matcher = PERIOD.matcher(value);
        if (!matcher.matches()) return -1;
        long number;
        try {
            number = Long.parseLong(matcher.group(1));
        } catch (NumberFormatException e) {
            return -1;
        }
        long minutes = switch (matcher.group(2).toLowerCase(Locale.ROOT)) {
            case "m" -> number;
            case "h" -> Math.multiplyExact(number, 60);
            case "d" -> Math.multiplyExact(number, 1440);
            default -> -1;
        };
        return minutes >= 1 && minutes <= 4320 ? minutes * 60_000L : -1;
    }

    private static String formatTime(long time) {
        return Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDateTime().toString().replace('T', ' ');
    }

    private static String text(JsonObject object, String key, String fallback) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : fallback;
    }

    private static double number(JsonObject object, String key) {
        try {
            return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsDouble() : 0.0;
        } catch (RuntimeException ignored) {
            return 0.0;
        }
    }

    private static String sourceName(String source) {
        return switch (source.toUpperCase(Locale.ROOT)) {
            case "BEHAVIOR" -> "游戏行为取证";
            case "AUTOMATIC_AI" -> "自动 AI";
            case "MANUAL_REVIEW" -> "玩家举报复核";
            case "MOD_AUDIT" -> "客户端模组审计";
            case "ADMIN" -> "管理员操作";
            case "PASSIVE-CANDIDATE" -> "被动行为取证";
            case "MOD-AUDIT-KICK" -> "客户端模组审计踢出";
            default -> source;
        };
    }
}
