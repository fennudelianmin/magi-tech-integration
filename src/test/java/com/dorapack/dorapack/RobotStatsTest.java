package com.dorapack.dorapack;

import com.dorapack.dorapack.config.DoraConfig;
import com.dorapack.dorapack.robot.RobotStats;
import com.dorapack.dorapack.robot.RobotUpgrade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link RobotStats#recompute()}: how installed upgrade-module levels derive the
 * robot's battery, storage, energy cost, speed multipliers and feature flags.
 */
class RobotStatsTest {

    private static final double EPS = 1.0e-9;

    @BeforeEach
    void setUp() {
        DoraConfig.robotInitialEnergy = 1000;
        DoraConfig.robotInitialStorageSlots = 16;
    }

    @Test
    @DisplayName("a fresh robot uses config base stats")
    void defaultStats() {
        RobotStats stats = new RobotStats();
        assertEquals(1000, stats.getMaxEnergy());
        assertEquals(16, stats.getStorageSlots());
        assertEquals(20.0D, stats.getEnergyPerBlock(), EPS);
        assertEquals(1.0D, stats.getMoveSpeedMultiplier(), EPS);
        assertEquals(1.0D, stats.getDigSpeedMultiplier(), EPS);
        assertEquals(0, stats.getFortuneLevel());
        assertFalse(stats.isSmelting());
        assertFalse(stats.isWhitelist());
        assertFalse(stats.isOreScanner());
    }

    @Test
    @DisplayName("battery levels add 1000 EU each")
    void batteryScaling() {
        RobotStats stats = new RobotStats();
        stats.addLevel(RobotUpgrade.BATTERY);
        stats.addLevel(RobotUpgrade.BATTERY);
        assertEquals(1000 + 2 * 1000, stats.getMaxEnergy());
    }

    @Test
    @DisplayName("storage levels add 16 slots each")
    void storageScaling() {
        RobotStats stats = new RobotStats();
        stats.addLevel(RobotUpgrade.STORAGE);
        assertEquals(16 + 16, stats.getStorageSlots());
    }

    @Test
    @DisplayName("energy-saver reduces per-block cost by 20% multiplicatively")
    void energySaverScaling() {
        RobotStats stats = new RobotStats();
        stats.addLevel(RobotUpgrade.EFFICIENCY_ENERGY);
        assertEquals(20.0D * 0.8D, stats.getEnergyPerBlock(), EPS);
        stats.addLevel(RobotUpgrade.EFFICIENCY_ENERGY);
        assertEquals(20.0D * 0.8D * 0.8D, stats.getEnergyPerBlock(), EPS);
    }

    @Test
    @DisplayName("speed and dig-speed multipliers scale per level")
    void speedScaling() {
        RobotStats stats = new RobotStats();
        stats.addLevel(RobotUpgrade.SPEED);
        stats.addLevel(RobotUpgrade.SPEED);
        assertEquals(1.0D + 0.15D * 2, stats.getMoveSpeedMultiplier(), EPS);

        stats.addLevel(RobotUpgrade.DIG_SPEED);
        assertEquals(1.0D + 0.25D * 1, stats.getDigSpeedMultiplier(), EPS);
    }

    @Test
    @DisplayName("feature flags flip on when their module is installed")
    void featureFlags() {
        RobotStats stats = new RobotStats();
        stats.addLevel(RobotUpgrade.SMELTING);
        stats.addLevel(RobotUpgrade.WHITELIST);
        stats.addLevel(RobotUpgrade.ORE_SCANNER);
        stats.addLevel(RobotUpgrade.FORTUNE);
        assertTrue(stats.isSmelting());
        assertTrue(stats.isWhitelist());
        assertTrue(stats.isOreScanner());
        assertEquals(1, stats.getFortuneLevel());
    }

    @Test
    @DisplayName("addLevel refuses to exceed a module's max level")
    void addLevelRespectsCap() {
        RobotStats stats = new RobotStats();
        // SMELTING max level is 1.
        assertTrue(stats.addLevel(RobotUpgrade.SMELTING));
        assertFalse(stats.addLevel(RobotUpgrade.SMELTING), "cannot exceed max level 1");
        assertEquals(1, stats.getLevel(RobotUpgrade.SMELTING));

        // FORTUNE max level is 3.
        assertTrue(stats.addLevel(RobotUpgrade.FORTUNE));
        assertTrue(stats.addLevel(RobotUpgrade.FORTUNE));
        assertTrue(stats.addLevel(RobotUpgrade.FORTUNE));
        assertFalse(stats.addLevel(RobotUpgrade.FORTUNE), "capped at 3");
        assertEquals(3, stats.getLevel(RobotUpgrade.FORTUNE));
    }
}
