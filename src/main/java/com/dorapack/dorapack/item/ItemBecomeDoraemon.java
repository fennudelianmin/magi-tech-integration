package com.dorapack.dorapack.item;

import com.dorapack.dorapack.DoraPackMod;
import com.dorapack.dorapack.item.base.ItemBase;
import com.dorapack.dorapack.util.DoraUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.GameType;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

/**
 * Creative-only easter egg "我要成为哆啦A梦了". Right-click toggles the user between survival/adventure
 * and creative mode. Server-authoritative and OP-gated for safety; every use is logged.
 */
public class ItemBecomeDoraemon extends ItemBase {

    public ItemBecomeDoraemon(String name) {
        super(name, 1);
    }

    @Override
    @Nonnull
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, @Nonnull EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (world.isRemote || !(player instanceof EntityPlayerMP)) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        EntityPlayerMP mp = (EntityPlayerMP) player;
        if (!isOp(mp)) {
            DoraUtil.sendMessage(player, TextFormatting.RED, "需要 OP 权限才能使用");
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        GameType current = mp.interactionManager.getGameType();
        GameType target = current == GameType.CREATIVE ? GameType.SURVIVAL : GameType.CREATIVE;
        mp.setGameType(target);
        DoraPackMod.getLogger().info("[Ultimate] {} switched game mode {} -> {}",
                player.getName(), current, target);
        DoraUtil.sendMessage(player, TextFormatting.GOLD, "叮~ 你现在是 " + target + " 模式");
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    private boolean isOp(EntityPlayerMP player) {
        return player.getServer() != null
                && player.getServer().getPlayerList().getOppedPlayers().getEntry(player.getGameProfile()) != null;
    }
}
