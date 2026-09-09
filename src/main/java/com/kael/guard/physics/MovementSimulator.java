package com.kael.guard.physics;

import com.kael.guard.data.PlayerData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

public final class MovementSimulator {

    public static final double GRAVITY = VanillaPhysics.GRAVITY;
    public static final double JUMP_VELOCITY = VanillaPhysics.JUMP_STRENGTH;
    public static final double JUMP_BOOST_FACTOR = VanillaPhysics.JUMP_BOOST_PER_LEVEL;
    public static final double SPRINT_MULTIPLIER = 1 + VanillaPhysics.SPRINT_BONUS;
    public static final double SNEAK_MULTIPLIER = VanillaPhysics.SNEAK_MULTIPLIER;
    public static final double GROUND_FRICTION = VanillaPhysics.GROUND_FRICTION;
    public static final double AIR_FRICTION = VanillaPhysics.AIR_DRAG_HORIZONTAL;

    private MovementSimulator() {
    }

    public static double jumpVelocity(int jumpBoostAmp) {
        return VanillaPhysics.JUMP_STRENGTH
                * (1 + Math.max(0, jumpBoostAmp) * VanillaPhysics.JUMP_BOOST_PER_LEVEL);
    }

    public static double horizontalLimit(PlayerData d, double tolerance) {
        double perTick = d.walkSpeed * VanillaPhysics.WALK_SPEED_TO_BLOCKS_PER_TICK;
        if (d.sprinting) perTick *= SPRINT_MULTIPLIER;
        if (d.sneaking) perTick *= SNEAK_MULTIPLIER;
        if (d.speedAmp >= 0) perTick *= 1 + VanillaPhysics.SPEED_EFFECT_PER_LEVEL * (d.speedAmp + 1);
        if (d.slownessAmp >= 0) perTick *= 1 - VanillaPhysics.SLOWNESS_EFFECT_PER_LEVEL * (d.slownessAmp + 1);
        return Math.max(0.62, perTick * tolerance);
    }

    public static boolean isNearGround(Player player) {
        if (player.isOnGround()) return true;
        Block below = player.getLocation().getBlock().getRelative(0, -1, 0);
        return below.getType().isSolid();
    }

    public static double breakSeconds(Player player, Block block) {
        if (block == null) return 1.0;
        double hardness = block.getType().getHardness();
        if (hardness < 0) return -1;
        ItemStack hand = player.getInventory().getItemInMainHand();
        double speed = toolSpeed(hand);
        int eff = hand.getEnchantmentLevel(Enchantment.EFFICIENCY);
        if (eff > 0 && isAppropriateTool(hand, block)) {
            speed += (double) eff * eff + VanillaPhysics.EFFICIENCY_BONUS_PER_LEVEL_SQUARED;
        }
        var haste = player.getPotionEffect(PotionEffectType.HASTE);
        if (haste != null) speed *= 1 + VanillaPhysics.HASTE_PER_LEVEL * (haste.getAmplifier() + 1);
        var fatigue = player.getPotionEffect(PotionEffectType.MINING_FATIGUE);
        if (fatigue != null) {
            speed *= Math.max(0.1, 1 - VanillaPhysics.FATIGUE_PER_LEVEL * (fatigue.getAmplifier() + 1));
        }
        return Math.max(VanillaPhysics.MIN_BREAK_SECONDS,
                hardness * VanillaPhysics.HARDNESS_TO_SECONDS / Math.max(0.1, speed));
    }

    private static double toolSpeed(ItemStack hand) {
        if (hand == null || hand.getType().isAir()) return VanillaPhysics.TOOL_SPEED_HAND;
        String name = hand.getType().name();
        if (name.contains("_PICKAXE") || name.contains("_AXE")
                || name.contains("_SHOVEL") || name.contains("_HOE")) {
            if (name.startsWith("NETHERITE_")) return VanillaPhysics.TOOL_SPEED_NETHERITE;
            if (name.startsWith("DIAMOND_")) return VanillaPhysics.TOOL_SPEED_DIAMOND;
            if (name.startsWith("IRON_")) return VanillaPhysics.TOOL_SPEED_IRON;
            if (name.startsWith("STONE_")) return VanillaPhysics.TOOL_SPEED_STONE;
            if (name.startsWith("GOLDEN_")) return VanillaPhysics.TOOL_SPEED_GOLDEN;
            if (name.startsWith("WOODEN_")) return VanillaPhysics.TOOL_SPEED_WOODEN;
        }
        if (name.contains("_SWORD")) return VanillaPhysics.TOOL_SPEED_SWORD;
        if (name.contains("SHEARS")) return VanillaPhysics.TOOL_SPEED_SHEARS;
        return VanillaPhysics.TOOL_SPEED_HAND;
    }

