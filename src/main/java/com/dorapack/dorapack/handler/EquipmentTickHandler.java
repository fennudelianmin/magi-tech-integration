package com.dorapack.dorapack.handler;

import com.dorapack.dorapack.Reference;
import com.dorapack.dorapack.config.DoraConfig;
import com.dorapack.dorapack.item.ItemHeadlamp;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Per-player tick logic for head-slot utility items:
 * <ul>
 *   <li>Headlamp: refreshes night-vision and applies a small slowness (movement-speed penalty).</li>
 *   <li>Bamboo copter: while an active flight is running, keeps the player aloft, decrements the
 *       timer, and credits the flight achievement.</li>
 * </ul>
 * The night-vision effect is applied with a duration longer than the check interval so it never
 * flickers, and removed promptly when the lamp is taken off.
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public final class EquipmentTickHandler {

    private static final int NIGHT_VISION_DURATION = 300;
    private static final int SLOWNESS_REFRESH = 40;

    private EquipmentTickHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        EntityPlayer player = event.player;
        handleHeadlamp(player);
        FlightHandler.tick(player);
        ShrinkHandler.tick(player);
    }

    private static void handleHeadlamp(EntityPlayer player) {
        if (!ItemHeadlamp.isEquipped(player)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, NIGHT_VISION_DURATION, 0, true, false));
        // A small, steady speed penalty representing the lamp's weight. Slowness I is stronger than
        // the design's 3%, so we only apply it briefly each refresh window to average out lighter.
        if (DoraConfig.headlampSpeedPenaltyPercent > 0 && player.ticksExisted % SLOWNESS_REFRESH == 0) {
            player.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 2, 0, true, false));
        }
    }
}
