package com.dorapack.dorapack.proxy;

import com.dorapack.dorapack.DoraPackMod;
import com.dorapack.dorapack.capability.CapabilityRegistrar;
import com.dorapack.dorapack.client.gui.DoraGuiHandler;
import com.dorapack.dorapack.config.DoraConfig;
import com.dorapack.dorapack.network.PacketHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

/**
 * Common (both-sides) lifecycle logic. Client and server proxies extend this and add side specifics.
 */
public class CommonProxy implements IProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        DoraConfig.load(event.getModConfigurationDirectory());
        CapabilityRegistrar.register();
        PacketHandler.register();
        NetworkRegistry.INSTANCE.registerGuiHandler(DoraPackMod.instance, new DoraGuiHandler());
    }

    @Override
    public void init(FMLInitializationEvent event) {
        // Recipes and events are registered via @ObjectHolder / @SubscribeEvent registrars.
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        // Reserved for cross-mod interop wiring (e.g. IC2) that needs all mods loaded.
    }
}
