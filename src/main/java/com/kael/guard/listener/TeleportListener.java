package com.kael.guard.listener;

import com.kael.guard.KaelorvynGuard;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public final class TeleportListener implements Listener {

    private final KaelorvynGuard plugin;

    public TeleportListener(KaelorvynGuard plugin) {
        this.plugin = plugin;
    }

    // 兼容 Essentials/Residence/RTP/Multiverse 等一切传送插件
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        long now = System.currentTimeMillis();
        var d = plugin.playerDataManager().get(event.getPlayer());
        d.joinTime = now;
        d.lastTeleport = now;
        d.lastRespawn = now;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        var from = event.getFrom();
        var to = event.getTo();
        if (to != null && (from.getWorld() != to.getWorld() || from.distanceSquared(to) > 0.01)) {
            plugin.playerDataManager().get(event.getPlayer()).lastTeleport = System.currentTimeMillis();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        plugin.playerDataManager().get(event.getPlayer()).lastRespawn = System.currentTimeMillis();
    }
}
