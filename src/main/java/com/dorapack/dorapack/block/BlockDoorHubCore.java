package com.dorapack.dorapack.block;

import com.dorapack.dorapack.block.tile.TileAnywhereDoor;
import com.dorapack.dorapack.block.tile.TileDoorHubCore;
import com.dorapack.dorapack.config.DoraConfig;
import com.dorapack.dorapack.init.DoraCreativeTab;
import com.dorapack.dorapack.util.DoraUtil;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Network core block ("网络核心") for the anywhere-door hub. Built into the centre of a 7x7 end-stone
 * base framed by four anywhere doors (N/S/E/W). Right-clicking cycles through the frame doors' bound
 * targets and teleports the owner to the selected one, honouring the daily cross-dimension limit and
 * cost from {@link DoraConfig}.
 *
 * <p>The frame doors are found by checking only the four cardinal neighbours of the core — a bounded
 * lookup, never a global scan — satisfying the design document's performance constraints.</p>
 */
public class BlockDoorHubCore extends BlockContainer {

    private static final String USAGE_KEY = "door_hub_cross_dim";

    public BlockDoorHubCore(String name) {
        super(Material.ROCK);
        setRegistryName(name);
        setTranslationKey(name);
        setCreativeTab(DoraCreativeTab.INSTANCE);
        setHardness(3.0F);
        setResistance(30.0F);
    }

    @Override
    @Nullable
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileDoorHubCore();
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
                                EntityLivingBase placer, ItemStack stack) {
        if (!world.isRemote && placer instanceof EntityPlayer) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileDoorHubCore) {
                ((TileDoorHubCore) te).setOwnerUuid(placer.getUniqueID());
            }
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileDoorHubCore)) {
            return false;
        }
        TileDoorHubCore core = (TileDoorHubCore) te;
        if (!core.isOwner(player.getUniqueID())) {
            DoraUtil.sendMessage(player, TextFormatting.RED, "这不是你的传送网络！");
            return true;
        }
        activateHub(world, pos, player, core);
        return true;
    }

    private void activateHub(World world, BlockPos pos, EntityPlayer player, TileDoorHubCore core) {
        List<TileAnywhereDoor> doors = findFrameDoors(world, pos);
        if (doors.isEmpty()) {
            DoraUtil.sendMessage(player, TextFormatting.YELLOW,
                    "未检测到框架任意门（东南西北各放置一扇并绑定）");
            return;
        }
        int index = core.advanceSelection(doors.size());
        TileAnywhereDoor selected = doors.get(index);
        if (!selected.hasTarget() || selected.getTargetPos() == null) {
            DoraUtil.sendMessage(player, TextFormatting.YELLOW, "选中的门尚未绑定目标");
            return;
        }
        teleport(world, player, selected);
    }

    private void teleport(World world, EntityPlayer player, TileAnywhereDoor door) {
        boolean crossDim = door.getTargetDim() != world.provider.getDimension();
        if (crossDim && !checkCrossDimBudget(player)) {
            return;
        }
        BlockPos target = door.getTargetPos();
        if (player instanceof EntityPlayerMP) {
            player.setPositionAndUpdate(target.getX() + 0.5D, target.getY() + 1.0D, target.getZ() + 0.5D);
            world.playSound(null, target, net.minecraft.init.SoundEvents.ENTITY_ENDERMEN_TELEPORT,
                    net.minecraft.util.SoundCategory.PLAYERS, 1.0F, 1.0F);
            if (crossDim) {
                com.dorapack.dorapack.capability.IDoraPlayerData data =
                        com.dorapack.dorapack.capability.DoraCapabilities.get(player);
                if (data != null) {
                    data.incrementDailyUsage(USAGE_KEY, 1);
                }
            }
        }
    }

    private boolean checkCrossDimBudget(EntityPlayer player) {
        com.dorapack.dorapack.capability.IDoraPlayerData data =
                com.dorapack.dorapack.capability.DoraCapabilities.get(player);
        if (data == null) {
            return false;
        }
        if (data.getDailyUsage(USAGE_KEY) >= DoraConfig.doorHubDailyCrossDimLimit) {
            DoraUtil.sendMessage(player, TextFormatting.RED, "今日跨维度传送次数已用尽");
            return false;
        }
        return true;
    }

    private List<TileAnywhereDoor> findFrameDoors(World world, BlockPos pos) {
        List<TileAnywhereDoor> doors = new ArrayList<>(4);
        for (EnumFacing facing : EnumFacing.HORIZONTALS) {
            TileEntity te = world.getTileEntity(pos.offset(facing));
            if (te instanceof TileAnywhereDoor) {
                doors.add((TileAnywhereDoor) te);
            }
        }
        return doors;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return true;
    }
}
