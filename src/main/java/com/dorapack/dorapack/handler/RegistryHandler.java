package com.dorapack.dorapack.handler;

import com.dorapack.dorapack.Reference;
import com.dorapack.dorapack.config.DoraConfig;
import com.dorapack.dorapack.entity.EntityFairy;
import com.dorapack.dorapack.entity.EntityMiningRobot;
import com.dorapack.dorapack.init.DoraBlocks;
import com.dorapack.dorapack.init.DoraItems;
import com.dorapack.dorapack.item.ItemAirCannon;
import com.dorapack.dorapack.item.ItemBagUpgrade;
import com.dorapack.dorapack.item.ItemBambooCopter;
import com.dorapack.dorapack.item.ItemBecomeDoraemon;
import com.dorapack.dorapack.item.ItemEmergencyPill;
import com.dorapack.dorapack.item.ItemExpBottle;
import com.dorapack.dorapack.item.ItemFourDimensionBag;
import com.dorapack.dorapack.item.ItemGiantAirCannon;
import com.dorapack.dorapack.item.ItemHeadlamp;
import com.dorapack.dorapack.item.ItemInvisCloak;
import com.dorapack.dorapack.item.ItemMemoryBread;
import com.dorapack.dorapack.item.ItemMinerDrill;
import com.dorapack.dorapack.item.ItemMiningRobot;
import com.dorapack.dorapack.item.ItemMomotaroFlute;
import com.dorapack.dorapack.item.ItemRobotUpgrade;
import com.dorapack.dorapack.item.ItemRocketBooster;
import com.dorapack.dorapack.item.ItemTameFood;
import com.dorapack.dorapack.item.ItemTimeStopWatch;
import com.dorapack.dorapack.item.ItemTimeWrap;
import com.dorapack.dorapack.item.ItemTranslateJelly;
import com.dorapack.dorapack.item.ItemWeatherController;
import com.dorapack.dorapack.item.ItemWisdomKing;
import com.dorapack.dorapack.item.base.ItemBase;
import net.minecraft.entity.EntityLiving;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraft.util.ResourceLocation;

