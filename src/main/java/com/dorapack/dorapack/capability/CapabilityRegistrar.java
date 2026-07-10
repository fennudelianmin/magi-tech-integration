package com.dorapack.dorapack.capability;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.util.Constants;

import java.util.Map;

/**
 * Registers the {@link IDoraPlayerData} capability and defines its NBT (de)serialization.
 */
public final class CapabilityRegistrar {

    private static final String KEY_TOTAL_POINTS = "totalPoints";
    private static final String KEY_TODAY_POINTS = "pointsToday";
    private static final String KEY_LAST_DAY = "lastDay";
    private static final String KEY_BASIC = "basicTheory";
    private static final String KEY_ADVANCED = "advancedTheory";
    private static final String KEY_SUPER = "superTheory";
    private static final String KEY_UNLOCKED = "unlockedItems";
    private static final String KEY_ACHIEVEMENTS = "achievements";
    private static final String KEY_DAILY_USAGE = "dailyUsage";
    private static final String KEY_COUNTERS = "counters";
    private static final String KEY_MAP_KEY = "k";
    private static final String KEY_MAP_VALUE = "v";

    private CapabilityRegistrar() {
    }

    public static void register() {
        CapabilityManager.INSTANCE.register(IDoraPlayerData.class, new Storage(), DoraPlayerData::new);
    }

    /**
     * NBT storage for the player data capability.
     */
    public static class Storage implements Capability.IStorage<IDoraPlayerData> {

        @Override
        public NBTBase writeNBT(Capability<IDoraPlayerData> capability, IDoraPlayerData instance, EnumFacing side) {
            DoraPlayerData data = (DoraPlayerData) instance;
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger(KEY_TOTAL_POINTS, data.getTotalPoints());
            tag.setInteger(KEY_TODAY_POINTS, data.getPointsGainedToday());
            tag.setLong(KEY_LAST_DAY, data.getLastDay());
            tag.setBoolean(KEY_BASIC, data.hasBasicTheory());
            tag.setBoolean(KEY_ADVANCED, data.hasAdvancedTheory());
            tag.setBoolean(KEY_SUPER, data.hasSuperTheory());
            tag.setTag(KEY_UNLOCKED, writeStringSet(data.getUnlockedItems()));
            tag.setTag(KEY_ACHIEVEMENTS, writeStringSet(data.getCompletedAchievements()));
            tag.setTag(KEY_DAILY_USAGE, writeIntMap(data.getDailyUsageMap()));
            tag.setTag(KEY_COUNTERS, writeLongMap(data.getCounterMap()));
            return tag;
        }

        @Override
        public void readNBT(Capability<IDoraPlayerData> capability, IDoraPlayerData instance, EnumFacing side, NBTBase nbt) {
            DoraPlayerData data = (DoraPlayerData) instance;
            NBTTagCompound tag = (NBTTagCompound) nbt;
            data.setTotalPoints(tag.getInteger(KEY_TOTAL_POINTS));
            data.setPointsGainedToday(tag.getInteger(KEY_TODAY_POINTS));
            data.setLastDay(tag.getLong(KEY_LAST_DAY));
            data.setBasicTheory(tag.getBoolean(KEY_BASIC));
            data.setAdvancedTheory(tag.getBoolean(KEY_ADVANCED));
            data.setSuperTheory(tag.getBoolean(KEY_SUPER));
            readStringSet(tag.getTagList(KEY_UNLOCKED, Constants.NBT.TAG_STRING), data.getUnlockedItems());
            readStringSet(tag.getTagList(KEY_ACHIEVEMENTS, Constants.NBT.TAG_STRING), data.getCompletedAchievements());
            readIntMap(tag.getTagList(KEY_DAILY_USAGE, Constants.NBT.TAG_COMPOUND), data.getDailyUsageMap());
            readLongMap(tag.getTagList(KEY_COUNTERS, Constants.NBT.TAG_COMPOUND), data.getCounterMap());
        }

        private NBTTagList writeStringSet(Iterable<String> values) {
            NBTTagList list = new NBTTagList();
            for (String value : values) {
                list.appendTag(new NBTTagString(value));
            }
            return list;
        }

        private void readStringSet(NBTTagList list, java.util.Set<String> target) {
            target.clear();
            for (int i = 0; i < list.tagCount(); i++) {
                target.add(list.getStringTagAt(i));
            }
        }

        private NBTTagList writeIntMap(Map<String, Integer> map) {
            NBTTagList list = new NBTTagList();
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                NBTTagCompound entryTag = new NBTTagCompound();
                entryTag.setString(KEY_MAP_KEY, entry.getKey());
                entryTag.setInteger(KEY_MAP_VALUE, entry.getValue());
                list.appendTag(entryTag);
            }
            return list;
        }

        private void readIntMap(NBTTagList list, Map<String, Integer> target) {
            target.clear();
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound entryTag = list.getCompoundTagAt(i);
                target.put(entryTag.getString(KEY_MAP_KEY), entryTag.getInteger(KEY_MAP_VALUE));
            }
        }

        private NBTTagList writeLongMap(Map<String, Long> map) {
            NBTTagList list = new NBTTagList();
            for (Map.Entry<String, Long> entry : map.entrySet()) {
                NBTTagCompound entryTag = new NBTTagCompound();
                entryTag.setString(KEY_MAP_KEY, entry.getKey());
                entryTag.setLong(KEY_MAP_VALUE, entry.getValue());
                list.appendTag(entryTag);
            }
            return list;
        }

        private void readLongMap(NBTTagList list, Map<String, Long> target) {
            target.clear();
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound entryTag = list.getCompoundTagAt(i);
                target.put(entryTag.getString(KEY_MAP_KEY), entryTag.getLong(KEY_MAP_VALUE));
            }
        }
    }
}
