package com.dorapack.dorapack.block;

import com.dorapack.dorapack.DoraPackMod;
import com.dorapack.dorapack.client.gui.DoraGuiHandler;
import com.dorapack.dorapack.init.DoraCreativeTab;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Research table block ("研究台"). Two variants exist (sprout / endgame) differentiated by the
 * {@code endgame} flag, which controls the GUI id opened and therefore which research tiers are
 * available. Right-click opens the research GUI.
 */
public class BlockResearchTable extends Block {

    private final boolean endgame;

    public BlockResearchTable(String name, boolean endgame) {
        super(Material.ROCK);
        this.endgame = endgame;
        setRegistryName(name);
        setTranslationKey(name);
        setCreativeTab(DoraCreativeTab.INSTANCE);
        setHardness(2.5F);
        setResistance(10.0F);
    }

    public boolean isEndgame() {
        return endgame;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            int guiId = endgame ? DoraGuiHandler.GUI_RESEARCH_ENDGAME : DoraGuiHandler.GUI_RESEARCH_SPROUT;
            player.openGui(DoraPackMod.instance, guiId, world, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }
}
