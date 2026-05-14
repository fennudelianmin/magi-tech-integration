package com.magitech.magitech.block;

import com.magitech.magitech.tile.TileElvenTradeMachine;
import net.minecraft.block.material.Material;

public class BlockElvenTradeMachine extends BlockBase {
    public BlockElvenTradeMachine() {
        super(Material.ROCK, "elven_trade_machine", TileElvenTradeMachine::new);
    }
}
