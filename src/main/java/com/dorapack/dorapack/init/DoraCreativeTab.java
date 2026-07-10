package com.dorapack.dorapack.init;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

/**
 * Creative tab holding all DoraPack items. The icon lazily resolves to the initial bag once
 * items are registered.
 */
public class DoraCreativeTab extends CreativeTabs {

    public static final DoraCreativeTab INSTANCE = new DoraCreativeTab();

    private DoraCreativeTab() {
        super("dorapack");
    }

    @Override
    @Nonnull
    public ItemStack createIcon() {
        if (DoraItems.BAG_INITIAL != null) {
            return new ItemStack(DoraItems.BAG_INITIAL);
        }
        return new ItemStack(net.minecraft.init.Items.LEATHER);
    }

}
