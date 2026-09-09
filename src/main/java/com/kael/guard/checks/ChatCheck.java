package com.kael.guard.checks;

import com.kael.guard.KaelorvynGuard;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatCheck implements Listener {

    private final KaelorvynGuard plugin;
    private final Map<UUID, Deque<Long>> messages = new ConcurrentHashMap<>();

    public ChatCheck(KaelorvynGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(PlayerChatEvent event) {
        if (!plugin.settings().chatEnabled()) return;
        if (!plugin.settings().activeDetectionEnabled()) return;
        Player player = event.getPlayer();
        if (plugin.isBypass(player)) return;
        String msg = event.getMessage();
        if (plugin.settings().chatNewlineEnabled() && msg != null
                && (msg.contains("\n") || msg.contains("\r"))) {
            plugin.onCandidate(player, CheckResult.of("CHAT", 0.8, "聊天消息含换行符"));
        }
        Deque<Long> times = messages.computeIfAbsent(player.getUniqueId(), k -> new ArrayDeque<>());
        long now = System.currentTimeMillis();
        times.addLast(now);
        while (!times.isEmpty() && now - times.getFirst() > plugin.settings().chatWindowMs()) {
            times.removeFirst();
        }
        int max = plugin.settings().chatSpamPerSecond();
        if (max > 0 && times.size() > max) {
            plugin.onCandidate(player, CheckResult.of("CHAT_SPAM", 0.7, "聊天刷屏",
                    Map.of("perSecond", times.size())));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        messages.remove(event.getPlayer().getUniqueId());
    }
}
