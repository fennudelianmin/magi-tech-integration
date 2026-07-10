package com.dorapack.dorapack.item;

import com.dorapack.dorapack.item.base.ItemResearchLocked;
import com.dorapack.dorapack.research.ResearchKeys;

/**
 * Emergency pill ("应急药丸"). Passive: while in the player's inventory and unlocked, it auto-triggers
 * when a hit would drop the player to 2 hearts or below (see {@code CombatEventHandler}). It heals,
 * grants short buffs and is consumed. Daily-limited.
 *
 * <p>This item has no active right-click behaviour; all logic lives in the combat event handler so
 * the effect can intercept lethal damage precisely.</p>
 */
public class ItemEmergencyPill extends ItemResearchLocked {

    public ItemEmergencyPill(String name) {
        super(name, 16, ResearchKeys.EMERGENCY_PILL);
    }
}
