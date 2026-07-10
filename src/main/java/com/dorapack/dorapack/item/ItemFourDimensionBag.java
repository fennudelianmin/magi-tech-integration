package com.dorapack.dorapack.item;

import com.dorapack.dorapack.DoraPackMod;
import com.dorapack.dorapack.client.gui.DoraGuiHandler;
import com.dorapack.dorapack.item.base.ItemBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

/**
 * The four-dimensional bag ("百宝袋"). Right-click opens a shared, per-player inventory backed by
 * {@link com.dorapack.dorapack.world.PlayerBagData} — every bag a player holds shows the same
 * contents, and those contents are never dropped on death.
 */
public class ItemFourDimensionBag extends ItemBase {

    public ItemFourDimensionBag(String name) {
        super(name, 1);
    }

    @Override
    @Nonnull
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, @Nonnull EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);
        if (!world.isRemote) {
            player.openGui(DoraPackMod.instance, DoraGuiHandler.GUI_BAG, world,
                    (int) player.posX, (int) player.posY, (int) player.posZ);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, held);
    }
}
