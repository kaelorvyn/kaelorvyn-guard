package com.kael.guard.checks;

import com.kael.guard.KaelorvynGuard;
import com.kael.guard.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class DamageMitigationListener implements Listener {

    private final KaelorvynGuard plugin;

    public DamageMitigationListener(KaelorvynGuard plugin) {
        this.plugin = plugin;
    }

    // 思路来源: AnGuard/Intave 软惩罚 — 可疑玩家伤害削弱
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!plugin.settings().activeDetectionEnabled()) return;
        if (!plugin.settings().mitigationEnabled()) return;
        PlayerData d = plugin.playerDataManager().get(attacker);
        double worst = 0;
        for (String category : plugin.settings().mitigationCategories()) {
            worst = Math.max(worst, d.score(category).getConfidence());
        }
        if (worst >= plugin.settings().mitigationActivation()) {
            event.setDamage(event.getDamage() * plugin.settings().mitigationMultiplier());
        }
    }
}
