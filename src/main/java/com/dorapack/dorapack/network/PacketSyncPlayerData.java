package com.dorapack.dorapack.network;

import com.dorapack.dorapack.capability.DoraCapabilities;
import com.dorapack.dorapack.capability.IDoraPlayerData;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Server -> client sync of the essential player research snapshot (points and theory tiers)
 * used by the research GUI. Kept intentionally small; the client never mutates this data.
 */
public class PacketSyncPlayerData implements IMessage {

    private int totalPoints;
    private int pointsToday;
    private boolean basic;
    private boolean advanced;
    private boolean superTheory;

    public PacketSyncPlayerData() {
    }

    public PacketSyncPlayerData(IDoraPlayerData data) {
        this.totalPoints = data.getTotalPoints();
        this.pointsToday = data.getPointsGainedToday();
        this.basic = data.hasBasicTheory();
        this.advanced = data.hasAdvancedTheory();
        this.superTheory = data.hasSuperTheory();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.totalPoints = buf.readInt();
        this.pointsToday = buf.readInt();
        this.basic = buf.readBoolean();
        this.advanced = buf.readBoolean();
        this.superTheory = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(totalPoints);
        buf.writeInt(pointsToday);
        buf.writeBoolean(basic);
        buf.writeBoolean(advanced);
        buf.writeBoolean(superTheory);
    }

    /**
     * Client-side handler applying the synced snapshot to the local player capability.
     */
    public static class Handler implements IMessageHandler<PacketSyncPlayerData, IMessage> {

        @Override
        public IMessage onMessage(PacketSyncPlayerData message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT) {
                ClientApplier.schedule(message);
            }
            return null;
        }
    }

    /**
     * Isolates all {@code net.minecraft.client} references so the enclosing packet class stays
     * safe to classload on a dedicated server.
     */
    @SideOnly(Side.CLIENT)
    private static final class ClientApplier {

        private ClientApplier() {
        }

        static void schedule(PacketSyncPlayerData message) {
            Minecraft.getMinecraft().addScheduledTask(() -> apply(message));
        }

        static void apply(PacketSyncPlayerData message) {
            EntityPlayer player = Minecraft.getMinecraft().player;
            if (player == null) {
                return;
            }
            IDoraPlayerData data = DoraCapabilities.get(player);
            if (data == null) {
                return;
            }
            data.setTotalPoints(message.totalPoints);
            data.setPointsGainedToday(message.pointsToday);
            data.setBasicTheory(message.basic);
            data.setAdvancedTheory(message.advanced);
            data.setSuperTheory(message.superTheory);
        }
    }
}
