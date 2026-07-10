package com.dorapack.dorapack.item;

import com.dorapack.dorapack.capability.DoraCapabilities;
import com.dorapack.dorapack.capability.IDoraPlayerData;
import com.dorapack.dorapack.config.DoraConfig;
import com.dorapack.dorapack.item.base.ItemBase;
import com.dorapack.dorapack.util.DoraUtil;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;

/**
 * Momotaro rice ball ("桃太郎饭团"). Right-click an animal to instantly tame it. Limited per animal
 * type per day to avoid trivializing mob farming.
 */
public class ItemTameFood extends ItemBase {

    private static final String USAGE_PREFIX = "tamefood_";

    public ItemTameFood(String name) {
        super(name, 16);
    }

    @Override
    public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player, net.minecraft.entity.EntityLivingBase target, EnumHand hand) {
        if (player.world.isRemote) {
            return false;
        }
        if (!(target instanceof EntityAnimal)) {
            return false;
        }
        IDoraPlayerData data = DoraCapabilities.get(player);
        if (data != null) {
            data.refreshDay(DoraUtil.getWorldDay(player.world));
            String key = USAGE_PREFIX + target.getClass().getSimpleName();
            if (data.getDailyUsage(key) >= DoraConfig.tameFoodDailyLimitPerAnimal) {
                DoraUtil.sendActionBar(player, TextFormatting.RED + "今天这种动物已经喂过啦~");
                return true;
            }
            data.incrementDailyUsage(key, 1);
        }
        tame((EntityAnimal) target, player);
        if (!player.isCreative()) {
            stack.shrink(1);
        }
        return true;
    }

    private void tame(EntityAnimal animal, EntityPlayer player) {
        if (animal instanceof EntityTameable) {
            EntityTameable tameable = (EntityTameable) animal;
            tameable.setTamedBy(player);
            tameable.setHealth(tameable.getMaxHealth());
        } else {
            animal.setInLove(player);
        }
        animal.world.setEntityState(animal, (byte) 7);
    }
}
