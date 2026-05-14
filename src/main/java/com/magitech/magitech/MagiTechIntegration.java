package com.magitech.magitech;

import com.magitech.magitech.block.ModBlocks;
import com.magitech.magitech.config.ModConfig;
import com.magitech.magitech.item.ModItems;
import com.magitech.magitech.network.PacketHandler;
import com.magitech.magitech.proxy.CommonProxy;
import com.magitech.magitech.recipe.ModRecipes;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
    modid = Tags.MOD_ID,
    name = Tags.MOD_NAME,
    version = Tags.VERSION,
    dependencies = "required-after:botania;required-after:appliedenergistics2"
)
public class MagiTechIntegration {

    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    @SidedProxy(
        clientSide = "com.magitech.magitech.proxy.ClientProxy",
        serverSide = "com.magitech.magitech.proxy.CommonProxy"
    )
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("Magi Tech Integration - 植物魔法 x AE2 联动模组启动中...");
        ModConfig.init(event.getSuggestedConfigurationFile());
        PacketHandler.register();
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ModRecipes.register();
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }
}
