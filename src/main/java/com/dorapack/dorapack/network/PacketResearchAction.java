package com.dorapack.dorapack.network;

import com.dorapack.dorapack.capability.DoraCapabilities;
import com.dorapack.dorapack.capability.IDoraPlayerData;
import com.dorapack.dorapack.research.ResearchManager;
import com.dorapack.dorapack.research.ResearchTier;
import com.dorapack.dorapack.util.DoraUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Client -> server request to perform a research action (research a theory tier, or unlock a
 * specific item). The server validates all prerequisites via {@link ResearchManager}; the client
 * never mutates research state directly.
 */
public class PacketResearchAction implements IMessage {

    /** Action codes. */
    public static final int ACTION_BASIC_THEORY = 0;
    public static final int ACTION_ADVANCED_THEORY = 1;
    public static final int ACTION_SUPER_THEORY = 2;
    public static final int ACTION_UNLOCK_ITEM = 3;

    /** Upper bound on the item-key string length accepted from the wire, to reject malformed packets. */
    private static final int MAX_ITEM_KEY_LENGTH = 256;

    private int action;
    private String itemKey = "";
    private int tierOrdinal;
    private int cost;

    public PacketResearchAction() {
    }

    public PacketResearchAction(int action) {
        this.action = action;
    }

    public PacketResearchAction(String itemKey, ResearchTier tier, int cost) {
        this.action = ACTION_UNLOCK_ITEM;
        this.itemKey = itemKey;
        this.tierOrdinal = tier.ordinal();
        this.cost = cost;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.action = buf.readInt();
        this.tierOrdinal = buf.readInt();
        this.cost = buf.readInt();
        int len = buf.readInt();
        // Reject corrupt or malicious lengths: a char is 2 bytes, so len*2 must fit the readable window.
        if (len < 0 || len > MAX_ITEM_KEY_LENGTH || (long) len * 2L > buf.readableBytes()) {
            this.itemKey = "";
            return;
        }
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(buf.readChar());
        }
        this.itemKey = sb.toString();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(action);
        buf.writeInt(tierOrdinal);
        buf.writeInt(cost);
        buf.writeInt(itemKey.length());
        for (int i = 0; i < itemKey.length(); i++) {
            buf.writeChar(itemKey.charAt(i));
        }
    }

    /**
     * Server-side handler applying the validated research action.
     */
    public static class Handler implements IMessageHandler<PacketResearchAction, IMessage> {

        @Override
        public IMessage onMessage(PacketResearchAction message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> apply(message, player));
            return null;
        }

        private void apply(PacketResearchAction message, EntityPlayerMP player) {
            boolean success;
            switch (message.action) {
                case ACTION_BASIC_THEORY:
                    success = ResearchManager.researchBasicTheory(player);
                    break;
                case ACTION_ADVANCED_THEORY:
                    success = ResearchManager.researchAdvancedTheory(player);
                    break;
                case ACTION_SUPER_THEORY:
                    success = ResearchManager.researchSuperTheory(player);
                    break;
                case ACTION_UNLOCK_ITEM:
                    ResearchTier tier = ResearchTier.values()[Math.floorMod(message.tierOrdinal, ResearchTier.values().length)];
                    success = ResearchManager.unlockItem(player, message.itemKey, tier, message.cost);
                    break;
                default:
                    success = false;
                    break;
            }
            if (success) {
                DoraUtil.sendMessage(player, TextFormatting.GREEN, "研究成功！");
                syncToClient(player);
            } else {
                DoraUtil.sendMessage(player, TextFormatting.RED, "研究失败：条件不足");
            }
        }

        private void syncToClient(EntityPlayerMP player) {
            IDoraPlayerData data = DoraCapabilities.get(player);
            if (data != null) {
                PacketHandler.INSTANCE.sendTo(new PacketSyncPlayerData(data), player);
            }
        }
    }
}
