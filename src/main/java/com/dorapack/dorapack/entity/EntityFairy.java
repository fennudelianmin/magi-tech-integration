package com.dorapack.dorapack.entity;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Momotaro fairy ("小精灵"). A short-lived flying helper summoned by the flute. Every second it
 * damages the nearest hostile mob within range for 1 point. Its lifetime is bounded and can be
 * extended (up to a cap) by the owner feeding rice balls.
 *
 * <p>Uses a bounded search radius and a 20-tick attack interval — no global scanning — per the
 * performance constraints in the design document.</p>
 */
public class EntityFairy extends EntityLiving {

    private static final double SEARCH_RADIUS = 6.0D;
    private static final int ATTACK_INTERVAL_TICKS = 20;
    private static final float ATTACK_DAMAGE = 1.0F;
    private static final int MAX_LIFETIME_TICKS = 8 * 2 * 60 * 20;
    private static final int DEFAULT_LIFETIME_TICKS = 2 * 60 * 20;
    private static final String KEY_OWNER_MOST = "OwnerMost";
    private static final String KEY_OWNER_LEAST = "OwnerLeast";
    private static final String KEY_REMAINING = "RemainingTicks";

    private UUID ownerUuid;
    private int remainingTicks = DEFAULT_LIFETIME_TICKS;

    public EntityFairy(World world) {
        super(world);
        setSize(0.4F, 0.4F);
        this.noClip = true;
        this.experienceValue = 0;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(4.0D);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(SEARCH_RADIUS);
    }

    public void setOwner(EntityPlayer player) {
        this.ownerUuid = player.getUniqueID();
    }

    @Nullable
    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    /** Adds lifetime (in ticks), capped at the maximum. Used when feeding rice balls. */
    public void extendLifetime(int ticks) {
        this.remainingTicks = Math.min(MAX_LIFETIME_TICKS, this.remainingTicks + ticks);
    }

    @Override
    public boolean hasNoGravity() {
        return true;
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (world.isRemote) {
            return;
        }
        if (--remainingTicks <= 0) {
            setDead();
            return;
        }
        followOwner();
        if (ticksExisted % ATTACK_INTERVAL_TICKS == 0) {
            attackNearestMob();
        }
    }

    private void followOwner() {
        if (ownerUuid == null) {
            return;
        }
        EntityPlayer owner = world.getPlayerEntityByUUID(ownerUuid);
        if (owner == null) {
            return;
        }
        double distSq = getDistanceSq(owner);
        if (distSq > 100.0D) {
            setPositionAndUpdate(owner.posX, owner.posY + 1.5D, owner.posZ);
        }
    }

    private void attackNearestMob() {
        AxisAlignedBB area = getEntityBoundingBox().grow(SEARCH_RADIUS);
        List<EntityLivingBase> mobs = world.getEntitiesWithinAABB(EntityLivingBase.class, area,
                e -> e instanceof IMob && e.isEntityAlive());
        EntityLivingBase closest = null;
        double closestDist = Double.MAX_VALUE;
        for (EntityLivingBase mob : mobs) {
            double dist = getDistanceSq(mob);
            if (dist < closestDist) {
                closestDist = dist;
                closest = mob;
            }
        }
        if (closest != null) {
            closest.attackEntityFrom(DamageSource.MAGIC, ATTACK_DAMAGE);
        }
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        if (ownerUuid != null) {
            compound.setLong(KEY_OWNER_MOST, ownerUuid.getMostSignificantBits());
            compound.setLong(KEY_OWNER_LEAST, ownerUuid.getLeastSignificantBits());
        }
        compound.setInteger(KEY_REMAINING, remainingTicks);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        if (compound.hasKey(KEY_OWNER_MOST)) {
            this.ownerUuid = new UUID(compound.getLong(KEY_OWNER_MOST), compound.getLong(KEY_OWNER_LEAST));
        }
        if (compound.hasKey(KEY_REMAINING)) {
            this.remainingTicks = compound.getInteger(KEY_REMAINING);
        }
    }

    /** Fairies are invulnerable to normal damage; they simply expire. */
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }
}
