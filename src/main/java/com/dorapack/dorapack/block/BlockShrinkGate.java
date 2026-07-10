package com.dorapack.dorapack.block;

import com.dorapack.dorapack.handler.ShrinkHandler;
import com.dorapack.dorapack.init.DoraCreativeTab;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Shrink-tunnel mouth block ("缩小隧道口"). The tunnel is a player-built structure (cobblestone body with
 * a large glass mouth and a small iron-bar mouth); these blocks mark the two mouths. Walking into the
 * large mouth shrinks the player; walking into the small mouth restores them.
 *
 * <p>Detection is done via {@link Block#onEntityWalk} on the mouth blocks themselves — an event-driven
 * trigger rather than the forbidden real-time structure scan.</p>
 */
public class BlockShrinkGate extends Block {

    private final boolean large;

    public BlockShrinkGate(String name, boolean large) {
        super(Material.GLASS);
        this.large = large;
        setRegistryName(name);
        setTranslationKey(name);
        setCreativeTab(DoraCreativeTab.INSTANCE);
        setHardness(1.5F);
        setResistance(10.0F);
    }

    public boolean isLarge() {
        return large;
    }

    @Override
    public void onEntityWalk(World world, BlockPos pos, Entity entity) {
        if (!world.isRemote && entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            if (large) {
                ShrinkHandler.shrink(player);
            } else {
                ShrinkHandler.restore(player);
            }
        }
        super.onEntityWalk(world, pos, entity);
    }
}
