package com.dorapack.dorapack.client.gui;

import com.dorapack.dorapack.capability.DoraCapabilities;
import com.dorapack.dorapack.capability.IDoraPlayerData;
import com.dorapack.dorapack.config.DoraConfig;
import com.dorapack.dorapack.network.PacketHandler;
import com.dorapack.dorapack.network.PacketResearchAction;
import com.dorapack.dorapack.research.ResearchKeys;
import com.dorapack.dorapack.research.ResearchTier;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Research table GUI. Shows the player's current points and buttons to research theory tiers and
 * unlock individual items. The sprout table only offers tier-1 research; the endgame table offers
 * tiers 2 and 3. All actions are validated server-side; buttons here just send request packets.
 */
@SideOnly(Side.CLIENT)
public class GuiResearch extends GuiContainer {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("textures/gui/demo_background.png");
    private static final int BTN_BASIC = 100;
    private static final int BTN_ADVANCED = 101;
    private static final int BTN_SUPER = 102;
    private static final int BTN_ITEM_BASE = 200;

    private final EntityPlayer player;
    private final boolean endgame;
    private final List<ItemUnlockEntry> itemEntries = new ArrayList<>();

    public GuiResearch(EntityPlayer player, boolean endgame) {
        super(new ContainerResearch(player, endgame));
        this.player = player;
        this.endgame = endgame;
        this.xSize = 248;
        this.ySize = 200;
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.clear();
        itemEntries.clear();
        int left = (width - xSize) / 2 + 12;
        int top = (height - ySize) / 2 + 30;
        if (!endgame) {
            addTheoryButtons(left, top);
            addItemButtons(left, top + 48, ResearchTier.TIER_1, DoraConfig.basicTheoryCost);
        } else {
            addEndgameButtons(left, top);
        }
    }

    private void addTheoryButtons(int left, int top) {
        buttonList.add(new GuiButton(BTN_BASIC, left, top, 210, 20, "研究：基础理论 (" + DoraConfig.basicTheoryCost + "点)"));
    }

    private void addEndgameButtons(int left, int top) {
        buttonList.add(new GuiButton(BTN_ADVANCED, left, top, 210, 20, "研究：高级理论 (" + DoraConfig.advancedTheoryCost + "点)"));
        buttonList.add(new GuiButton(BTN_SUPER, left, top + 24, 210, 20, "研究：超级理论 (" + DoraConfig.superTheoryCost + "点)"));
        addItemButtons(left, top + 54, ResearchTier.TIER_2, 0);
        addItemButtons(left, top + 54 + ResearchTier.TIER_2.getItemKeys().length * 22,
                ResearchTier.TIER_3, 0);
    }

    private void addItemButtons(int left, int top, ResearchTier tier, int cost) {
        String[] keys = tier.getItemKeys();
        for (int i = 0; i < keys.length; i++) {
            int id = BTN_ITEM_BASE + itemEntries.size();
            buttonList.add(new GuiButton(id, left, top + i * 22, 210, 20, "解锁：" + keys[i]));
            itemEntries.add(new ItemUnlockEntry(keys[i], tier, cost));
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case BTN_BASIC:
                send(new PacketResearchAction(PacketResearchAction.ACTION_BASIC_THEORY));
                break;
            case BTN_ADVANCED:
                send(new PacketResearchAction(PacketResearchAction.ACTION_ADVANCED_THEORY));
                break;
            case BTN_SUPER:
                send(new PacketResearchAction(PacketResearchAction.ACTION_SUPER_THEORY));
                break;
            default:
                handleItemButton(button.id);
                break;
        }
    }

    private void handleItemButton(int id) {
        int index = id - BTN_ITEM_BASE;
        if (index >= 0 && index < itemEntries.size()) {
            ItemUnlockEntry entry = itemEntries.get(index);
            send(new PacketResearchAction(entry.key, entry.tier, entry.cost));
        }
    }

    private void send(PacketResearchAction packet) {
        PacketHandler.INSTANCE.sendToServer(packet);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = endgame ? "终局研究台" : "萌芽期研究台";
        fontRenderer.drawString(title, 12, 8, 0x404040);
        IDoraPlayerData data = DoraCapabilities.get(player);
        int points = data == null ? 0 : data.getTotalPoints();
        fontRenderer.drawString("研究点：" + points, 12, 20, 0x105010);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        drawDefaultBackground();
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        int left = (width - xSize) / 2;
        int top = (height - ySize) / 2;
        drawTexturedModalRect(left, top, 0, 0, 248, 166);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    /** Immutable descriptor tying a GUI button to an item-unlock request. */
    private static final class ItemUnlockEntry {
        private final String key;
        private final ResearchTier tier;
        private final int cost;

        ItemUnlockEntry(String key, ResearchTier tier, int cost) {
            this.key = key;
            this.tier = tier;
            this.cost = cost;
        }
    }
}
