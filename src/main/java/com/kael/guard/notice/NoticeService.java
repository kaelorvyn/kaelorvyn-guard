package com.kael.guard.notice;

import com.kael.guard.KaelorvynGuard;
import com.kael.guard.data.PlayerData;
import com.kael.guard.util.Chat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

public final class NoticeService implements Listener {

    private final KaelorvynGuard plugin;
    private boolean registered;

    public NoticeService(KaelorvynGuard plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (registered) return;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        registered = true;
    }

    public void stop() {
        if (registered) {
            HandlerList.unregisterAll(this);
            registered = false;
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerData d = plugin.playerDataManager().get(player);
        d.trustLevel = plugin.memoryStore().trust(player.getUniqueId());
        // 进服提示统一由代理端延迟 3 秒发送，避免重复/过早
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.playerDataManager().remove(event.getPlayer().getUniqueId());
        plugin.memoryStore().flush();
    }

    public void sendNotice(Player player) {
        if (!plugin.settings().noticeEnabled()) return;
        List<String> lines = plugin.settings().noticeLines();
        int banned = plugin.memoryStore().bannedCount();
        String text = String.join("\n", lines.stream()
                .map(line -> line.replace("{banned}", banned >= 0 ? String.valueOf(banned) : "?"))
                .map(Chat::color)
                .toList());
        player.sendMessage(text);
    }
}
