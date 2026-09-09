package com.kael.guard.command;

import com.kael.guard.KaelorvynGuard;
import com.kael.guard.checks.CheckResult;
import com.kael.guard.data.PlayerData;
import com.kael.guard.stats.StatsUtil;
import com.kael.guard.util.Chat;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public final class AdminCommand implements CommandExecutor, TabCompleter {

    private final KaelorvynGuard plugin;

    public AdminCommand(KaelorvynGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("kg.admin")) {
            sender.sendMessage(Chat.color("&c你没有权限执行此命令。"));
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.settings().reload();
                if (plugin.analysisService() != null) plugin.analysisService().syncAutomatic();
                sender.sendMessage(Chat.color("&aKaelorvynGuard 配置已重载。"));
            }
            case "info" -> {
                boolean active = plugin.settings().activeDetectionEnabled();
                sender.sendMessage(Chat.color("&aKaelorvynGuard v" + plugin.getDescription().getVersion()
                        + " &7封包监听=" + (plugin.packetMode() ? "开启（仅采集）" : "关闭")
                        + " &7抗体库=" + plugin.antibodyLibrary().size()));
                sender.sendMessage(Chat.color("&7游戏内主动行为检测：" + (active ? "&a开启" : "&c关闭")));
                sender.sendMessage(Chat.color("&7被动行为证据记录：&a开启"));
                sender.sendMessage(Chat.color("&7行为自动告警/踢出/封禁：" + (active ? "&a按配置执行" : "&c关闭")));
                sender.sendMessage(Chat.color("&7AI 自动周期复盘：" + (active ? "&a按配置执行" : "&c关闭")));
                sender.sendMessage(Chat.color("&7玩家举报 /kinform 人工复核："
                        + (plugin.analysisService() != null ? "&a开启" : "&c关闭")));
                sender.sendMessage(Chat.color("&7客户端模组审计：&e由 SurvivalSplit 独立控制"));
                if (plugin.analysisService() != null) {
                    sender.sendMessage(Chat.color("&7AI 状态：" + plugin.analysisService().status()
                            + " &7AI判定=" + plugin.analysisService().verdictCount()));
                }
            }
            case "stats" -> {
                if (args.length < 2) {
                    sender.sendMessage(Chat.color("&c用法：/kg stats <玩家>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(Chat.color("&c玩家不在线。"));
                    return true;
                }
                PlayerData d = plugin.playerDataManager().get(target);
                long cps = d.attackIntervals.stream().filter(t -> t >= System.currentTimeMillis() - 1000).count();
                double entropy = StatsUtil.entropy(d.attackIntervals, 5.0);
                double gcd = StatsUtil.gcd(d.yawDeltas);
                sender.sendMessage(Chat.color("&b" + target.getName()
                        + " &7品牌=" + (d.brand.isBlank() ? "未知" : d.brand)
                        + " &7信任=" + d.trustLevel
                        + " &7CPS=" + cps
                        + " 点击熵=" + String.format("%.2f", entropy)
                        + " GCD=" + String.format("%.5f", gcd)
                        + " 空气tick=" + d.airTicks
                        + " 警告=" + plugin.memoryStore().warnings(target.getUniqueId())));
            }
            case "review" -> {
                if (args.length < 2 || plugin.analysisService() == null) {
                    sender.sendMessage(Chat.color("&c用法：/kg review <玩家>（AI 未启用）"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(Chat.color("&c玩家不在线。"));
                    return true;
                }
                plugin.analysisService().submitManualReview(target, plugin.playerDataManager().get(target), "当前行为窗口");
                sender.sendMessage(Chat.color("&a已提交 AI 复核：" + target.getName()));
            }
            case "warn" -> {
                if (args.length < 3) {
                    sender.sendMessage(Chat.color("&c用法：/kg warn <玩家> <原因>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(Chat.color("&c玩家不在线。"));
                    return true;
                }
                String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
                plugin.punishService().manualWarn(target, "MANUAL", reason);
                sender.sendMessage(Chat.color("&a已警告 " + target.getName() + "：" + reason));
            }
            case "notice" -> {
                if (sender instanceof Player p) plugin.noticeService().sendNotice(p);
                else sender.sendMessage(Chat.color("&c仅玩家可用。"));
            }
            case "testkick" -> {
                Player target = sender instanceof Player p ? p : args.length > 1 ? Bukkit.getPlayer(args[1]) : null;
                if (target == null) {
                    sender.sendMessage(Chat.color("&c请指定在线玩家。"));
                    return true;
                }
                plugin.punishService().testKick(target);
            }
            case "antibody" -> {
                if (args.length < 3) {
                    sender.sendMessage(Chat.color("&c用法：/kg antibody <玩家> <外挂家族名>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(Chat.color("&c玩家不在线。"));
                    return true;
                }
                plugin.confirmAntibody(target.getUniqueId(), args[2]);
                sender.sendMessage(Chat.color("&a抗体已确认：" + args[2]));
            }
            case "trust" -> {
                if (args.length < 2) {
                    sender.sendMessage(Chat.color("&c用法：/kg trust <玩家>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(Chat.color("&c玩家不在线。"));
                    return true;
                }
                plugin.addTrusted(target.getUniqueId(), target.getName());
                sender.sendMessage(Chat.color("&a已将 " + target.getName() + " 加入可信名单。"));
            }
            case "trustlevel" -> {
                if (args.length < 3) {
                    sender.sendMessage(Chat.color("&c用法：/kg trustlevel <玩家> <0-5>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(Chat.color("&c玩家不在线。"));
                    return true;
                }
                try {
                    int level = Integer.parseInt(args[2]);
                    plugin.memoryStore().setTrust(target.getUniqueId(), level);
                    plugin.playerDataManager().get(target).trustLevel = level;
                    sender.sendMessage(Chat.color("&a已将 " + target.getName() + " 信任等级设为 " + level));
                } catch (NumberFormatException e) {
                    sender.sendMessage(Chat.color("&c等级必须是 0-5 的数字。"));
                }
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Chat.color("&b/kg reload &7重载配置"));
        sender.sendMessage(Chat.color("&b/kg info &7查看状态"));
        sender.sendMessage(Chat.color("&b/kg stats <玩家> &7查看检测统计"));
        sender.sendMessage(Chat.color("&b/kg review <玩家> &7提交 AI 复核"));
        sender.sendMessage(Chat.color("&b/kg warn <玩家> <原因> &7手动警告"));
        sender.sendMessage(Chat.color("&b/kg notice &7测试进服通知"));
        sender.sendMessage(Chat.color("&b/kg testkick &7测试踢出界面"));
        sender.sendMessage(Chat.color("&b/kg antibody <玩家> <家族> &7确认抗体"));
        sender.sendMessage(Chat.color("&b/kg trust <玩家> &7加入可信名单"));
        sender.sendMessage(Chat.color("&b/kg trustlevel <玩家> <0-5> &7设置信任等级"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("reload", "info", "stats", "review", "warn", "notice", "testkick", "antibody", "trust", "trustlevel")
                    .stream().filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && List.of("stats", "review", "warn", "antibody", "trust", "trustlevel").contains(args[0].toLowerCase(Locale.ROOT))) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.startsWith(args[1])).toList();
        }
        return List.of();
    }
}
