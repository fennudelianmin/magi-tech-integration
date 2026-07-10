package com.dorapack.dorapack.research;

import com.dorapack.dorapack.capability.DoraCapabilities;
import com.dorapack.dorapack.capability.IDoraPlayerData;
import com.dorapack.dorapack.config.DoraConfig;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Stateless helper encapsulating the research-tree rules: theory prerequisites, point spending and
 * item-unlock gating. All methods are server-authoritative and null-safe against fake players.
 */
public final class ResearchManager {

    /** Number of tier-1 items that must be unlocked before advanced theory can be researched. */
    public static final int ADVANCED_THEORY_REQUIRED_TIER1 = 3;

    private ResearchManager() {
    }

    public static boolean isItemAvailable(EntityPlayer player, String itemKey) {
        IDoraPlayerData data = DoraCapabilities.get(player);
        return data != null && data.isItemUnlocked(itemKey);
    }

    /**
     * Attempts to research basic theory, spending the configured point cost.
     *
     * @return {@code true} if newly unlocked
     */
    public static boolean researchBasicTheory(EntityPlayer player) {
        return researchBasicTheory(DoraCapabilities.get(player));
    }

    /**
     * Attempts to research advanced theory. Requires basic theory plus at least
     * {@link #ADVANCED_THEORY_REQUIRED_TIER1} unlocked tier-1 items (prevents level skipping).
     */
    public static boolean researchAdvancedTheory(EntityPlayer player) {
        return researchAdvancedTheory(DoraCapabilities.get(player));
    }

    /**
     * Attempts to research super theory. Requires advanced theory plus every tier-2 item unlocked.
     */
    public static boolean researchSuperTheory(EntityPlayer player) {
        return researchSuperTheory(DoraCapabilities.get(player));
    }

    /**
     * Unlocks a specific item if its theory prerequisite is met and the player can pay.
     *
     * @param cost points to spend (0 for recipe-gated items)
     */
    public static boolean unlockItem(EntityPlayer player, String itemKey, ResearchTier tier, int cost) {
        return unlockItem(DoraCapabilities.get(player), itemKey, tier, cost);
    }

    // --- Pure, data-only rule logic (server-authoritative and unit-testable) ---

    /** @see #researchBasicTheory(EntityPlayer) */
    public static boolean researchBasicTheory(IDoraPlayerData data) {
        if (data == null || data.hasBasicTheory()) {
            return false;
        }
        if (!spendPoints(data, DoraConfig.basicTheoryCost)) {
            return false;
        }
        data.setBasicTheory(true);
        return true;
    }

    /** @see #researchAdvancedTheory(EntityPlayer) */
    public static boolean researchAdvancedTheory(IDoraPlayerData data) {
        if (data == null || data.hasAdvancedTheory() || !data.hasBasicTheory()) {
            return false;
        }
        if (countUnlockedTier1(data) < ADVANCED_THEORY_REQUIRED_TIER1) {
            return false;
        }
        if (!spendPoints(data, DoraConfig.advancedTheoryCost)) {
            return false;
        }
        data.setAdvancedTheory(true);
        return true;
    }

    /** @see #researchSuperTheory(EntityPlayer) */
    public static boolean researchSuperTheory(IDoraPlayerData data) {
        if (data == null || data.hasSuperTheory() || !data.hasAdvancedTheory()) {
            return false;
        }
        if (!allTier2Unlocked(data)) {
            return false;
        }
        if (!spendPoints(data, DoraConfig.superTheoryCost)) {
            return false;
        }
        data.setSuperTheory(true);
        return true;
    }

    /** @see #unlockItem(EntityPlayer, String, ResearchTier, int) */
    public static boolean unlockItem(IDoraPlayerData data, String itemKey, ResearchTier tier, int cost) {
        if (data == null || data.isItemUnlocked(itemKey)) {
            return false;
        }
        if (!isTierUnlocked(data, tier)) {
            return false;
        }
        if (!spendPoints(data, cost)) {
            return false;
        }
        data.unlockItem(itemKey);
        return true;
    }

    private static boolean isTierUnlocked(IDoraPlayerData data, ResearchTier tier) {
        switch (tier) {
            case TIER_1:
                return data.hasBasicTheory();
            case TIER_2:
                return data.hasAdvancedTheory();
            case TIER_3:
                return data.hasSuperTheory();
            default:
                return true;
        }
    }

    private static boolean spendPoints(IDoraPlayerData data, int cost) {
        if (cost <= 0) {
            return true;
        }
        if (data.getTotalPoints() < cost) {
            return false;
        }
        data.setTotalPoints(data.getTotalPoints() - cost);
        return true;
    }

    private static int countUnlockedTier1(IDoraPlayerData data) {
        int count = 0;
        for (String key : ResearchTier.TIER_1.getItemKeys()) {
            if (data.isItemUnlocked(key)) {
                count++;
            }
        }
        return count;
    }

    private static boolean allTier2Unlocked(IDoraPlayerData data) {
        for (String key : ResearchTier.TIER_2.getItemKeys()) {
            if (!data.isItemUnlocked(key)) {
                return false;
            }
        }
        return true;
    }
}
