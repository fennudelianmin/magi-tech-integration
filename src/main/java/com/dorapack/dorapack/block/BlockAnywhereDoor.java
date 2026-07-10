package com.dorapack.dorapack.block;

import com.dorapack.dorapack.achievement.AchievementManager;
import com.dorapack.dorapack.block.tile.TileAnywhereDoor;
import com.dorapack.dorapack.config.DoraConfig;
import com.dorapack.dorapack.init.DoraCreativeTab;
import com.dorapack.dorapack.util.DoraUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Basic anywhere door block. Behaviour:
 * <ul>
 *   <li>On placement, records the placing player as owner.</li>
 *   <li>Shift-right-click selects this door as a binding source; shift-right-click another door
 *       binds the two together (both directions).</li>
 *   <li>Right-click teleports the owner to the bound door, subject to cooldown and max distance.</li>
 *   <li>Only the owner may bind or use the door (anti-theft).</li>
 * </ul>
 */
public class BlockAnywhereDoor extends Block {

    /** Transient per-player pending bind source (server-side only, cleared after binding). */
    private static final java.util.Map<UUID, BlockPos> PENDING_BIND = new java.util.HashMap<>();

    public BlockAnywhereDoor(String name) {
        super(Material.IRON);
        setRegistryName(name);
        setTranslationKey(name);
        setCreativeTab(DoraCreativeTab.INSTANCE);
        setHardness(3.0F);
        setResistance(15.0F);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    @Nullable
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileAnywhereDoor();
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
                                net.minecraft.entity.EntityLivingBase placer, ItemStack stack) {
        if (!world.isRemote && placer instanceof EntityPlayer) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileAnywhereDoor) {
                ((TileAnywhereDoor) te).setOwnerUuid(placer.getUniqueID());
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
        if (!(te instanceof TileAnywhereDoor)) {
            return false;
        }
        TileAnywhereDoor door = (TileAnywhereDoor) te;
        if (!door.isOwner(player.getUniqueID())) {
            DoraUtil.sendMessage(player, TextFormatting.RED, "这不是你的门！");
            return true;
        }
        if (player.isSneaking()) {
            handleBinding(world, pos, player, door);
        } else {
            handleTeleport(world, player, door);
        }
        return true;
    }

    private void handleBinding(World world, BlockPos pos, EntityPlayer player, TileAnywhereDoor door) {
        UUID uuid = player.getUniqueID();
        BlockPos pending = PENDING_BIND.get(uuid);
        if (pending == null || pending.equals(pos)) {
            PENDING_BIND.put(uuid, pos);
            DoraUtil.sendMessage(player, TextFormatting.AQUA, "已选择这扇门，右键另一扇门完成绑定");
            return;
        }
        TileEntity otherTe = world.getTileEntity(pending);
        if (!(otherTe instanceof TileAnywhereDoor)) {
            PENDING_BIND.remove(uuid);
            DoraUtil.sendMessage(player, TextFormatting.RED, "之前选择的门已不存在");
            return;
        }
        int dim = world.provider.getDimension();
        door.bindTo(pending, dim);
        ((TileAnywhereDoor) otherTe).bindTo(pos, dim);
        PENDING_BIND.remove(uuid);
        DoraUtil.sendMessage(player, TextFormatting.GREEN, "两扇门已绑定！");
    }

    private void handleTeleport(World world, EntityPlayer player, TileAnywhereDoor door) {
        if (!door.hasTarget() || door.getTargetPos() == null) {
            DoraUtil.sendMessage(player, TextFormatting.YELLOW, "这扇门还没有绑定目标（潜行右键绑定）");
            return;
        }
        long cooldownGate = door.getCooldownReady(player.getUniqueID());
        if (world.getTotalWorldTime() < cooldownGate) {
            long remaining = (cooldownGate - world.getTotalWorldTime()) / 20L;
            DoraUtil.sendActionBar(player, TextFormatting.YELLOW + "任意门冷却中：" + remaining + " 秒");
            return;
        }
        BlockPos target = door.getTargetPos();
        double distance = Math.sqrt(player.getDistanceSq(target));
        if (distance > DoraConfig.anywhereDoorMaxDistance) {
            DoraUtil.sendMessage(player, TextFormatting.RED, "目标太远了（上限 "
                    + DoraConfig.anywhereDoorMaxDistance + " 格）");
            return;
        }
        if (player instanceof EntityPlayerMP) {
            player.setPositionAndUpdate(target.getX() + 0.5D, target.getY() + 1.0D, target.getZ() + 0.5D);
            world.playSound(null, target, net.minecraft.init.SoundEvents.ENTITY_ENDERMEN_TELEPORT,
                    net.minecraft.util.SoundCategory.PLAYERS, 1.0F, 1.0F);
            door.setCooldownReady(player.getUniqueID(),
                    world.getTotalWorldTime() + DoraConfig.anywhereDoorCooldownSeconds * 20L);
            AchievementManager.onDoorTeleport(player);
        }
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        // Remove any binding referencing this door on the paired side handled lazily on use.
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
}
