package com.dorapack.dorapack.client;

import com.dorapack.dorapack.Reference;
import com.dorapack.dorapack.entity.EntityFairy;
import com.dorapack.dorapack.entity.EntityMiningRobot;
import com.dorapack.dorapack.init.DoraBlocks;
import com.dorapack.dorapack.init.DoraItems;
import com.dorapack.dorapack.robot.RobotUpgrade;
import net.minecraft.block.Block;
import net.minecraft.client.model.ModelVillager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderSnowball;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Client-only registrar for item/block models and entity renderers. Models point at the JSON assets
 * shipped under {@code assets/dorapack/models}, which reuse vanilla textures so no bespoke art is
 * required. Entity renderers reuse vanilla model/render classes as placeholders per the project brief.
 */
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = Reference.MOD_ID)
@SideOnly(Side.CLIENT)
public final class ClientRegistryHandler {

    private ClientRegistryHandler() {
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        registerSimpleItems();
        registerUpgradeModels();
        registerBlockItems();
    }

    private static void registerSimpleItems() {
        registerItemModel(DoraItems.BAMBOO_COPTER);
        registerItemModel(DoraItems.MINI_AIR_CANNON);
        registerItemModel(DoraItems.BAG_INITIAL);
        registerItemModel(DoraItems.TAME_FOOD);
        registerItemModel(DoraItems.HEADLAMP);
        registerItemModel(DoraItems.AIR_CANNON);
        registerItemModel(DoraItems.TIME_WRAP);
        registerItemModel(DoraItems.TRANSLATE_JELLY);
        registerItemModel(DoraItems.MEMORY_BREAD);
        registerItemModel(DoraItems.EMERGENCY_PILL);
        registerItemModel(DoraItems.EXP_BOTTLE);
        registerItemModel(DoraItems.MOMOTARO_FLUTE);
        registerItemModel(DoraItems.MINER_DRILL);
        registerItemModel(DoraItems.INVIS_CLOAK);
        registerItemModel(DoraItems.BAG_UPGRADE);
        registerItemModel(DoraItems.GIANT_AIR_CANNON);
        registerItemModel(DoraItems.ROCKET_BOOSTER);
        registerItemModel(DoraItems.WEATHER_CONTROLLER);
        registerItemModel(DoraItems.TIME_STOP_WATCH);
        registerItemModel(DoraItems.MINING_ROBOT);
        registerItemModel(DoraItems.BECOME_DORAEMON);
        registerItemModel(DoraItems.WISDOM_KING);
        registerItemModel(DoraItems.DIMENSION_SHARD);
        registerItemModel(DoraItems.DIMENSION_CORE);
    }

    private static void registerUpgradeModels() {
        RobotUpgrade[] values = RobotUpgrade.values();
        for (int i = 0; i < values.length; i++) {
            String path = DoraItems.NAME_ROBOT_UPGRADE + "_" + values[i].getId();
            ModelLoader.setCustomModelResourceLocation(DoraItems.ROBOT_UPGRADE, i,
                    new ModelResourceLocation(new ResourceLocation(Reference.MOD_ID, path), "inventory"));
        }
    }

    private static void registerBlockItems() {
        registerBlockItemModel(DoraBlocks.ANYWHERE_DOOR);
        registerBlockItemModel(DoraBlocks.RESEARCH_TABLE_SPROUT);
        registerBlockItemModel(DoraBlocks.RESEARCH_TABLE_ENDGAME);
        registerBlockItemModel(DoraBlocks.CHARGING_STATION);
        registerBlockItemModel(DoraBlocks.SHRINK_GATE_LARGE);
        registerBlockItemModel(DoraBlocks.SHRINK_GATE_SMALL);
        registerBlockItemModel(DoraBlocks.DOOR_HUB_CORE);
    }

    private static void registerItemModel(Item item) {
        ModelLoader.setCustomModelResourceLocation(item, 0,
                new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }

    private static void registerBlockItemModel(Block block) {
        Item item = Item.getItemFromBlock(block);
        ModelLoader.setCustomModelResourceLocation(item, 0,
                new ModelResourceLocation(block.getRegistryName(), "inventory"));
    }

    /** Registers entity renderers; called from the client proxy during init. */
    public static void registerEntityRenderers() {
        RenderingRegistry.registerEntityRenderingHandler(EntityFairy.class,
                manager -> new RenderSnowball<>(manager, Items.SNOWBALL,
                        net.minecraft.client.Minecraft.getMinecraft().getRenderItem()));
        RenderingRegistry.registerEntityRenderingHandler(EntityMiningRobot.class, RobotRender::new);
    }

    /** Placeholder robot renderer reusing the vanilla villager model. */
    @SideOnly(Side.CLIENT)
    private static class RobotRender extends RenderLiving<EntityMiningRobot> {
        RobotRender(RenderManager manager) {
            super(manager, new ModelVillager(0.0F), 0.5F);
        }

        @Override
        protected ResourceLocation getEntityTexture(EntityMiningRobot entity) {
            return new ResourceLocation("textures/entity/iron_golem/iron_golem.png");
        }
    }
}
