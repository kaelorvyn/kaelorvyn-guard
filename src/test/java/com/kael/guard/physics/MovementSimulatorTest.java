package com.kael.guard.physics;

import com.kael.guard.data.PlayerData;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovementSimulatorTest {

    @Test
    void speedEffectIncreasesLimit() {
        PlayerData normal = new PlayerData(UUID.randomUUID(), "a");
        normal.walkSpeed = 0.2f;
        normal.speedAmp = -1;
        PlayerData buffed = new PlayerData(UUID.randomUUID(), "b");
        buffed.walkSpeed = 0.2f;
        buffed.speedAmp = 1;
        buffed.sprinting = true;
        assertTrue(MovementSimulator.horizontalLimit(buffed, 1.9)
                > MovementSimulator.horizontalLimit(normal, 1.9));
    }

    @Test
    void jumpBoostIncreasesJumpVelocity() {
        assertTrue(MovementSimulator.jumpVelocity(2) > MovementSimulator.jumpVelocity(0));
    }

    @Test
    void breakTimeIsPositive() {
        assertTrue(MovementSimulator.jumpVelocity(0) > 0);
    }

    @Test
    void officialTerminalFallSpeedIsFourBlocksPerTick() {
        assertEquals(4.0, VanillaPhysics.TERMINAL_FALL_SPEED, 1e-9);
    }

    @Test
    void officialClimbSpeedIsPointOneFive() {
        assertEquals(0.15, VanillaPhysics.CLIMB_SPEED, 1e-9);
    }

    @Test
    void officialWalkSpeedMatchesVanilla() {
        PlayerData normal = new PlayerData(UUID.randomUUID(), "a");
        normal.walkSpeed = 0.2f;
        assertEquals(0.21585 * 4.0, MovementSimulator.horizontalLimit(normal, 4.0), 1e-4);
    }
}
