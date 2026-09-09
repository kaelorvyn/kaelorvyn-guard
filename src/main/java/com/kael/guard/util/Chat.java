package com.kael.guard.util;

import java.util.List;

public final class Chat {

    private Chat() {
    }

    public static String color(String text) {
        return text == null ? "" : text.replace('&', '§');
    }

    public static String join(List<String> lines) {
        return String.join("\n", lines.stream().map(Chat::color).toList());
    }

    public static String categoryName(String category) {
        if (category == null) return "";
        return switch (category.toUpperCase(java.util.Locale.ROOT)) {
            case "FLIGHT" -> "飞行";
            case "SPEED" -> "速度";
            case "NO_FALL" -> "无摔落";
            case "PHASE" -> "穿墙";
            case "TIMER", "TICK_TIMER" -> "变速";
            case "REACH" -> "攻击距离";
            case "AIM", "AIM_SNAP" -> "锁头";
            case "AUTO_CLICKER" -> "左键连点器";
            case "AUTO_CLICKER_RIGHT" -> "右键连点器";
            case "KILL_AURA" -> "杀戮光环";
            case "MULTI_AURA" -> "多目标攻击";
            case "SCAFFOLD" -> "搭路";
            case "FAST_BREAK" -> "快速挖掘";
            case "MULTI_BREAK" -> "多方块破坏";
            case "ELYTRA_FLY" -> "鞘翅飞行";
            case "ELYTRA_BOOST" -> "鞘翅加速";
            case "ELYTRA_NO_ITEM" -> "无鞘翅滑翔";
            case "VEHICLE_FLY" -> "载具飞行";
            case "FAST_CLIMB" -> "快速攀爬";
            case "FAST_FALL" -> "快速下落";
            case "SLOW_FALL" -> "缓慢下落";
            case "BLINK" -> "闪烁";
            case "NO_SWING", "NO_SWING_BREAK" -> "无挥动";
            case "NO_VELOCITY", "KNOCKBACK" -> "抗击退";
            case "FAST_PLACE" -> "快速放置";
            case "MULTI_PLACE" -> "多位置放置";
            case "AIR_PLACE" -> "空中放置";
            case "XRAY" -> "透视挖矿";
            case "AIR_JUMP" -> "空中跳跃";
            case "SPIDER" -> "爬墙";
            case "JESUS" -> "水面行走";
            case "NO_SLOWDOWN" -> "无减速";
            case "CRITICALS" -> "暴击";
            case "THROUGH_WALLS" -> "隔墙攻击";
            case "INSTANT_MINE" -> "瞬挖";
            case "PACKET_SPAM" -> "封包洪水";
            case "ATTACK_WHILE_MINING" -> "挖矿攻击";
            case "AIR_LIQUID_BREAK" -> "液体挖掘";
            case "ANIMATION_SPAM" -> "动画刷包";
            case "CRASH_A" -> "崩溃攻击";
            case "CHAT" -> "聊天";
            case "CHAT_SPAM" -> "聊天刷屏";
            case "BACKTRACK" -> "回滚";
            case "BAD_PACKETS" -> "异常封包";
            case "PACKET_ORDER" -> "包序异常";
            case "INVENTORY_MACRO" -> "背包宏";
            case "AUTO_SWITCH" -> "自动切刀";
            case "WRONG_TOOL_FAST" -> "错工具速挖";
            case "STEP" -> "台阶";
            case "PACKET_FLY" -> "封包飞行";
            case "ANTIBODY" -> "已知外挂";
            case "AI_REVIEW" -> "AI 复核";
            case "MANUAL" -> "管理员";
            case "TELEPORT" -> "传送作弊";
            case "LOOK_PACKETS" -> "旋转包异常";
            case "CLIENT_MOD", "MOD_AUDIT" -> "客户端模组违规";
            default -> category;
        };
    }
}
