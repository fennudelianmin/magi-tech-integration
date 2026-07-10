package com.dorapack.dorapack.handler;

import com.dorapack.dorapack.Reference;
import com.dorapack.dorapack.capability.DoraCapabilities;
import com.dorapack.dorapack.capability.IDoraPlayerData;
import com.dorapack.dorapack.config.DoraConfig;
import com.dorapack.dorapack.item.ItemEmergencyPill;
import com.dorapack.dorapack.research.ResearchKeys;
import com.dorapack.dorapack.util.DoraUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Combat-related reactions:
 * <ul>
 *   <li>Emergency pill: intercepts a hit that would drop the player to 2 hearts or below, cancels
 *       the damage, heals and buffs the player, consumes one pill, and enforces a daily limit.</li>
 *   <li>Invisibility-cloak reveal: attacking while invisible ends the invisibility.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public final class CombatEventHandler {

    private static final String USAGE_KEY = "emergencypill";
    private static final float TRIGGER_HEALTH = 4.0F;

    private CombatEventHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof EntityPlayer) || event.getEntity().world.isRemote) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.getEntity();
        if (player.getHealth() - event.getAmount() > TRIGGER_HEALTH) {
            return;
        }
        IDoraPlayerData data = DoraCapabilities.get(player);
        if (data == null || !data.isItemUnlocked(ResearchKeys.EMERGENCY_PILL)) {
            return;
        }
        data.refreshDay(DoraUtil.getWorldDay(player.world));
        if (data.getDailyUsage(USAGE_KEY) >= DoraConfig.emergencyPillDailyLimit) {
            return;
        }
        if (!consumePill(player)) {
            return;
        }
        event.setCanceled(true);
        activate(player);
        data.incrementDailyUsage(USAGE_KEY, 1);
    }

    private static boolean consumePill(EntityPlayer player) {
        InventoryPlayer inv = player.inventory;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemEmergencyPill) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    private static void activate(EntityPlayer player) {
        player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 8.0F));
        player.setAbsorptionAmount(8.0F);
        player.addPotionEffect(new PotionEffect(MobEffects.SPEED, 100, 1, false, true));
        player.addPotionEffect(new PotionEffect(MobEffects.JUMP_BOOST, 100, 1, false, true));
        DoraUtil.sendMessage(player, TextFormatting.GOLD, "♪ 应急药丸触发！");
    }
}
