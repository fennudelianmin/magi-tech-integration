package com.dorapack.dorapack.block;

import com.dorapack.dorapack.block.tile.TileChargingStation;
import com.dorapack.dorapack.init.DoraCreativeTab;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * Charging station block ("充电站"). Holds a {@link TileChargingStation} which draws IC2 energy and
 * charges nearby mining robots. Uses {@link BlockContainer} so the standard invisible-render / TE
 * plumbing is handled by the base class.
 */
public class BlockChargingStation extends BlockContainer {

    public BlockChargingStation(String name) {
        super(Material.IRON);
        setRegistryName(name);
        setTranslationKey(name);
        setCreativeTab(DoraCreativeTab.INSTANCE);
        setHardness(3.5F);
        setResistance(20.0F);
    }

    @Override
    @Nullable
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileChargingStation();
    }

    /** Keep the standard model render (BlockContainer defaults to INVISIBLE otherwise). */
    @Override
    public net.minecraft.util.EnumBlockRenderType getRenderType(IBlockState state) {
        return net.minecraft.util.EnumBlockRenderType.MODEL;
    }
}
