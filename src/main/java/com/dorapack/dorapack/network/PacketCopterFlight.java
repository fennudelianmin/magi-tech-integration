package com.dorapack.dorapack.network;

import com.dorapack.dorapack.handler.FlightHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Client -> server request to start a bamboo-copter flight, sent when the player double-taps jump
 * while wearing the copter. The server validates equipment/weather and starts the timed flight.
 */
public class PacketCopterFlight implements IMessage {

    @Override
    public void fromBytes(ByteBuf buf) {
        // No payload: the action is implicit.
    }

    @Override
    public void toBytes(ByteBuf buf) {
        // No payload.
    }

    /**
     * Server-side handler that begins the flight.
     */
    public static class Handler implements IMessageHandler<PacketCopterFlight, IMessage> {

        @Override
        public IMessage onMessage(PacketCopterFlight message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> FlightHandler.startFlight(player));
            return null;
        }
    }
}
