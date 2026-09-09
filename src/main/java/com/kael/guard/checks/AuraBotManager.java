package com.kael.guard.checks;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.kael.guard.KaelorvynGuard;
import com.kael.guard.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class AuraBotManager {

    private final KaelorvynGuard plugin;

    public AuraBotManager(KaelorvynGuard plugin) {
        this.plugin = plugin;
    }

    // 思路来源: ALICE AuraBotManager — 给可疑玩家发送一个隐形假玩家实体作为诱饵
    public void maybeSpawn(Player player, PlayerData d) {
        if (!plugin.settings().auraBotEnabled()) return;
        if (d.auraBotId != -1) return;
        if (System.currentTimeMillis() - d.auraBotSpawnedAt < plugin.settings().auraBotCooldownMs()) return;
        int botId = ThreadLocalRandom.current().nextInt(100000, 2000000);
        UUID botUuid = UUID.randomUUID();
        d.auraBotId = botId;
        d.auraBotSpawnedAt = System.currentTimeMillis();
        Location loc = player.getLocation();
        Vector dir = loc.getDirection().normalize();
        Vector3d pos = new Vector3d(
                loc.getX() - dir.getX() * plugin.settings().auraBotDistance(),
                loc.getY() + plugin.settings().auraBotYOffset(),
                loc.getZ() - dir.getZ() * plugin.settings().auraBotDistance());
        try {
            WrapperPlayServerSpawnEntity spawn = new WrapperPlayServerSpawnEntity(
                    botId, Optional.of(botUuid), EntityTypes.PLAYER, pos,
                    loc.getYaw(), 0f, 0f, 0, Optional.empty());
            PacketEvents.getAPI().getProtocolManager().sendPacket(player, spawn);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (d.auraBotId == botId) {
                    d.auraBotId = -1;
                    destroy(player, botId);
                }
            }, plugin.settings().auraBotLifetimeTicks());
        } catch (Exception e) {
            d.auraBotId = -1;
        }
    }

    public void onAttack(Player player, PlayerData d, int entityId) {
        if (d.auraBotId == -1 || entityId != d.auraBotId) return;
        d.auraBotId = -1;
        plugin.onCandidate(player, CheckResult.signature("AURA_BOT", "aura-bot", 0.95,
                "攻击了隐形诱饵实体（KillAura 实锤）"));
        destroy(player, entityId);
    }

    private void destroy(Player player, int entityId) {
        try {
            PacketEvents.getAPI().getProtocolManager().sendPacket(
                    player, new WrapperPlayServerDestroyEntities(entityId));
        } catch (Exception ignored) {
        }
    }
}
