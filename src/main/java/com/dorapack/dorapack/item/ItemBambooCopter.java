package com.dorapack.dorapack.item;

import com.dorapack.dorapack.config.DoraConfig;
import com.dorapack.dorapack.init.DoraCreativeTab;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

/**
 * Bamboo copter ("竹蜻蜓简易"). Worn in the helmet slot. Double-tapping jump triggers a short
 * timed flight, handled by {@code FlightHandler}. Disabled in rain; incurs landing stun.
 *
 * <p>Extends {@link ItemArmor} purely so it is head-slot equippable; it grants no armor points.</p>
 */
public class ItemBambooCopter extends ItemArmor {

    public ItemBambooCopter(String name) {
        super(ArmorMaterial.LEATHER, 0, EntityEquipmentSlot.HEAD);
        setRegistryName(name);
        setTranslationKey(name);
        setCreativeTab(DoraCreativeTab.INSTANCE);
        setMaxDamage(DoraConfig.bambooCopterDurability);
        setMaxStackSize(1);
    }

    /** Grant no protection: this is a utility item, not armour. */
    @Override
    public int getItemEnchantability() {
        return 0;
    }

    public static boolean isEquipped(EntityLivingBase entity) {
        ItemStack head = entity.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
        return !head.isEmpty() && head.getItem() instanceof ItemBambooCopter;
    }
}
