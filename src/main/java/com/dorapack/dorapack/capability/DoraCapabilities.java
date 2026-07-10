package com.dorapack.dorapack.capability;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;

import javax.annotation.Nullable;

/**
 * Holds the injected capability instance and provides a convenient accessor.
 */
public final class DoraCapabilities {

    @CapabilityInject(IDoraPlayerData.class)
    public static Capability<IDoraPlayerData> PLAYER_DATA = null;

    private DoraCapabilities() {
    }

    /**
     * @return the player's DoraPack data, or {@code null} if the capability is unavailable
     * (should not happen for real players but guards against fake players).
     */
    @Nullable
    public static IDoraPlayerData get(EntityPlayer player) {
        if (player == null || PLAYER_DATA == null) {
            return null;
        }
        return player.getCapability(PLAYER_DATA, null);
    }
}
