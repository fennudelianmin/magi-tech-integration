package com.dorapack.dorapack.item;

import com.dorapack.dorapack.capability.DoraCapabilities;
import com.dorapack.dorapack.capability.IDoraPlayerData;
import com.dorapack.dorapack.config.DoraConfig;
import com.dorapack.dorapack.item.base.ItemResearchLocked;
import com.dorapack.dorapack.research.ResearchKeys;
import com.dorapack.dorapack.util.DoraUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.WorldInfo;

import javax.annotation.Nonnull;

/**
 * Weather controller ("天气控制器"). Right-click cycles the current dimension's weather
 * clear -> rain -> thunder -> clear. Daily-limited per player; the change is server-authoritative
 * and broadcast to all players in the world.
 */
public class ItemWeatherController extends ItemResearchLocked {

    private static final String USAGE_KEY = "weathercontroller";
    private static final int CLEAR_DURATION = 6000;

    public ItemWeatherController(String name) {
        super(name, 1, ResearchKeys.WEATHER_CONTROLLER);
    }

    @Override
    @Nonnull
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, @Nonnull EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!isUnlocked(player)) {
            if (!world.isRemote) {
                notifyLocked(player);
            }
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        if (world.isRemote || !(world instanceof WorldServer)) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        IDoraPlayerData data = DoraCapabilities.get(player);
        if (data != null) {
            data.refreshDay(DoraUtil.getWorldDay(world));
            if (data.getDailyUsage(USAGE_KEY) >= DoraConfig.weatherControllerDailyLimit) {
                DoraUtil.sendActionBar(player, TextFormatting.RED + "天气控制器今日已使用");
                return new ActionResult<>(EnumActionResult.FAIL, stack);
            }
            data.incrementDailyUsage(USAGE_KEY, 1);
        }
        cycleWeather((WorldServer) world);
        DoraUtil.sendMessage(player, TextFormatting.AQUA, "天气已改变");
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    private void cycleWeather(WorldServer world) {
        WorldInfo info = world.getWorldInfo();
        if (!info.isRaining()) {
            info.setRaining(true);
            info.setRainTime(CLEAR_DURATION);
        } else if (!info.isThundering()) {
            info.setThundering(true);
            info.setThunderTime(CLEAR_DURATION);
        } else {
            info.setRaining(false);
            info.setThundering(false);
            info.setRainTime(CLEAR_DURATION);
            info.setThunderTime(CLEAR_DURATION);
        }
    }
}
