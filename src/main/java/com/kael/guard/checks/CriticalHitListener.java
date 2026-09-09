package com.kael.guard.checks;

import com.kael.guard.KaelorvynGuard;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Map;

/**
 * 直接使用服务端官方暴击判定（EntityDamageByEntityEvent#isCritical）。
 * 原版暴击必须满足：不在地面、不在水中/攀爬/载具，且实际有下落距离。
 */
public final class CriticalHitListener implements Listener {

    private final KaelorvynGuard plugin;

    public CriticalHitListener(KaelorvynGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player p)) return;
        if (!plugin.settings().activeDetectionEnabled()) return;
        if (!event.isCritical()) return;
        if (!plugin.settings().criticalDamageEnabled()) return;
        if (plugin.isBypass(p)) return;
        boolean impossible = p.isOnGround() || p.isInWater() || p.isClimbing()
                || p.isInsideVehicle();
        if (!impossible) return;
        plugin.onCandidate(p, CheckResult.of("CRITICALS", 0.9,
                "站地/无下落却造成暴击伤害（Criticals 实锤）",
                Map.of("onGround", p.isOnGround(),
                        "water", p.isInWater(),
                        "climbing", p.isClimbing(),
                        "vehicle", p.isInsideVehicle(),
                        "fallDistance", p.getFallDistance())));
    }
}
