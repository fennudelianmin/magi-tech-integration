package com.magitech.magitech.block;

import com.magitech.magitech.tile.TileTerraPlateAssembler;
import net.minecraft.block.material.Material;

public class BlockTerraPlateAssembler extends BlockBase {
    public BlockTerraPlateAssembler() {
        super(Material.ROCK, "terra_plate_assembler", TileTerraPlateAssembler::new);
    }
}
