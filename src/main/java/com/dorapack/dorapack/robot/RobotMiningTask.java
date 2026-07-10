package com.dorapack.dorapack.robot;

import com.dorapack.dorapack.entity.EntityMiningRobot;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Stateless mining logic for {@link EntityMiningRobot}. On each work tick it selects one target block
 * within a small fixed radius around the robot, breaks it, and stores the resulting drops (applying
 * fortune / smelting / whitelist upgrades). Selection is a bounded local scan — never a global one —
 * satisfying the design document's performance constraint.
 */
public final class RobotMiningTask {

    private static final int RADIUS = 3;
    private static final float MAX_HARDNESS = 50.0F;
    /** setBlockState flag: 2 = send to clients without triggering a block update. */
    private static final int BLOCK_UPDATE_FLAG = 2;
    /** Vertical offset used when dropping overflow items so they spawn at the robot's centre. */
    private static final float DROP_OFFSET = 0.5F;
    /** OreDictionary name prefix identifying ore blocks. */
    private static final String ORE_PREFIX = "ore";

    private RobotMiningTask() {
    }

    /**
     * Runs one mining step.
     *
     * @return {@code true} if a block was mined (and energy should be consumed)
     */
    public static boolean tick(EntityMiningRobot robot) {
        World world = robot.world;
        BlockPos origin = robot.getPosition();
        RobotStats stats = robot.getStats();
        BlockPos target = findTarget(world, origin, stats.isOreScanner());
        if (target == null) {
            return false;
        }
        IBlockState state = world.getBlockState(target);
        NonNullList<ItemStack> drops = NonNullList.create();
        state.getBlock().getDrops(drops, world, target, state, stats.getFortuneLevel());
        world.setBlockState(target, Blocks.AIR.getDefaultState(), BLOCK_UPDATE_FLAG);
        for (ItemStack drop : drops) {
            ItemStack processed = stats.isSmelting() ? SmeltingHelper.smelt(world, drop) : drop;
            if (stats.isWhitelist() && !RobotWhitelist.isAllowed(robot, processed)) {
                continue;
            }
            ItemStack leftover = robot.storeItem(processed);
            if (!leftover.isEmpty()) {
                robot.entityDropItem(leftover, DROP_OFFSET);
            }
        }
        return true;
    }

    /**
     * Finds the nearest mineable block within {@link #RADIUS}. When {@code preferOres} is set, ore
     * blocks are returned first; otherwise the first solid, breakable, non-fluid block is chosen.
     */
    private static BlockPos findTarget(World world, BlockPos origin, boolean preferOres) {
        BlockPos firstSolid = null;
        for (int dy = -RADIUS; dy <= RADIUS; dy++) {
            for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    BlockPos pos = origin.add(dx, dy, dz);
                    if (!isMineable(world, pos)) {
                        continue;
                    }
                    if (preferOres && isOre(world, pos)) {
                        return pos;
                    }
                    if (firstSolid == null) {
                        firstSolid = pos;
                    }
                }
            }
        }
        return firstSolid;
    }

    private static boolean isMineable(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock().isAir(state, world, pos)) {
            return false;
        }
        if (state.getMaterial().isLiquid()) {
            return false;
        }
        float hardness = state.getBlockHardness(world, pos);
        return hardness >= 0.0F && hardness <= MAX_HARDNESS;
    }

    private static boolean isOre(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        ItemStack stack = new ItemStack(state.getBlock(), 1, state.getBlock().getMetaFromState(state));
        if (stack.isEmpty()) {
            return false;
        }
        int[] ids = net.minecraftforge.oredict.OreDictionary.getOreIDs(stack);
        for (int id : ids) {
            String name = net.minecraftforge.oredict.OreDictionary.getOreName(id);
            if (name.startsWith(ORE_PREFIX)) {
                return true;
            }
        }
        return false;
    }
}
