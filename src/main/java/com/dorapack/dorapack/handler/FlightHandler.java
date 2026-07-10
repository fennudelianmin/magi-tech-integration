package com.dorapack.dorapack.handler;

import com.dorapack.dorapack.achievement.AchievementManager;
import com.dorapack.dorapack.config.DoraConfig;
import com.dorapack.dorapack.item.ItemBambooCopter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages timed bamboo-copter flight. Flight is started via {@link #startFlight(EntityPlayer)}
 * (invoked from a client keybind packet on double-tap jump) and ticked each player tick.
 *
 * <p>Flight state is transient server-side data keyed by player UUID: remaining ticks. While active,
 * the player is granted vanilla flight (capabilities) and gentle lift; on expiry, flight is revoked
 * and a short landing stun (slowness) is applied. Rain cancels/blocks flight.</p>
 */
public final class FlightHandler {

    private static final Map<UUID, Integer> ACTIVE_FLIGHTS = new HashMap<>();

    private FlightHandler() {
    }

    /**
     * Attempts to begin a copter flight for the player.
     *
     * @return {@code true} if flight started
     */
    public static boolean startFlight(EntityPlayer player) {
        if (player.world.isRemote || !ItemBambooCopter.isEquipped(player)) {
            return false;
        }
        if (player.world.isRaining() && player.world.canSeeSky(player.getPosition())) {
            return false;
        }
        if (player.isCreative() || player.isSpectator()) {
            return false;
        }
        ACTIVE_FLIGHTS.put(player.getUniqueID(), DoraConfig.bambooCopterFlightSeconds * 20);
        player.capabilities.allowFlying = true;
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
        return true;
    }

    public static boolean isFlying(EntityPlayer player) {
        return ACTIVE_FLIGHTS.containsKey(player.getUniqueID());
    }

    static void tick(EntityPlayer player) {
        if (player.world.isRemote) {
            return;
        }
        UUID uuid = player.getUniqueID();
        Integer remaining = ACTIVE_FLIGHTS.get(uuid);
        if (remaining == null) {
            return;
        }
        boolean forceLand = !ItemBambooCopter.isEquipped(player)
                || (player.world.isRaining() && player.world.canSeeSky(player.getPosition()));
        if (forceLand || remaining <= 0) {
            endFlight(player);
            return;
        }
        ACTIVE_FLIGHTS.put(uuid, remaining - 1);
        AchievementManager.onFlightTicks(player, 1);
        damageCopter(player);
    }

    private static void damageCopter(EntityPlayer player) {
        if (player.ticksExisted % 20 != 0 || player.isCreative()) {
            return;
        }
        ItemStack head = player.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
        if (!head.isEmpty() && head.getItem() instanceof ItemBambooCopter) {
            head.damageItem(1, player);
        }
    }

    private static void endFlight(EntityPlayer player) {
        ACTIVE_FLIGHTS.remove(player.getUniqueID());
        if (!player.isCreative() && !player.isSpectator()) {
            player.capabilities.allowFlying = false;
            player.capabilities.isFlying = false;
            player.sendPlayerAbilities();
            // Landing stun.
            player.addPotionEffect(new net.minecraft.potion.PotionEffect(
                    net.minecraft.init.MobEffects.SLOWNESS, 30, 2, false, false));
        }
    }
}
