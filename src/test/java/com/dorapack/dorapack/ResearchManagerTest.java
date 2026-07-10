package com.dorapack.dorapack;

import com.dorapack.dorapack.capability.DoraPlayerData;
import com.dorapack.dorapack.config.DoraConfig;
import com.dorapack.dorapack.research.ResearchManager;
import com.dorapack.dorapack.research.ResearchTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the research-tree prerequisite and point-spending rules in {@link ResearchManager},
 * exercised through the data-only overloads that operate on {@link DoraPlayerData}.
 */
class ResearchManagerTest {

    private DoraPlayerData data;

    @BeforeEach
    void setUp() {
        DoraConfig.dailyPointCap = 100000;
        DoraConfig.basicTheoryCost = 10;
        DoraConfig.advancedTheoryCost = 50;
        DoraConfig.superTheoryCost = 100;
        data = new DoraPlayerData();
    }

    private void giveExactPoints(int points) {
        data.setTotalPoints(points);
    }

    @Test
    @DisplayName("basic theory requires and spends its point cost")
    void basicTheorySpendsPoints() {
        assertFalse(ResearchManager.researchBasicTheory(data), "no points -> fail");

        giveExactPoints(10);
        assertTrue(ResearchManager.researchBasicTheory(data));
        assertTrue(data.hasBasicTheory());
        assertEquals(0, data.getTotalPoints(), "cost deducted");

        // Already unlocked -> no-op, no further spend.
        giveExactPoints(10);
        assertFalse(ResearchManager.researchBasicTheory(data));
        assertEquals(10, data.getTotalPoints());
    }

    @Test
    @DisplayName("advanced theory needs basic theory + 3 tier-1 items + points")
    void advancedTheoryPrerequisites() {
        giveExactPoints(1000);
        // Without basic theory it must fail even with points.
        assertFalse(ResearchManager.researchAdvancedTheory(data));

        ResearchManager.researchBasicTheory(data);
        // Basic theory alone is not enough: need 3 tier-1 unlocks.
        assertFalse(ResearchManager.researchAdvancedTheory(data));

        String[] tier1 = ResearchTier.TIER_1.getItemKeys();
        data.unlockItem(tier1[0]);
        data.unlockItem(tier1[1]);
        assertFalse(ResearchManager.researchAdvancedTheory(data), "only 2 tier-1 unlocked");

        data.unlockItem(tier1[2]);
        assertTrue(ResearchManager.researchAdvancedTheory(data), "3 tier-1 satisfied");
        assertTrue(data.hasAdvancedTheory());
    }

    @Test
    @DisplayName("super theory needs advanced theory + all tier-2 items unlocked")
    void superTheoryPrerequisites() {
        giveExactPoints(100000);
        data.setBasicTheory(true);
        data.setAdvancedTheory(true);

        // No tier-2 unlocked yet.
        assertFalse(ResearchManager.researchSuperTheory(data));

        String[] tier2 = ResearchTier.TIER_2.getItemKeys();
        for (int i = 0; i < tier2.length - 1; i++) {
            data.unlockItem(tier2[i]);
        }
        assertFalse(ResearchManager.researchSuperTheory(data), "one tier-2 item still missing");

        data.unlockItem(tier2[tier2.length - 1]);
        assertTrue(ResearchManager.researchSuperTheory(data));
        assertTrue(data.hasSuperTheory());
    }

    @Test
    @DisplayName("super theory is blocked without advanced theory")
    void superTheoryRequiresAdvanced() {
        giveExactPoints(100000);
        data.setBasicTheory(true);
        for (String key : ResearchTier.TIER_2.getItemKeys()) {
            data.unlockItem(key);
        }
        assertFalse(ResearchManager.researchSuperTheory(data), "advanced theory missing");
    }

    @Test
    @DisplayName("unlockItem gates on the item's tier and spends the cost")
    void unlockItemTierGating() {
        giveExactPoints(100);
        String tier1Key = ResearchTier.TIER_1.getItemKeys()[0];

        // Tier-1 item needs basic theory first.
        assertFalse(ResearchManager.unlockItem(data, tier1Key, ResearchTier.TIER_1, 20));
        data.setBasicTheory(true);
        assertTrue(ResearchManager.unlockItem(data, tier1Key, ResearchTier.TIER_1, 20));
        assertTrue(data.isItemUnlocked(tier1Key));
        assertEquals(80, data.getTotalPoints());

        // Re-unlock is a no-op.
        assertFalse(ResearchManager.unlockItem(data, tier1Key, ResearchTier.TIER_1, 20));
        assertEquals(80, data.getTotalPoints());
    }

    @Test
    @DisplayName("unlockItem fails when the player cannot pay")
    void unlockItemInsufficientPoints() {
        data.setBasicTheory(true);
        giveExactPoints(5);
        String tier1Key = ResearchTier.TIER_1.getItemKeys()[0];
        assertFalse(ResearchManager.unlockItem(data, tier1Key, ResearchTier.TIER_1, 20));
        assertFalse(data.isItemUnlocked(tier1Key));
        assertEquals(5, data.getTotalPoints(), "no points spent on failed unlock");
    }

    @Test
    @DisplayName("zero-cost (recipe-gated) unlocks require no points")
    void unlockItemZeroCost() {
        data.setBasicTheory(true);
        String tier1Key = ResearchTier.TIER_1.getItemKeys()[1];
        assertTrue(ResearchManager.unlockItem(data, tier1Key, ResearchTier.TIER_1, 0));
        assertTrue(data.isItemUnlocked(tier1Key));
    }

    @Test
    @DisplayName("null player data never throws and always fails")
    void nullDataSafe() {
        assertFalse(ResearchManager.researchBasicTheory((com.dorapack.dorapack.capability.IDoraPlayerData) null));
        assertFalse(ResearchManager.researchAdvancedTheory((com.dorapack.dorapack.capability.IDoraPlayerData) null));
        assertFalse(ResearchManager.researchSuperTheory((com.dorapack.dorapack.capability.IDoraPlayerData) null));
        assertFalse(ResearchManager.unlockItem((com.dorapack.dorapack.capability.IDoraPlayerData) null,
                "x", ResearchTier.TIER_1, 0));
    }
}
