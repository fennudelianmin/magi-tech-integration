package com.magitech.magitech.tile;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 实现该接口的 TileEntity 在被破坏时需要将内部物品掉落至世界。
 * BlockBase.breakBlock() 会检测 TileEntity 是否实现了此接口，
 * 并在方块破坏时自动调用 dropItems() 方法。
 */
public interface IDroppableInventory {

    /**
     * 将 TileEntity 内部的所有物品以 EntityItem 的形式掉落至世界中。
     * 应当在方块被破坏（breakBlock）时调用。
     *
     * @param world 世界实例
     * @param pos   方块位置
     */
    void dropItems(World world, BlockPos pos);
}
