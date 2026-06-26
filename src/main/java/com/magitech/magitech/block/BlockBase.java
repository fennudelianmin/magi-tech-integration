package com.magitech.magitech.block;

import com.magitech.magitech.creativetab.MagiTechTab;
import com.magitech.magitech.tile.IDroppableInventory;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
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

    /**
     * 方块被破坏时调用。
     * 如果该方块对应的 TileEntity 实现了 IDroppableInventory 接口，
     * 则自动将内部所有物品以掉落物形式弹出到世界中，
     * 避免玩家因破坏机器而损失内部物品。
     */
    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) {
            TileEntity tileEntity = world.getTileEntity(pos);
            if (tileEntity instanceof IDroppableInventory) {
                ((IDroppableInventory) tileEntity).dropItems(world, pos);
            }
        }
        super.breakBlock(world, pos, state);
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
