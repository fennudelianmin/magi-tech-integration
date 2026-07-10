package com.dorapack.dorapack.network;

import com.dorapack.dorapack.Reference;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Central SimpleImpl network channel for DoraPack.
 */
public final class PacketHandler {

    public static final SimpleNetworkWrapper INSTANCE =
            NetworkRegistry.INSTANCE.newSimpleChannel(Reference.MOD_ID);

    private static int nextId;

    private PacketHandler() {
    }

    public static void register() {
        INSTANCE.registerMessage(PacketSyncPlayerData.Handler.class, PacketSyncPlayerData.class,
                nextId++, Side.CLIENT);
        INSTANCE.registerMessage(PacketResearchAction.Handler.class, PacketResearchAction.class,
                nextId++, Side.SERVER);
        INSTANCE.registerMessage(PacketCopterFlight.Handler.class, PacketCopterFlight.class,
                nextId++, Side.SERVER);
    }
}
