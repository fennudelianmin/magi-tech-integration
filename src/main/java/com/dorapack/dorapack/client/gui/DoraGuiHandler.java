package com.dorapack.dorapack.client.gui;

import com.dorapack.dorapack.entity.EntityMiningRobot;
import com.dorapack.dorapack.world.PlayerBagData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import javax.annotation.Nullable;

/**
 * Central GUI handler for DoraPack. Registers the shared bag GUI, the two research-table GUIs and the
 * mining-robot storage GUI.
 *
 * <p>For the robot GUI the {@code x} argument carries the entity id (the robot has no block position),
 * following the standard Forge convention for entity-backed containers.</p>
 */
public class DoraGuiHandler implements IGuiHandler {

    public static final int GUI_BAG = 0;
    public static final int GUI_RESEARCH_SPROUT = 1;
    public static final int GUI_RESEARCH_ENDGAME = 2;
    public static final int GUI_ROBOT = 3;

    @Override
    @Nullable
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        switch (id) {
            case GUI_BAG:
                return new ContainerBag(player, PlayerBagData.get(world).getBag(player.getUniqueID()));
            case GUI_RESEARCH_SPROUT:
                return new ContainerResearch(player, false);
            case GUI_RESEARCH_ENDGAME:
                return new ContainerResearch(player, true);
            case GUI_ROBOT: {
                EntityMiningRobot robot = findRobot(world, x);
                return robot == null ? null : new ContainerRobot(player, robot);
            }
            default:
                return null;
        }
    }

    @Override
    @Nullable
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        switch (id) {
            case GUI_BAG:
                return new GuiBag(player, PlayerBagData.get(world).getBag(player.getUniqueID()));
            case GUI_RESEARCH_SPROUT:
                return new GuiResearch(player, false);
            case GUI_RESEARCH_ENDGAME:
                return new GuiResearch(player, true);
            case GUI_ROBOT: {
                EntityMiningRobot robot = findRobot(world, x);
                return robot == null ? null : new GuiRobot(player, robot);
            }
            default:
                return null;
        }
    }

    @Nullable
    private static EntityMiningRobot findRobot(World world, int entityId) {
        Entity entity = world.getEntityByID(entityId);
        return entity instanceof EntityMiningRobot ? (EntityMiningRobot) entity : null;
    }
}
