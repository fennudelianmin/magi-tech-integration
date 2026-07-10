package com.dorapack.dorapack.client;

import com.dorapack.dorapack.item.ItemBambooCopter;
import com.dorapack.dorapack.network.PacketCopterFlight;
import com.dorapack.dorapack.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Client-side detector for the bamboo-copter double-tap-jump gesture. When the player double-taps the
 * jump key within {@link #DOUBLE_TAP_WINDOW_TICKS} while wearing a copter, a {@link PacketCopterFlight}
 * is sent to the server to begin the timed flight.
 *
 * <p>Registered on the client only. Uses edge detection on the vanilla jump keybind rather than a
 * custom keybind so the gesture matches the whitepaper (double jump), and throttles resends so flight
 * cannot be spammed while already airborne.</p>
 */
@SideOnly(Side.CLIENT)
public final class ClientInputHandler {

    private static final int DOUBLE_TAP_WINDOW_TICKS = 8;
    private static final int RESEND_COOLDOWN_TICKS = 20;

    private boolean jumpWasDown;
    private int ticksSinceFirstTap = -1;
    private int resendCooldown;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (player == null || mc.currentScreen != null) {
            reset();
            return;
        }
        if (resendCooldown > 0) {
            resendCooldown--;
        }
        if (ticksSinceFirstTap >= 0) {
            ticksSinceFirstTap++;
            if (ticksSinceFirstTap > DOUBLE_TAP_WINDOW_TICKS) {
                ticksSinceFirstTap = -1;
            }
        }

        boolean jumpDown = mc.gameSettings.keyBindJump.isKeyDown();
        boolean pressedThisTick = jumpDown && !jumpWasDown;
        jumpWasDown = jumpDown;

        if (!pressedThisTick) {
            return;
        }
        if (ticksSinceFirstTap >= 0) {
            // Second tap within the window: fire flight request.
            ticksSinceFirstTap = -1;
            tryStartFlight(player);
        } else {
            ticksSinceFirstTap = 0;
        }
    }

    private void tryStartFlight(EntityPlayerSP player) {
        if (resendCooldown > 0 || !ItemBambooCopter.isEquipped(player)) {
            return;
        }
        resendCooldown = RESEND_COOLDOWN_TICKS;
        PacketHandler.INSTANCE.sendToServer(new PacketCopterFlight());
    }

    private void reset() {
        jumpWasDown = false;
        ticksSinceFirstTap = -1;
    }
}
