package com.magitech.magitech.item;

import com.magitech.magitech.creativetab.MagiTechTab;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber
public class ModItems {

    // 中间合成物品
    public static final Item RUNE_ALTAR_CORE = new Item()
        .setRegistryName("magitech", "rune_altar_core")
        .setTranslationKey("magitech.rune_altar_core")
        .setCreativeTab(MagiTechTab.INSTANCE);
    public static final Item TERRA_ASSEMBLY_CORE = new Item()
        .setRegistryName("magitech", "terra_assembly_core")
        .setTranslationKey("magitech.terra_assembly_core")
        .setCreativeTab(MagiTechTab.INSTANCE);
    public static final Item ELVEN_TRADE_CORE = new Item()
        .setRegistryName("magitech", "elven_trade_core")
        .setTranslationKey("magitech.elven_trade_core")
        .setCreativeTab(MagiTechTab.INSTANCE);

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(
            RUNE_ALTAR_CORE,
            TERRA_ASSEMBLY_CORE,
            ELVEN_TRADE_CORE
        );
    }
}
