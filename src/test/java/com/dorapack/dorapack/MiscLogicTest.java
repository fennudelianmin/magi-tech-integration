package com.dorapack.dorapack;

import com.dorapack.dorapack.config.DoraConfig;
import com.dorapack.dorapack.item.ItemBagUpgrade;
import com.dorapack.dorapack.research.ResearchTier;
import com.dorapack.dorapack.robot.RobotUpgrade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Assorted pure-logic unit tests: bag-upgrade cost scaling, research-tier item-key integrity and
 * robot-upgrade id lookup.
 */
class MiscLogicTest {

    @BeforeEach
    void setUp() {
        DoraConfig.bagInitialSlots = 8;
        DoraConfig.bagMaxSlots = 32;
        DoraConfig.bagExpandCostBase = 50;
    }

    @Test
    @DisplayName("bag upgrade cost rises linearly with upgrades already applied")
    void bagUpgradeCostScaling() {
        // 0 upgrades done (initial 8 slots) -> base * 1
        assertEquals(50, ItemBagUpgrade.computeCost(8));
        // 1 upgrade done (16 slots) -> base * 2
        assertEquals(100, ItemBagUpgrade.computeCost(16));
        // 2 upgrades done (24 slots) -> base * 3
        assertEquals(150, ItemBagUpgrade.computeCost(24));
    }

    @Test
    @DisplayName("bag upgrade cost never drops below one upgrade's worth")
    void bagUpgradeCostFloor() {
        // Below the initial size should still cost the base (upgradesDone floored at 0).
        assertEquals(50, ItemBagUpgrade.computeCost(0));
    }

    @Test
    @DisplayName("research tiers expose defensive copies of their item-key arrays")
    void tierItemKeysAreCopies() {
        String[] first = ResearchTier.TIER_1.getItemKeys();
        String[] second = ResearchTier.TIER_1.getItemKeys();
        assertTrue(first != second, "each call returns a fresh clone");
        first[0] = "mutated";
        assertTrue(!"mutated".equals(ResearchTier.TIER_1.getItemKeys()[0]),
                "mutating the copy must not affect the enum");
    }

    @Test
    @DisplayName("tier item keys are non-empty and unique across tiers")
    void tierItemKeysUnique() {
        Set<String> seen = new HashSet<>();
        for (ResearchTier tier : ResearchTier.values()) {
            for (String key : tier.getItemKeys()) {
                assertNotNull(key);
                assertTrue(!key.isEmpty());
                assertTrue(seen.add(key), "duplicate research key across tiers: " + key);
            }
        }
    }

    @Test
    @DisplayName("tier 3 contains the mining robot and charging station keys")
    void tier3Contents() {
        Set<String> tier3 = new HashSet<>();
        for (String key : ResearchTier.TIER_3.getItemKeys()) {
            tier3.add(key);
        }
        assertEquals(2, tier3.size());
    }

    @Test
    @DisplayName("RobotUpgrade.byId round-trips every module id")
    void robotUpgradeByIdRoundTrip() {
        for (RobotUpgrade upgrade : RobotUpgrade.values()) {
            assertEquals(upgrade, RobotUpgrade.byId(upgrade.getId()));
            assertTrue(upgrade.getMaxLevel() >= 1);
        }
    }

    @Test
    @DisplayName("RobotUpgrade.byId returns null for unknown or null ids")
    void robotUpgradeByIdUnknown() {
        assertNull(RobotUpgrade.byId("does_not_exist"));
        assertNull(RobotUpgrade.byId(null));
    }
}
