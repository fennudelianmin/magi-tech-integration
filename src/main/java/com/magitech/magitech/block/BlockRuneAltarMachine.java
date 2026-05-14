package com.magitech.magitech.block;

import com.magitech.magitech.tile.TileRuneAltarMachine;
import net.minecraft.block.material.Material;

public class BlockRuneAltarMachine extends BlockBase {
    public BlockRuneAltarMachine() {
        super(Material.ROCK, "rune_altar_machine", TileRuneAltarMachine::new);
    }
}
