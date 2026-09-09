package com.kael.guard.checks;

import com.kael.guard.KaelorvynGuard;
import com.kael.guard.Settings;
import com.kael.guard.data.PlayerData;
import org.bukkit.entity.Player;

import java.util.Map;

public final class SignatureEngine {

    private final KaelorvynGuard plugin;

    public SignatureEngine(KaelorvynGuard plugin) {
        this.plugin = plugin;
    }

    public void onMove(Player player, PlayerData d, double dy, boolean onGround, long ignoredInterval) {
        Settings s = plugin.settings();
        if (!s.signaturesEnabled()) return;
        // 传送/回弹会造成大位移，不能当 PacketFly 指纹
        if (System.currentTimeMillis() - d.lastTeleport < s.signatureTeleportExemptMs()) return;
        if (s.signaturePacketFlyEnabled() && dy <= -s.signaturePacketFlyMinDy() && onGround) {
            d.countEvent("packetfly");
            plugin.onCandidate(player, CheckResult.signature("PACKET_FLY", "packetfly-y420",
                    s.signaturePacketFlyConfidence(), "PacketFly y-420 异常包", Map.of("dy", dy)));
        }
        if (s.signatureDualMoveEnabled() && d.lastPacketY != 0
                && !d.water && !d.climbing && !d.vehicle && !d.flying && !d.gliding
                && Math.abs(dy - s.signatureDualMoveDy()) < s.signatureDualTolerance() && onGround) {
            long now = System.currentTimeMillis();
            if (d.packetFlyDualHits == 0 || now - d.packetFlyDualFirstHit > s.signatureDualWindowMs()) {
                d.packetFlyDualHits = 1;
                d.packetFlyDualFirstHit = now;
            } else {
                d.packetFlyDualHits++;
            }
            if (d.packetFlyDualHits >= s.signatureDualMinHits()) {
                plugin.onCandidate(player, CheckResult.signature("PACKET_FLY", "packetfly-dual-move",
                        s.signatureDualMoveConfidence(), "PacketFly 双移动包（y-0.01 + onGround）"));
                d.packetFlyDualHits = 0;
            }
        } else if (d.packetFlyDualHits > 0
                && System.currentTimeMillis() - d.packetFlyDualFirstHit > s.signatureDualWindowMs()) {
            d.packetFlyDualHits = 0;
        }
        if (s.signatureMeteorEnabled() && d.lastPacketY != 0
                && !d.water && !d.climbing && !d.vehicle && !d.flying && !d.gliding
                && Math.abs(dy - s.signatureMeteorDy()) < s.signatureMeteorTolerance()) {
            long now = System.currentTimeMillis();
            if (d.meteorAntiKickHits == 0 || now - d.meteorAntiKickFirstHit > s.signatureMeteorWindowMs()) {
                d.meteorAntiKickHits = 1;
                d.meteorAntiKickFirstHit = now;
            } else {
                d.meteorAntiKickHits++;
            }
            if (d.meteorAntiKickHits >= s.signatureMeteorMinHits()) {
                plugin.onCandidate(player, CheckResult.signature("FLIGHT", "meteor-anti-kick",
                        s.signatureMeteorConfidence(), "Meteor AntiKick 移动包改写（-0.0313）", Map.of("dy", dy)));
                d.meteorAntiKickHits = 0;
            }
        } else if (d.meteorAntiKickHits > 0
                && System.currentTimeMillis() - d.meteorAntiKickFirstHit > s.signatureMeteorWindowMs()) {
            d.meteorAntiKickHits = 0;
        }
        d.lastPacketY = d.lastY;
    }
}
