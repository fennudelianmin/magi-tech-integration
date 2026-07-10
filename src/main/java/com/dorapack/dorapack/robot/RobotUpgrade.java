package com.dorapack.dorapack.robot;

import java.util.Objects;

/**
 * The nine mining-robot upgrade modules from the design document. Each has a stable id (used as the
 * item sub-type / registry suffix and NBT key) and, where relevant, a maximum stack level.
 *
 * <p>Effects are applied by {@link RobotStats#recompute} reading the installed module levels, so the
 * enum only describes identity and stacking limits, not behaviour.</p>
 */
public enum RobotUpgrade {

    /** +1000 EU battery capacity per level. */
    BATTERY("battery", 8),
    /** +16 storage slots per level. */
    STORAGE("storage", 8),
    /** Fortune enchant level on mined ores, capped at 3. */
    FORTUNE("fortune", 3),
    /** Auto-smelt mined ores into ingots (requires fuel). */
    SMELTING("smelting", 1),
    /** Whitelist filter: only keep configured items. */
    WHITELIST("whitelist", 1),
    /** Prioritise ore blocks when mining. */
    ORE_SCANNER("ore_scanner", 1),
    /** -20% energy use per level. */
    EFFICIENCY_ENERGY("energy_saver", 3),
    /** +15% movement speed per level. */
    SPEED("speed", 3),
    /** +25% dig speed per level. */
    DIG_SPEED("dig_speed", 3);

    private final String id;
    private final int maxLevel;

    RobotUpgrade(String id, int maxLevel) {
        this.id = id;
        this.maxLevel = maxLevel;
    }

    public String getId() {
        return id;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    /** @return the upgrade whose id matches, or {@code null} if none. */
    public static RobotUpgrade byId(String id) {
        for (RobotUpgrade upgrade : values()) {
            if (Objects.equals(upgrade.id, id)) {
                return upgrade;
            }
        }
        return null;
    }
}
