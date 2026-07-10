package com.dorapack.dorapack.init;

import com.dorapack.dorapack.Reference;
import com.dorapack.dorapack.block.BlockAnywhereDoor;
import com.dorapack.dorapack.block.BlockChargingStation;
import com.dorapack.dorapack.block.BlockDoorHubCore;
import com.dorapack.dorapack.block.BlockResearchTable;
import com.dorapack.dorapack.block.BlockShrinkGate;
import com.dorapack.dorapack.block.tile.TileAnywhereDoor;
import com.dorapack.dorapack.block.tile.TileChargingStation;
import com.dorapack.dorapack.block.tile.TileDoorHubCore;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Central registrar and holder for DoraPack blocks and their item-blocks.
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public final class DoraBlocks {

    public static final String NAME_ANYWHERE_DOOR = "anywhere_door";
    public static final String NAME_RESEARCH_TABLE_SPROUT = "research_table_sprout";
    public static final String NAME_RESEARCH_TABLE_ENDGAME = "research_table_endgame";
    public static final String NAME_CHARGING_STATION = "charging_station";
    public static final String NAME_SHRINK_GATE_LARGE = "shrink_gate_large";
    public static final String NAME_SHRINK_GATE_SMALL = "shrink_gate_small";
    public static final String NAME_DOOR_HUB_CORE = "door_hub_core";

    public static Block ANYWHERE_DOOR;
    public static Block RESEARCH_TABLE_SPROUT;
    public static Block RESEARCH_TABLE_ENDGAME;
    public static Block CHARGING_STATION;
    public static Block SHRINK_GATE_LARGE;
    public static Block SHRINK_GATE_SMALL;
    public static Block DOOR_HUB_CORE;

    private static final List<Block> BLOCKS = new ArrayList<>();

    private DoraBlocks() {
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        ANYWHERE_DOOR = new BlockAnywhereDoor(NAME_ANYWHERE_DOOR);
        RESEARCH_TABLE_SPROUT = new BlockResearchTable(NAME_RESEARCH_TABLE_SPROUT, false);
        RESEARCH_TABLE_ENDGAME = new BlockResearchTable(NAME_RESEARCH_TABLE_ENDGAME, true);
        CHARGING_STATION = new BlockChargingStation(NAME_CHARGING_STATION);
        SHRINK_GATE_LARGE = new BlockShrinkGate(NAME_SHRINK_GATE_LARGE, true);
        SHRINK_GATE_SMALL = new BlockShrinkGate(NAME_SHRINK_GATE_SMALL, false);
        DOOR_HUB_CORE = new BlockDoorHubCore(NAME_DOOR_HUB_CORE);
        BLOCKS.clear();
        BLOCKS.add(ANYWHERE_DOOR);
        BLOCKS.add(RESEARCH_TABLE_SPROUT);
        BLOCKS.add(RESEARCH_TABLE_ENDGAME);
        BLOCKS.add(CHARGING_STATION);
        BLOCKS.add(SHRINK_GATE_LARGE);
        BLOCKS.add(SHRINK_GATE_SMALL);
        BLOCKS.add(DOOR_HUB_CORE);
        for (Block block : BLOCKS) {
            event.getRegistry().register(block);
        }
        GameRegistry.registerTileEntity(TileAnywhereDoor.class,
                new ResourceLocation(Reference.MOD_ID, NAME_ANYWHERE_DOOR));
        GameRegistry.registerTileEntity(TileChargingStation.class,
                new ResourceLocation(Reference.MOD_ID, NAME_CHARGING_STATION));
        GameRegistry.registerTileEntity(TileDoorHubCore.class,
                new ResourceLocation(Reference.MOD_ID, NAME_DOOR_HUB_CORE));
    }

    /** Called from the item registry event to register matching item-blocks. */
    public static void registerItemBlocks(IForgeRegistry<Item> registry) {
        for (Block block : BLOCKS) {
            ItemBlock itemBlock = new ItemBlock(block);
            itemBlock.setRegistryName(block.getRegistryName());
            registry.register(itemBlock);
        }
    }
}
