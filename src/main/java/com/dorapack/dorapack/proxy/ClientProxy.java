package com.dorapack.dorapack.proxy;

import com.dorapack.dorapack.client.ClientInputHandler;
import com.dorapack.dorapack.client.ClientRegistryHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * Client proxy. Item/block model registration is handled by {@code ClientRegistryHandler}
 * listening for {@code ModelRegistryEvent}, so this proxy only handles side-specific init hooks.
 */
public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        MinecraftForge.EVENT_BUS.register(new ClientInputHandler());
        ClientRegistryHandler.registerEntityRenderers();
    }
}
