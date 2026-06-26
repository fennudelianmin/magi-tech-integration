package com.magitech.magitech.block;

import com.magitech.magitech.tile.TileFlowerCraftingTable;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

import javax.annotation.Nullable;

public class BlockFlowerCraftingTable extends BlockBase {

    public BlockFlowerCraftingTable() {
        super(Material.ROCK, "flower_crafting_table", TileFlowerCraftingTable::new);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack heldItem = player.getHeldItem(hand);
        if (heldItem.isEmpty()) {
            // 空手可以留作以后打开 GUI，这里直接忽略
            return false;
        }

        // 获取内部水槽的 IFluidHandler
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileFlowerCraftingTable)) {
            return false;
        }
        IFluidHandler tankHandler = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, facing);
        if (tankHandler == null) {
            return false;
        }

        // 尝试使用 Forge 的 FluidUtil 进行流体交互（支持水桶及任何流体容器）
        FluidActionResult result = FluidUtil.tryEmptyContainer(heldItem, tankHandler, 1000, player, true);
        if (result.isSuccess()) {
            // 成功注水：替换手中物品（例如水桶变空桶），播放声音，标记方块更新
            player.setHeldItem(hand, result.getResult());
            world.notifyBlockUpdate(pos, state, state, 3);
            te.markDirty();

            // 播放倒水声（可选）
            SoundEvent sound = net.minecraft.init.SoundEvents.ITEM_BUCKET_EMPTY;
            world.playSound(null, pos, sound, SoundCategory.BLOCKS, 1.0F, 1.0F);

            return true; // 交互已处理，阻止水桶的默认行为
        }

        // 如果玩家手持的是空容器（如空桶），尝试从水槽中取出水
        FluidActionResult fillResult = FluidUtil.tryFillContainer(heldItem, tankHandler, 1000, player, true);
        if (fillResult.isSuccess()) {
            player.setHeldItem(hand, fillResult.getResult());
            world.notifyBlockUpdate(pos, state, state, 3);
            te.markDirty();

            SoundEvent sound = net.minecraft.init.SoundEvents.ITEM_BUCKET_FILL;
            world.playSound(null, pos, sound, SoundCategory.BLOCKS, 1.0F, 1.0F);

            return true;
        }

        // 其他物品（非流体容器）默认不处理
        return false;
    }
}