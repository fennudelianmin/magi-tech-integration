package com.dorapack.dorapack.robot;

import com.dorapack.dorapack.config.DoraConfig;
import net.minecraft.nbt.NBTTagCompound;

import java.util.EnumMap;
import java.util.Map;

/**
 * Holds a mining robot's installed upgrade-module levels and the derived stats computed from them.
 *
 * <p>Levels are the persisted source of truth (serialised to NBT); derived fields (battery capacity,
 * storage slots, energy-per-block, dig/move multipliers, fortune level, smelting/whitelist/ore flags)
 * are recomputed by {@link #recompute()} whenever levels change. All balance numbers come from
 * {@link DoraConfig} where configurable, matching the whitepaper values.</p>
 */
public final class RobotStats {

    private static final double BASE_ENERGY_PER_BLOCK = 20.0D;
    private static final int STORAGE_PER_LEVEL = 16;
    private static final int BATTERY_PER_LEVEL = 1000;
    /** Each energy-saver level multiplies energy cost by this factor (20% saving per level). */
    private static final double ENERGY_SAVER_FACTOR = 0.8D;
    /** Move-speed bonus fraction added per SPEED module level. */
    private static final double MOVE_SPEED_PER_LEVEL = 0.15D;
    /** Dig-speed bonus fraction added per DIG_SPEED module level. */
    private static final double DIG_SPEED_PER_LEVEL = 0.25D;
    private static final double BASE_MULTIPLIER = 1.0D;
    private static final String KEY_MODULES = "Modules";

    private final Map<RobotUpgrade, Integer> levels = new EnumMap<>(RobotUpgrade.class);

    private int maxEnergy;
    private int storageSlots;
    private double energyPerBlock;
    private double digSpeedMultiplier;
    private double moveSpeedMultiplier;
    private int fortuneLevel;
    private boolean smelting;
    private boolean whitelist;
    private boolean oreScanner;

    public RobotStats() {
        recompute();
    }

    public int getLevel(RobotUpgrade upgrade) {
        return levels.getOrDefault(upgrade, 0);
    }

    /**
     * Installs one level of the given upgrade if below its cap.
     *
     * @return {@code true} if a level was added
     */
    public boolean addLevel(RobotUpgrade upgrade) {
        int current = getLevel(upgrade);
        if (current >= upgrade.getMaxLevel()) {
            return false;
        }
        levels.put(upgrade, current + 1);
        recompute();
        return true;
    }

    /** Recomputes all derived stats from the current module levels. */
    public void recompute() {
        int batteryLevel = getLevel(RobotUpgrade.BATTERY);
        int storageLevel = getLevel(RobotUpgrade.STORAGE);
        int energySaverLevel = getLevel(RobotUpgrade.EFFICIENCY_ENERGY);
        int speedLevel = getLevel(RobotUpgrade.SPEED);
        int digLevel = getLevel(RobotUpgrade.DIG_SPEED);

        this.maxEnergy = DoraConfig.robotInitialEnergy + batteryLevel * BATTERY_PER_LEVEL;
        this.storageSlots = DoraConfig.robotInitialStorageSlots + storageLevel * STORAGE_PER_LEVEL;
        // Each energy-saver level reduces cost by 20% multiplicatively, floored to avoid free mining.
        this.energyPerBlock = BASE_ENERGY_PER_BLOCK * Math.pow(ENERGY_SAVER_FACTOR, energySaverLevel);
        this.moveSpeedMultiplier = BASE_MULTIPLIER + MOVE_SPEED_PER_LEVEL * speedLevel;
        this.digSpeedMultiplier = BASE_MULTIPLIER + DIG_SPEED_PER_LEVEL * digLevel;
        this.fortuneLevel = getLevel(RobotUpgrade.FORTUNE);
        this.smelting = getLevel(RobotUpgrade.SMELTING) > 0;
        this.whitelist = getLevel(RobotUpgrade.WHITELIST) > 0;
        this.oreScanner = getLevel(RobotUpgrade.ORE_SCANNER) > 0;
    }

    public int getMaxEnergy() {
        return maxEnergy;
    }

    public int getStorageSlots() {
        return storageSlots;
    }

    public double getEnergyPerBlock() {
        return energyPerBlock;
    }

    public double getDigSpeedMultiplier() {
        return digSpeedMultiplier;
    }

    public double getMoveSpeedMultiplier() {
        return moveSpeedMultiplier;
    }

    public int getFortuneLevel() {
        return fortuneLevel;
    }

    public boolean isSmelting() {
        return smelting;
    }

    public boolean isWhitelist() {
        return whitelist;
    }

    public boolean isOreScanner() {
        return oreScanner;
    }

    public void writeToNBT(NBTTagCompound compound) {
        NBTTagCompound modules = new NBTTagCompound();
        for (Map.Entry<RobotUpgrade, Integer> entry : levels.entrySet()) {
            modules.setInteger(entry.getKey().getId(), entry.getValue());
        }
        compound.setTag(KEY_MODULES, modules);
    }

    public void readFromNBT(NBTTagCompound compound) {
        levels.clear();
        NBTTagCompound modules = compound.getCompoundTag(KEY_MODULES);
        for (RobotUpgrade upgrade : RobotUpgrade.values()) {
            if (modules.hasKey(upgrade.getId())) {
                levels.put(upgrade, modules.getInteger(upgrade.getId()));
            }
        }
        recompute();
    }
}
