package com.dorapack.dorapack.world;

import com.dorapack.dorapack.config.DoraConfig;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.items.ItemStackHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * World-level persistent storage of every player's shared "four-dimensional bag" inventory,
 * keyed by player UUID. One instance per world save, retrieved via {@link #get(World)}.
 *
 * <p>Using {@link WorldSavedData} (instead of per-item NBT) means all bag stacks a player owns
 * share the same backing inventory and survive death without dropping.</p>
 */
public class PlayerBagData extends WorldSavedData {

    private static final String DATA_NAME = "dorapack_bags";
    private static final String KEY_ENTRIES = "entries";
    private static final String KEY_UUID = "uuid";
    private static final String KEY_SLOTS = "slots";
    private static final String KEY_INV = "inv";

    private final Map<UUID, ItemStackHandler> bags = new HashMap<>();

    public PlayerBagData() {
        super(DATA_NAME);
    }

    public PlayerBagData(String name) {
        super(name);
    }

    public static PlayerBagData get(World world) {
        MapStorage storage = world.getMapStorage();
        PlayerBagData data = (PlayerBagData) storage.getOrLoadData(PlayerBagData.class, DATA_NAME);
        if (data == null) {
            data = new PlayerBagData();
            storage.setData(DATA_NAME, data);
        }
        return data;
    }

    public ItemStackHandler getBag(UUID uuid) {
        return bags.computeIfAbsent(uuid, k -> new SavingHandler(DoraConfig.bagInitialSlots));
    }

    /**
     * Expands a player's bag by the given slot count, preserving existing contents,
     * capped at {@link DoraConfig#bagMaxSlots}.
     *
     * @return the new slot count
     */
    public int expandBag(UUID uuid, int extraSlots) {
        ItemStackHandler current = getBag(uuid);
        int newSize = Math.min(DoraConfig.bagMaxSlots, current.getSlots() + extraSlots);
        if (newSize == current.getSlots()) {
            return current.getSlots();
        }
        SavingHandler expanded = new SavingHandler(newSize);
        for (int i = 0; i < current.getSlots(); i++) {
            expanded.setStackInSlot(i, current.getStackInSlot(i));
        }
        bags.put(uuid, expanded);
        markDirty();
        return newSize;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        bags.clear();
        NBTTagList list = nbt.getTagList(KEY_ENTRIES, 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            UUID uuid = UUID.fromString(entry.getString(KEY_UUID));
            int slots = entry.getInteger(KEY_SLOTS);
            SavingHandler handler = new SavingHandler(Math.max(1, slots));
            handler.deserializeNBT(entry.getCompoundTag(KEY_INV));
            bags.put(uuid, handler);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (Map.Entry<UUID, ItemStackHandler> entry : bags.entrySet()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString(KEY_UUID, entry.getKey().toString());
            tag.setInteger(KEY_SLOTS, entry.getValue().getSlots());
            tag.setTag(KEY_INV, entry.getValue().serializeNBT());
            list.appendTag(tag);
        }
        nbt.setTag(KEY_ENTRIES, list);
        return nbt;
    }

    /**
     * ItemStackHandler that flags the owning WorldSavedData dirty whenever its contents change.
     */
    private final class SavingHandler extends ItemStackHandler {

        SavingHandler(int size) {
            super(size);
        }

        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
        }
    }
}
