package com.magitech.magitech.block;

import com.magitech.magitech.creativetab.MagiTechTab;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class BlockBase extends Block {

    private final Supplier<? extends TileEntity> tileEntitySupplier;

    public BlockBase(Material material, String name, Supplier<? extends TileEntity> tileEntitySupplier) {
        super(material);
        this.tileEntitySupplier = tileEntitySupplier;
        setRegistryName("magitech", name);
        setTranslationKey("magitech." + name);
        setCreativeTab(MagiTechTab.INSTANCE);
        setHardness(3.0F);
        setResistance(10.0F);
        setHarvestLevel("pickaxe", 1);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return tileEntitySupplier != null;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        if (tileEntitySupplier != null) {
            return tileEntitySupplier.get();
        }
        return null;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }
}