/**
 * Central registrar for items, blocks and entities, listening on the mod-specific event bus.
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public final class RegistryHandler {

    private static int entityId;

    private RegistryHandler() {
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        // No-unlock items
        DoraItems.BAMBOO_COPTER = new ItemBambooCopter(DoraItems.NAME_BAMBOO_COPTER);
        DoraItems.MINI_AIR_CANNON = new ItemAirCannon(DoraItems.NAME_MINI_AIR_CANNON,
                DoraConfig.miniAirCannonDurability, 3.0D, 0.5F, DoraConfig.miniAirCannonCooldownSeconds);
        DoraItems.BAG_INITIAL = new ItemFourDimensionBag(DoraItems.NAME_BAG_INITIAL);
        DoraItems.TAME_FOOD = new ItemTameFood(DoraItems.NAME_TAME_FOOD);
        DoraItems.HEADLAMP = new ItemHeadlamp(DoraItems.NAME_HEADLAMP);

        // Tier 1
        DoraItems.AIR_CANNON = new ItemAirCannon(DoraItems.NAME_AIR_CANNON,
                DoraConfig.airCannonDurability, 5.0D, 4.0F, DoraConfig.airCannonCooldownSeconds);
        DoraItems.TIME_WRAP = new ItemTimeWrap(DoraItems.NAME_TIME_WRAP);
        DoraItems.TRANSLATE_JELLY = new ItemTranslateJelly(DoraItems.NAME_TRANSLATE_JELLY);
        DoraItems.MEMORY_BREAD = new ItemMemoryBread(DoraItems.NAME_MEMORY_BREAD);
        DoraItems.EMERGENCY_PILL = new ItemEmergencyPill(DoraItems.NAME_EMERGENCY_PILL);
        DoraItems.EXP_BOTTLE = new ItemExpBottle(DoraItems.NAME_EXP_BOTTLE);
        DoraItems.MOMOTARO_FLUTE = new ItemMomotaroFlute(DoraItems.NAME_MOMOTARO_FLUTE);
        DoraItems.MINER_DRILL = new ItemMinerDrill(DoraItems.NAME_MINER_DRILL);
        DoraItems.INVIS_CLOAK = new ItemInvisCloak(DoraItems.NAME_INVIS_CLOAK);
        DoraItems.BAG_UPGRADE = new ItemBagUpgrade(DoraItems.NAME_BAG_UPGRADE);

        // Tier 2
        DoraItems.GIANT_AIR_CANNON = new ItemGiantAirCannon(DoraItems.NAME_GIANT_AIR_CANNON);
        DoraItems.ROCKET_BOOSTER = new ItemRocketBooster(DoraItems.NAME_ROCKET_BOOSTER);
        DoraItems.WEATHER_CONTROLLER = new ItemWeatherController(DoraItems.NAME_WEATHER_CONTROLLER);
        DoraItems.TIME_STOP_WATCH = new ItemTimeStopWatch(DoraItems.NAME_TIME_STOP_WATCH);

        // Tier 3
        DoraItems.MINING_ROBOT = new ItemMiningRobot(DoraItems.NAME_MINING_ROBOT,
                com.dorapack.dorapack.research.ResearchKeys.MINING_ROBOT);
        DoraItems.ROBOT_UPGRADE = new ItemRobotUpgrade(DoraItems.NAME_ROBOT_UPGRADE);

        // Creative ultimate
        DoraItems.BECOME_DORAEMON = new ItemBecomeDoraemon(DoraItems.NAME_BECOME_DORAEMON);
        DoraItems.WISDOM_KING = new ItemWisdomKing(DoraItems.NAME_WISDOM_KING);

        // Materials
        DoraItems.DIMENSION_SHARD = new ItemBase(DoraItems.NAME_DIMENSION_SHARD);
        DoraItems.DIMENSION_CORE = new ItemBase(DoraItems.NAME_DIMENSION_CORE, 1);

        event.getRegistry().registerAll(
                DoraItems.BAMBOO_COPTER, DoraItems.MINI_AIR_CANNON, DoraItems.BAG_INITIAL,
                DoraItems.TAME_FOOD, DoraItems.HEADLAMP,
                DoraItems.AIR_CANNON, DoraItems.TIME_WRAP, DoraItems.TRANSLATE_JELLY,
                DoraItems.MEMORY_BREAD, DoraItems.EMERGENCY_PILL, DoraItems.EXP_BOTTLE,
                DoraItems.MOMOTARO_FLUTE, DoraItems.MINER_DRILL, DoraItems.INVIS_CLOAK,
                DoraItems.BAG_UPGRADE,
                DoraItems.GIANT_AIR_CANNON, DoraItems.ROCKET_BOOSTER,
                DoraItems.WEATHER_CONTROLLER, DoraItems.TIME_STOP_WATCH,
                DoraItems.MINING_ROBOT, DoraItems.ROBOT_UPGRADE,
                DoraItems.BECOME_DORAEMON, DoraItems.WISDOM_KING,
                DoraItems.DIMENSION_SHARD, DoraItems.DIMENSION_CORE
        );

        DoraBlocks.registerItemBlocks(event.getRegistry());
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        event.getRegistry().register(buildEntity(EntityFairy.class, "fairy"));
        event.getRegistry().register(buildEntity(EntityMiningRobot.class, "mining_robot"));
    }

    private static EntityEntry buildEntity(Class<? extends EntityLiving> clazz, String name) {
        return EntityEntryBuilder.create()
                .entity(clazz)
                .id(new ResourceLocation(Reference.MOD_ID, name), entityId++)
                .name(Reference.MOD_ID + "." + name)
                .tracker(64, 3, true)
                .build();
    }
}
