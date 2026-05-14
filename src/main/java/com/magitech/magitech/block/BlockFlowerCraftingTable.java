package com.magitech.magitech.block;

import com.magitech.magitech.tile.TileFlowerCraftingTable;
import net.minecraft.block.material.Material;

public class BlockFlowerCraftingTable extends BlockBase {
    public BlockFlowerCraftingTable() {
        super(Material.ROCK, "flower_crafting_table", TileFlowerCraftingTable::new);
    }
}
