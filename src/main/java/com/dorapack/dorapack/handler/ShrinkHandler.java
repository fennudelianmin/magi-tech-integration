package com.dorapack.dorapack.handler;

import com.dorapack.dorapack.achievement.AchievementManager;
import com.dorapack.dorapack.config.DoraConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the shrink-tunnel effect ("缩小隧道"). Shrinking is triggered when a player passes through the
 * large mouth of a tunnel and reset at the small mouth (or on expiry). While shrunk the player's
 * collision box is reduced and a speed bonus is applied; underwater shrinking is disallowed.
 *
 * <p>State is transient server-side data keyed by player UUID (remaining ticks). This mirrors the
 * {@link FlightHandler} approach and avoids persisting a short-lived effect. On expiry the player's
 * size is restored.</p>
 */
public final class ShrinkHandler {

    private static final float SHRUNK_WIDTH = 0.3F;
    private static final float SHRUNK_HEIGHT = 0.6F;
    private static final float NORMAL_WIDTH = 0.6F;
    private static final float NORMAL_HEIGHT = 1.8F;

    private static final Map<UUID, Integer> ACTIVE = new HashMap<>();

    /** {@code Entity#setSize(float,float)} is protected; resolved once via SRG-aware reflection. */
    private static final Method SET_SIZE = ObfuscationReflectionHelper.findMethod(
            net.minecraft.entity.Entity.class, "func_70105_a", void.class, float.class, float.class);

    private ShrinkHandler() {
    }

    private static void setSize(EntityPlayer player, float width, float height) {
        try {
            SET_SIZE.invoke(player, width, height);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to invoke Entity#setSize via reflection", e);
        }
    }

    public static boolean isShrunk(EntityPlayer player) {
        return ACTIVE.containsKey(player.getUniqueID());
    }

    /**
     * Begins the shrink effect for a player.
     *
     * @return {@code true} if the effect started
     */
    public static boolean shrink(EntityPlayer player) {
        if (player.world.isRemote || isShrunk(player) || player.isInWater()) {
            return false;
        }
        ACTIVE.put(player.getUniqueID(), DoraConfig.shrinkTunnelDurationSeconds * 20);
        setSize(player, SHRUNK_WIDTH, SHRUNK_HEIGHT);
        applySpeed(player);
        AchievementManager.onShrinkRoom(player);
        return true;
    }

    /** Restores the player to normal size and clears the effect. */
    public static void restore(EntityPlayer player) {
        if (ACTIVE.remove(player.getUniqueID()) != null) {
            setSize(player, NORMAL_WIDTH, NORMAL_HEIGHT);
        }
    }

    static void tick(EntityPlayer player) {
        if (player.world.isRemote) {
            return;
        }
        UUID uuid = player.getUniqueID();
        Integer remaining = ACTIVE.get(uuid);
        if (remaining == null) {
            return;
        }
        if (remaining <= 0 || player.isInWater()) {
            restore(player);
            return;
        }
        ACTIVE.put(uuid, remaining - 1);
        if (remaining % 40 == 0) {
            applySpeed(player);
        }
    }

    private static void applySpeed(EntityPlayer player) {
        int amplifier = Math.max(0, DoraConfig.shrinkTunnelSpeedBonusPercent / 20);
        player.addPotionEffect(new PotionEffect(MobEffects.SPEED, 60, amplifier, false, false));
    }
}
