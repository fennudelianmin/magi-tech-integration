package com.dorapack.dorapack.entity;

import com.dorapack.dorapack.achievement.AchievementManager;
import com.dorapack.dorapack.robot.RobotStats;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Mining robot ("采矿机器人"). A tier-3, owner-bound helper that mines within a bounded radius around
 * itself, storing drops in an internal inventory and consuming IC2-derived energy per block. Its
 * capabilities scale with installed {@link RobotStats upgrade modules}. When energy is exhausted it
 * idles until recharged at a charging station.
 *
 * <p>Per the design document's performance rules, the robot never performs global scanning: mining
 * targets are chosen by {@link com.dorapack.dorapack.robot.RobotMiningTask} within a small fixed
 * radius, and work is throttled by {@link #workCooldown}.</p>
 */
public class EntityMiningRobot extends EntityCreature {

    private static final String KEY_OWNER_MOST = "OwnerMost";
    private static final String KEY_OWNER_LEAST = "OwnerLeast";
    private static final String KEY_ENERGY = "Energy";
    private static final String KEY_ITEMS = "Items";
    private static final int WORK_INTERVAL_TICKS = 20;

    private final RobotStats stats = new RobotStats();
    private NonNullList<ItemStack> inventory = NonNullList.withSize(stats.getStorageSlots(), ItemStack.EMPTY);
    private UUID ownerUuid;
    private int energy;
    private int workCooldown;

    public EntityMiningRobot(World world) {
        super(world);
        setSize(0.6F, 0.9F);
        this.energy = stats.getMaxEnergy();
        this.experienceValue = 0;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(20.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.25D);
        getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
    }

    public RobotStats getStats() {
        return stats;
    }

    public void setOwner(EntityPlayer player) {
        this.ownerUuid = player.getUniqueID();
    }

    @Nullable
    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    @Nullable
    public EntityPlayer getOwner() {
        return ownerUuid == null ? null : world.getPlayerEntityByUUID(ownerUuid);
    }

    public int getEnergy() {
        return energy;
    }

    public void setEnergy(int energy) {
        this.energy = Math.max(0, Math.min(stats.getMaxEnergy(), energy));
    }

    /** Adds energy up to capacity; returns the amount actually accepted. */
    public int addEnergy(int amount) {
        int accepted = Math.min(amount, stats.getMaxEnergy() - energy);
        energy += accepted;
        return accepted;
    }

    public boolean hasEnergyForBlock() {
        return energy >= stats.getEnergyPerBlock();
    }

    public void consumeBlockEnergy() {
        energy = Math.max(0, energy - (int) Math.ceil(stats.getEnergyPerBlock()));
    }

    public NonNullList<ItemStack> getInventory() {
        resizeInventoryIfNeeded();
        return inventory;
    }

    /** Grows the backing inventory when a storage upgrade increased the slot count. */
    public void resizeInventoryIfNeeded() {
        int target = stats.getStorageSlots();
        if (inventory.size() == target) {
            return;
        }
        NonNullList<ItemStack> resized = NonNullList.withSize(target, ItemStack.EMPTY);
        for (int i = 0; i < Math.min(inventory.size(), target); i++) {
            resized.set(i, inventory.get(i));
        }
        this.inventory = resized;
    }

    /**
     * Attempts to insert a stack into the internal inventory.
     *
     * @return the remaining stack that did not fit (empty if fully stored)
     */
    public ItemStack storeItem(ItemStack stack) {
        resizeInventoryIfNeeded();
        for (int i = 0; i < inventory.size() && !stack.isEmpty(); i++) {
            ItemStack slot = inventory.get(i);
            if (slot.isEmpty()) {
                inventory.set(i, stack.copy());
                return ItemStack.EMPTY;
            }
            if (ItemStack.areItemsEqual(slot, stack) && ItemStack.areItemStackTagsEqual(slot, stack)) {
                int room = slot.getMaxStackSize() - slot.getCount();
                int moved = Math.min(room, stack.getCount());
                slot.grow(moved);
                stack.shrink(moved);
            }
        }
        return stack;
    }

    public boolean isInventoryFull() {
        resizeInventoryIfNeeded();
        for (ItemStack slot : inventory) {
            if (slot.isEmpty() || slot.getCount() < slot.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (world.isRemote) {
            return;
        }
        if (workCooldown > 0) {
            workCooldown--;
            return;
        }
        workCooldown = WORK_INTERVAL_TICKS;
        if (!hasEnergyForBlock() || isInventoryFull()) {
            return;
        }
        boolean mined = com.dorapack.dorapack.robot.RobotMiningTask.tick(this);
        if (mined) {
            consumeBlockEnergy();
            EntityPlayer owner = getOwner();
            if (owner != null) {
                AchievementManager.onRobotWorkTicks(owner, WORK_INTERVAL_TICKS);
            }
        }
    }

    /** Right-clicking with the owner opens the robot GUI (handled by the interact item/handler). */
    @Override
    public boolean processInteract(EntityPlayer player, net.minecraft.util.EnumHand hand) {
        if (!world.isRemote && isOwner(player)) {
            player.openGui(com.dorapack.dorapack.DoraPackMod.instance,
                    com.dorapack.dorapack.client.gui.DoraGuiHandler.GUI_ROBOT,
                    world, getEntityId(), 0, 0);
            return true;
        }
        return super.processInteract(player, hand);
    }

    public boolean isOwner(EntityPlayer player) {
        return ownerUuid != null && ownerUuid.equals(player.getUniqueID());
    }

    /** Only the owner (or creative players) may damage/destroy the robot; drops its inventory. */
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (source.getTrueSource() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) source.getTrueSource();
            if (isOwner(player) || player.isCreative()) {
                return super.attackEntityFrom(source, amount);
            }
        }
        return false;
    }

    @Override
    protected void onDeathUpdate() {
        if (!world.isRemote && deathTime == 0) {
            dropInventory();
        }
        super.onDeathUpdate();
    }

    private void dropInventory() {
        for (ItemStack stack : getInventory()) {
            if (!stack.isEmpty()) {
                entityDropItem(stack, 0.5F);
            }
        }
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        if (ownerUuid != null) {
            compound.setLong(KEY_OWNER_MOST, ownerUuid.getMostSignificantBits());
            compound.setLong(KEY_OWNER_LEAST, ownerUuid.getLeastSignificantBits());
        }
        compound.setInteger(KEY_ENERGY, energy);
        stats.writeToNBT(compound);
        NBTTagCompound items = new NBTTagCompound();
        ItemStackHelper.saveAllItems(items, getInventory());
        compound.setTag(KEY_ITEMS, items);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        if (compound.hasKey(KEY_OWNER_MOST)) {
            this.ownerUuid = new UUID(compound.getLong(KEY_OWNER_MOST), compound.getLong(KEY_OWNER_LEAST));
        }
        stats.readFromNBT(compound);
        resizeInventoryIfNeeded();
        ItemStackHelper.loadAllItems(compound.getCompoundTag(KEY_ITEMS), inventory);
        this.energy = Math.min(compound.getInteger(KEY_ENERGY), stats.getMaxEnergy());
    }
}
