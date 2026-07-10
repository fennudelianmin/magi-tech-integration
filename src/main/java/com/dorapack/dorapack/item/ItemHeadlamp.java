package com.dorapack.dorapack.item;

import com.dorapack.dorapack.init.DoraCreativeTab;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

/**
 * Searchlight headlamp ("探照头灯"). Worn in the helmet slot; grants permanent night vision while
 * equipped (applied by {@code EquipmentTickHandler}) at a small movement-speed penalty.
 */
public class ItemHeadlamp extends ItemArmor {

    public ItemHeadlamp(String name) {
        super(ArmorMaterial.IRON, 0, EntityEquipmentSlot.HEAD);
        setRegistryName(name);
        setTranslationKey(name);
        setCreativeTab(DoraCreativeTab.INSTANCE);
        setMaxStackSize(1);
        setMaxDamage(0);
    }

    public static boolean isEquipped(EntityLivingBase entity) {
        ItemStack head = entity.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
        return !head.isEmpty() && head.getItem() instanceof ItemHeadlamp;
    }
}
