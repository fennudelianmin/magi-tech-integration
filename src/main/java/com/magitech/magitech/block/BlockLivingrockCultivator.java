package com.magitech.magitech.block;

import com.magitech.magitech.tile.TileLivingrockCultivator;
import net.minecraft.block.material.Material;

public class BlockLivingrockCultivator extends BlockBase {
    public BlockLivingrockCultivator() {
        super(Material.ROCK, "livingrock_cultivator", TileLivingrockCultivator::new);
    }
}
