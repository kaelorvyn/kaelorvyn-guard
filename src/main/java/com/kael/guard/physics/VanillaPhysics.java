package com.kael.guard.physics;

/**
 * Minecraft 1.21.11 官方物理常量。
 * 核对来源：服务器本机 paper-1.21.11.jar 字节码
 * （LivingEntity.travelInAir / Attributes.bootstrap / MobEffects / Entity）。
 */
public final class VanillaPhysics {

    private VanillaPhysics() {
    }

    // 移动物理
    public static final double GRAVITY = 0.08;                          // Attributes.GRAVITY 默认值
    public static final double AIR_DRAG_VERTICAL = 0.98;                // travelInAir: vy * 0.98 - gravity
    public static final double AIR_DRAG_HORIZONTAL = 0.91;              // travelInAir 空气水平阻力
    public static final double GROUND_FRICTION = 0.6;                   // 默认方块摩擦
    public static final double JUMP_STRENGTH = 0.42;                    // Attributes.JUMP_STRENGTH / BASE_JUMP_POWER
    public static final double JUMP_BOOST_PER_LEVEL = 0.1;              // 跳跃提升每级附加速度
    public static final double SPRINT_BONUS = 0.3;                      // SPEED_MODIFIER_SPRINTING
    public static final double SNEAK_MULTIPLIER = 0.3;                  // 潜行速度系数
    public static final double SPEED_EFFECT_PER_LEVEL = 0.2;            // 速度药水每级 +20%
    public static final double SLOWNESS_EFFECT_PER_LEVEL = 0.15;        // 缓慢药水每级 -15%
    public static final double CLIMB_SPEED = 0.15;                      // 梯子/藤蔓攀爬速度上限
    public static final double SLOW_FALL_GRAVITY = 0.01;                // 缓降状态重力

    public static final double TERMINAL_FALL_SPEED =
            GRAVITY / (1.0 - AIR_DRAG_VERTICAL);                        // = 4.0 blocks/tick
    public static final double SLOW_FALL_TERMINAL =
            SLOW_FALL_GRAVITY / (1.0 - AIR_DRAG_VERTICAL);              // = 0.5 blocks/tick

    // Bukkit walkSpeed=0.2 对应原版移动速度属性 0.1，官方为 4.317 blocks/s
    public static final double WALK_SPEED_BLOCKS_PER_SECOND = 4.317;
    public static final double WALK_SPEED_TO_BLOCKS_PER_TICK =
            WALK_SPEED_BLOCKS_PER_SECOND / 4.0;                          // 0.2 -> 0.21585 blocks/tick

    // 挖掘物理
    public static final double HARDNESS_TO_SECONDS = 1.5;               // hardness * 30 / 20 tick
    public static final double TOOL_SPEED_HAND = 1.0;
    public static final double TOOL_SPEED_SWORD = 1.5;
    public static final double TOOL_SPEED_SHEARS = 5.0;
    public static final double TOOL_SPEED_WOODEN = 2.0;
    public static final double TOOL_SPEED_STONE = 4.0;
    public static final double TOOL_SPEED_IRON = 6.0;
    public static final double TOOL_SPEED_DIAMOND = 8.0;
    public static final double TOOL_SPEED_NETHERITE = 9.0;
    public static final double TOOL_SPEED_GOLDEN = 12.0;
    public static final double EFFICIENCY_BONUS_PER_LEVEL_SQUARED = 1.0; // speed += eff^2 + 1
    public static final double HASTE_PER_LEVEL = 0.2;                    // 急迫每级 +20%
    public static final double FATIGUE_PER_LEVEL = 0.3;                  // 挖掘疲劳每级 -30%
    public static final double MIN_BREAK_SECONDS = 0.05;
}
