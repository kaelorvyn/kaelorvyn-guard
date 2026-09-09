package com.kael.guard.checks;

import com.kael.guard.KaelorvynGuard;
import com.kael.guard.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Map;

/**
 * 只使用服务端真实结果判定：实际破坏的方块、实际造成的伤害。
 * 客户端攻击包/挥动包/挖掘包只作采集，不再直接用于速度类判定。
 */
public final class ResultCheckListener implements Listener {

    private final KaelorvynGuard plugin;

    public ResultCheckListener(KaelorvynGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
        if (plugin.isBypass(p)) return;
        PlayerData d = plugin.playerDataManager().get(p);
        String type = event.getBlock().getType().name();
        if (CheckEngine.ORES.contains(type)) d.oresBroken++;
        d.blocksBroken++;
        if (!plugin.settings().activeDetectionEnabled()) return;
        if (!plugin.settings().xrayEnabled() || d.blocksBroken <= plugin.settings().minOreSamples()) {
            return;
        }
        double ratio = (double) d.oresBroken / d.blocksBroken;
        if (ratio > plugin.settings().oreRatio()) {
            d.countEvent("xray");
            plugin.onCandidate(p, CheckResult.of("XRAY_STAT", 0.6,
                    "挖矿目标异常集中（实际破坏结果）",
                    Map.of("oreRatio", ratio, "broken", d.blocksBroken, "ores", d.oresBroken)));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        long now = System.currentTimeMillis();
        if (event.getEntity() instanceof Player victim) {
            String damagerType = event.getDamager().getType().name();
            if (!(event.getDamager() instanceof Player)
                    || damagerType.contains("BREEZE") || damagerType.contains("WIND_CHARGE")
                    || damagerType.contains("PROJECTILE")) {
                markExternalImpulse(victim, now);
            }
        }
        if (!(event.getDamager() instanceof Player p)) return;
        if (plugin.isBypass(p)) return;
        PlayerData d = plugin.playerDataManager().get(p);
        d.actualDamageTimes.addLast(now);
        while (!d.actualDamageTimes.isEmpty()
                && now - d.actualDamageTimes.getFirst() > 5000) {
            d.actualDamageTimes.removeFirst();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplosion(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                || cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            markExternalImpulse(victim, System.currentTimeMillis());
        }
    }

    private void markExternalImpulse(Player player, long now) {
        PlayerData data = plugin.playerDataManager().get(player);
        data.lastVelocityTime = now;
        data.externalImpulseTimes.addLast(now);
        while (!data.externalImpulseTimes.isEmpty()
                && now - data.externalImpulseTimes.getFirst() > 750) {
            data.externalImpulseTimes.removeFirst();
        }
    }
}
