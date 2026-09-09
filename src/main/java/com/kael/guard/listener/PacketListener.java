package com.kael.guard.listener;

import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.simple.PacketPlayReceiveEvent;
import com.github.retrooper.packetevents.event.simple.PacketPlaySendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPong;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;
import com.kael.guard.KaelorvynGuard;
import com.kael.guard.checks.CheckEngine;
import com.kael.guard.data.PlayerData;
import org.bukkit.entity.Player;

public final class PacketListener extends com.github.retrooper.packetevents.event.SimplePacketListenerAbstract {

    private final KaelorvynGuard plugin;
    private final CheckEngine checks;

    public PacketListener(KaelorvynGuard plugin, CheckEngine checks) {
        this.plugin = plugin;
        this.checks = checks;
    }

    @Override
    public void onPacketPlayReceive(PacketPlayReceiveEvent event) {
        Object raw = event.getPlayer();
        if (!(raw instanceof Player p)) return;
        PlayerData d = plugin.playerDataManager().get(p);
        long now = System.currentTimeMillis();
        try {
            d.protocolVersion = event.getClientVersion().getProtocolVersion();
        } catch (Exception ignored) {
        }

        try {
            switch (event.getPacketType()) {
                case PLAYER_POSITION, PLAYER_POSITION_AND_ROTATION, PLAYER_ROTATION, PLAYER_FLYING ->
                        handleFlying(event, p, d, now);
                case ATTACK -> {
                    int entityId = new WrapperPlayClientAttack(event).getEntityId();
                    if (now - d.lastInteractAttackTime >= 30) {
                        d.lastInteractAttackTime = now;
                        checks.handleAttack(p, d, entityId, now);
                        if (plugin.settings().activeDetectionEnabled()) {
                            plugin.auraBotManager().onAttack(p, d, entityId);
                        }
                    }
                }
                case INTERACT_ENTITY -> {
                    WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
                    if (wrapper.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                        if (now - d.lastInteractAttackTime >= 30) {
                            d.lastInteractAttackTime = now;
                            checks.handleAttack(p, d, wrapper.getEntityId(), now);
                            if (plugin.settings().activeDetectionEnabled()) {
                                plugin.auraBotManager().onAttack(p, d, wrapper.getEntityId());
                            }
                        }
                    } else {
                        checks.handleUse(p, d, now);
                    }
                }
                case ANIMATION -> checks.handleSwing(p, d, now);
                case HELD_ITEM_CHANGE -> checks.handleHeldSlot(p, d, new WrapperPlayClientHeldItemChange(event).getSlot());
                case PLAYER_DIGGING -> handleDigging(event, p, d, now);
                case PLAYER_BLOCK_PLACEMENT -> {
                    WrapperPlayClientPlayerBlockPlacement place = new WrapperPlayClientPlayerBlockPlacement(event);
                    checks.handlePlace(p, d, now, place.getBlockPosition().getX(),
                            place.getBlockPosition().getY(), place.getBlockPosition().getZ(), place.getFace().name());
                }
                case CLICK_WINDOW -> checks.handleClickWindow(p, d,
                        new WrapperPlayClientClickWindow(event).getWindowClickType().name());
                case CLIENT_TICK_END -> checks.handleTickEnd(p, d, now);
                case PLUGIN_MESSAGE -> {
                    WrapperPlayClientPluginMessage msg = new WrapperPlayClientPluginMessage(event);
                    if ("minecraft:brand".equals(msg.getChannelName())) {
                        byte[] data = msg.getData();
                        if (data != null && data.length > 1) {
                            int len = data[0] & 0xFF;
                            if (len <= data.length - 1) {
                                d.brand = new String(data, 1, len, java.nio.charset.StandardCharsets.UTF_8);
                            }
                        }
                    }
                }
                case PONG -> {
                    new WrapperPlayClientPong(event);
                    checks.handleTransaction(p, d);
                }
                case WINDOW_CONFIRMATION -> checks.handleTransaction(p, d);
                default -> {
                }
            }
        } catch (Exception ignored) {
            // 个别协议变体解析失败时跳过，避免影响其它玩家
        }
    }

    private void handleFlying(PacketPlayReceiveEvent event, Player p, PlayerData d, long now) {
        WrapperPlayClientPlayerFlying wrapper = new WrapperPlayClientPlayerFlying(event);
        boolean hasPos = wrapper.hasPositionChanged();
        boolean hasLook = wrapper.hasRotationChanged();
        Location loc = wrapper.getLocation();
        double x = hasPos && loc != null ? loc.getX() : d.lastX;
        double y = hasPos && loc != null ? loc.getY() : d.lastY;
        double z = hasPos && loc != null ? loc.getZ() : d.lastZ;
        float yaw = hasLook && loc != null ? loc.getYaw() : d.lastYaw;
        float pitch = hasLook && loc != null ? loc.getPitch() : d.lastPitch;
        checks.handleMove(p, d, x, y, z, yaw, pitch, wrapper.isOnGround(), hasPos, hasLook, now);
    }

    private void handleDigging(PacketPlayReceiveEvent event, Player p, PlayerData d, long now) {
        WrapperPlayClientPlayerDigging wrapper = new WrapperPlayClientPlayerDigging(event);
        String action = wrapper.getAction().name();
        if (action.contains("START_DESTROY_BLOCK")) {
            checks.handleDigStart(p, d, wrapper.getBlockPosition().getX(),
                    wrapper.getBlockPosition().getY(), wrapper.getBlockPosition().getZ(), now);
        } else if (action.contains("FINISH_DESTROY_BLOCK") || action.contains("CANCEL_DESTROY_BLOCK")) {
            checks.handleDigEnd(p, d, now);
        }
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.PLAYER_POSITION_AND_LOOK) {
            Object raw = event.getPlayer();
            if (raw instanceof Player p) {
                plugin.playerDataManager().get(p).lastTeleport = System.currentTimeMillis();
            }
        } else if (event.getPacketType() == PacketType.Play.Server.ENTITY_VELOCITY) {
            Object raw = event.getPlayer();
            if (raw instanceof Player p) {
                try {
                    WrapperPlayServerEntityVelocity vel = new WrapperPlayServerEntityVelocity(event);
                    if (vel.getEntityId() == p.getEntityId()) {
                        PlayerData d = plugin.playerDataManager().get(p);
                        d.lastVelocityTime = System.currentTimeMillis();
                        d.expectedKbSpeed = Math.hypot(vel.getVelocity().getX(), vel.getVelocity().getZ());
                        d.expectedKnockbackUntil = d.lastVelocityTime + p.getPing() + 400;
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    public PacketListenerCommon register() {
        return com.github.retrooper.packetevents.PacketEvents.getAPI()
                .getEventManager().registerListener(this);
    }

    public void unregister(PacketListenerCommon handle) {
        if (handle != null) {
            com.github.retrooper.packetevents.PacketEvents.getAPI()
                    .getEventManager().unregisterListener(handle);
        }
    }
}
