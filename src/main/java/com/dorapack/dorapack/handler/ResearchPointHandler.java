package com.dorapack.dorapack.handler;

import com.dorapack.dorapack.Reference;
import com.dorapack.dorapack.achievement.AchievementManager;
import com.dorapack.dorapack.capability.DoraCapabilities;
import com.dorapack.dorapack.capability.IDoraPlayerData;
import com.dorapack.dorapack.config.DoraConfig;
import com.dorapack.dorapack.util.DoraUtil;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Grants research points from natural survival progress, exactly as specified in the design doc:
 * first crafting-table craft (+10), first descent below Y=40 (+20), killing N zombies (+15), all
 * respecting the per-day cap. Also feeds the block-mining achievement counter.
 *
 * <p>All checks are cheap and event-driven; the only periodic work is a lightweight cave check
 * throttled to once every 40 ticks per player.</p>
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public final class ResearchPointHandler {

    private static final String FLAG_CRAFTED = "flag_crafted_table";
    private static final String FLAG_CAVE = "flag_entered_cave";
    private static final String COUNTER_ZOMBIES = "zombieKills";
    private static final String FLAG_ZOMBIES_REWARDED = "flag_zombies_rewarded";
    private static final int CAVE_Y_THRESHOLD = 40;
    private static final int CAVE_CHECK_INTERVAL = 40;

    private ResearchPointHandler() {
    }

    @SubscribeEvent
    public static void onItemCrafted(net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemCraftedEvent event) {
        if (event.player.world.isRemote) {
            return;
        }
        ItemStack result = event.crafting;
        if (result.getItem() == net.minecraft.item.Item.getItemFromBlock(Blocks.CRAFTING_TABLE)) {
            awardOnce(event.player, FLAG_CRAFTED, DoraConfig.pointsCraftingTable, "首次合成工作台");
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) {
            return;
        }
        EntityPlayer player = event.player;
        if (player.ticksExisted % CAVE_CHECK_INTERVAL != 0) {
            return;
        }
        IDoraPlayerData data = DoraCapabilities.get(player);
        if (data == null) {
            return;
        }
        data.refreshDay(DoraUtil.getWorldDay(player.world));
        if (player.posY < CAVE_Y_THRESHOLD && data.getDailyUsage(FLAG_CAVE) == 0) {
            award(player, data, DoraConfig.pointsEnterCave, "首次进入矿洞");
            data.incrementDailyUsage(FLAG_CAVE, 1);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().world.isRemote || !(event.getEntity() instanceof EntityZombie)) {
            return;
        }
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        IDoraPlayerData data = DoraCapabilities.get(player);
        if (data == null || data.getDailyUsage(FLAG_ZOMBIES_REWARDED) > 0) {
            return;
        }
        data.addCounter(COUNTER_ZOMBIES, 1);
        if (data.getCounter(COUNTER_ZOMBIES) >= DoraConfig.killZombiesRequired) {
            award(player, data, DoraConfig.pointsKillZombies,
                    "击杀 " + DoraConfig.killZombiesRequired + " 只僵尸");
            data.incrementDailyUsage(FLAG_ZOMBIES_REWARDED, 1);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        EntityPlayer player = event.getPlayer();
        if (player == null || player.world.isRemote) {
            return;
        }
        // Feed the mining-master achievement only when using the miner drill.
        ItemStack held = player.getHeldItemMainhand();
        if (!held.isEmpty() && held.getItem() instanceof com.dorapack.dorapack.item.ItemMinerDrill) {
            AchievementManager.onBlockMined(player, 1);
        }
    }

    private static void awardOnce(EntityPlayer player, String flagKey, int amount, String reason) {
        IDoraPlayerData data = DoraCapabilities.get(player);
        if (data == null || data.getCounter(flagKey) > 0) {
            return;
        }
        data.addCounter(flagKey, 1);
        award(player, data, amount, reason);
    }

    private static void award(EntityPlayer player, IDoraPlayerData data, int amount, String reason) {
        int granted = data.addPoints(amount);
        if (granted > 0) {
            DoraUtil.sendMessage(player, TextFormatting.AQUA, "研究点 +" + granted + "（" + reason + "）");
        }
    }
}
