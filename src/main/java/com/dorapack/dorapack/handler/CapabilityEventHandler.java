package com.dorapack.dorapack.handler;

import com.dorapack.dorapack.Reference;
import com.dorapack.dorapack.capability.DoraCapabilities;
import com.dorapack.dorapack.capability.DoraPlayerDataProvider;
import com.dorapack.dorapack.capability.IDoraPlayerData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Attaches the {@link IDoraPlayerData} capability to players and preserves it across death/dimension
 * changes (the vanilla clone event wipes capabilities otherwise).
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public final class CapabilityEventHandler {

    private static final ResourceLocation PLAYER_DATA_ID =
            new ResourceLocation(Reference.MOD_ID, "player_data");

    private CapabilityEventHandler() {
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<net.minecraft.entity.Entity> event) {
        if (event.getObject() instanceof EntityPlayer) {
            event.addCapability(PLAYER_DATA_ID, new DoraPlayerDataProvider());
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        IDoraPlayerData original = DoraCapabilities.get(event.getOriginal());
        IDoraPlayerData clone = DoraCapabilities.get(event.getEntityPlayer());
        if (original == null || clone == null) {
            return;
        }
        // Copy via NBT so the whole state (research, achievements, counters) survives respawn.
        NBTTagCompound nbt = (NBTTagCompound) DoraCapabilities.PLAYER_DATA.getStorage()
                .writeNBT(DoraCapabilities.PLAYER_DATA, original, null);
        DoraCapabilities.PLAYER_DATA.getStorage()
                .readNBT(DoraCapabilities.PLAYER_DATA, clone, null, nbt);
    }
}
