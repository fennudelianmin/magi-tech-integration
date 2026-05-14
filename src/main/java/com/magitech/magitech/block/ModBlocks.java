package com.magitech.magitech.block;

import com.magitech.magitech.tile.*;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod.EventBusSubscriber
public class ModBlocks {

    public static final Block LIVINGROCK_CULTIVATOR = new BlockLivingrockCultivator();
    public static final Block FLOWER_CRAFTING_TABLE = new BlockFlowerCraftingTable();
    public static final Block RUNE_ALTAR_MACHINE = new BlockRuneAltarMachine();
    public static final Block TERRA_PLATE_ASSEMBLER = new BlockTerraPlateAssembler();
    public static final Block ELVEN_TRADE_MACHINE = new BlockElvenTradeMachine();

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(
            LIVINGROCK_CULTIVATOR,
            FLOWER_CRAFTING_TABLE,
            RUNE_ALTAR_MACHINE,
            TERRA_PLATE_ASSEMBLER,
            ELVEN_TRADE_MACHINE
        );

        GameRegistry.registerTileEntity(TileLivingrockCultivator.class, "magitech:livingrock_cultivator");
        GameRegistry.registerTileEntity(TileFlowerCraftingTable.class, "magitech:flower_crafting_table");
        GameRegistry.registerTileEntity(TileRuneAltarMachine.class, "magitech:rune_altar_machine");
        GameRegistry.registerTileEntity(TileTerraPlateAssembler.class, "magitech:terra_plate_assembler");
        GameRegistry.registerTileEntity(TileElvenTradeMachine.class, "magitech:elven_trade_machine");
    }

    @SubscribeEvent
    public static void registerItemBlocks(RegistryEvent.Register<net.minecraft.item.Item> event) {
        event.getRegistry().registerAll(
            new ItemBlock(LIVINGROCK_CULTIVATOR).setRegistryName(LIVINGROCK_CULTIVATOR.getRegistryName()),
            new ItemBlock(FLOWER_CRAFTING_TABLE).setRegistryName(FLOWER_CRAFTING_TABLE.getRegistryName()),
            new ItemBlock(RUNE_ALTAR_MACHINE).setRegistryName(RUNE_ALTAR_MACHINE.getRegistryName()),
            new ItemBlock(TERRA_PLATE_ASSEMBLER).setRegistryName(TERRA_PLATE_ASSEMBLER.getRegistryName()),
            new ItemBlock(ELVEN_TRADE_MACHINE).setRegistryName(ELVEN_TRADE_MACHINE.getRegistryName())
        );
    }
}
