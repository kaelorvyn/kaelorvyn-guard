package com.kael.guard.util;

import org.bukkit.World;
import org.bukkit.util.Vector;

public final class RayTracer {

    private RayTracer() {
    }

    public static boolean blocked(World world, Vector from, Vector to) {
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 0.5) return false;
        int steps = Math.max(1, (int) Math.ceil(dist * 4));
        for (int i = 1; i < steps; i++) {
            double t = (double) i / steps;
            int bx = (int) Math.floor(from.getX() + dx * t);
            int by = (int) Math.floor(from.getY() + dy * t);
            int bz = (int) Math.floor(from.getZ() + dz * t);
            var block = world.getBlockAt(bx, by, bz);
            if (block.getType().isCollidable() && !block.isLiquid()) return true;
        }
        return false;
    }
}