    private static boolean isAppropriateTool(ItemStack hand, Block block) {
        String tool = hand.getType().name();
        String target = block.getType().name();
        if (tool.contains("_PICKAXE")) {
            return target.contains("STONE") || target.contains("ORE") || target.contains("DEEPSLATE")
                    || target.contains("COBBLE") || target.contains("BRICK") || target.contains("CONCRETE")
                    || target.contains("TERRACOTTA") || target.contains("OBSIDIAN") || target.contains("ANVIL")
                    || target.contains("FURNACE") || target.contains("BLAST_FURNACE") || target.contains("SMOKER")
                    || target.contains("NETHERITE") || target.contains("DIAMOND") || target.contains("EMERALD")
                    || target.contains("LAPIS") || target.contains("REDSTONE") || target.contains("COAL")
                    || target.contains("QUARTZ") || target.contains("COPPER") || target.contains("AMETHYST")
                    || target.contains("CALCITE") || target.contains("TUFF") || target.contains("BASALT")
                    || target.contains("BLACKSTONE") || target.contains("END_STONE") || target.contains("PRISMARINE")
                    || target.contains("SEA_LANTERN") || target.contains("MAGMA") || target.contains("NETHER_BRICK")
                    || target.contains("PURPUR");
        }
        if (tool.contains("_AXE")) {
            return target.contains("LOG") || target.contains("WOOD") || target.contains("PLANKS")
                    || target.contains("FENCE") || target.contains("GATE") || target.contains("DOOR")
                    || target.contains("TRAPDOOR") || target.contains("SIGN") || target.contains("BARREL")
                    || target.contains("CHEST") || target.contains("CRAFTING") || target.contains("BOOKSHELF")
                    || target.contains("LADDER") || target.contains("BEEHIVE") || target.contains("BEE_NEST")
                    || target.contains("PUMPKIN") || target.contains("MELON") || target.contains("BAMBOO")
                    || target.contains("MUSHROOM") || target.contains("STEM") || target.contains("HYPHAE")
                    || target.contains("WART");
        }
        if (tool.contains("_SHOVEL")) {
            return target.contains("DIRT") || target.contains("GRASS") || target.contains("SAND")
                    || target.contains("GRAVEL") || target.contains("CLAY") || target.contains("SOUL")
                    || target.contains("PODZOL") || target.contains("MYCELIUM") || target.contains("PATH")
                    || target.contains("FARMLAND") || target.contains("SNOW") || target.contains("MUD");
        }
        if (tool.contains("_HOE")) {
            return target.contains("HAY") || target.contains("TARGET") || target.contains("DRIED")
                    || target.contains("SPONGE") || target.contains("LEAVES") || target.contains("SCULK")
                    || target.contains("MOSS") || target.contains("NETHER_WART") || target.contains("SHROOMLIGHT")
                    || target.contains("WARPED_WART");
        }
        if (tool.contains("_SWORD")) return target.equals("COBWEB");
        return false;
    }

    public static double friction(Material below) {
        if (below == null) return GROUND_FRICTION;
        switch (below) {
            case ICE, PACKED_ICE, FROSTED_ICE -> { return 0.98; }
            case BLUE_ICE -> { return 0.989; }
            case SLIME_BLOCK -> { return 0.8; }
            case SOUL_SAND, SOUL_SOIL -> { return 0.4; }
            case HONEY_BLOCK -> { return 0.5; }
            default -> { return GROUND_FRICTION; }
        }
    }
}
