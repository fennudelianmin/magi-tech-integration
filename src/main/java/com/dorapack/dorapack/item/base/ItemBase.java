package com.dorapack.dorapack.item.base;

import com.dorapack.dorapack.init.DoraCreativeTab;
import net.minecraft.item.Item;

/**
 * Base class for all simple DoraPack items. Sets the registry/translation name and creative tab.
 */
public class ItemBase extends Item {

    public ItemBase(String name) {
        setRegistryName(name);
        setTranslationKey(name);
        setCreativeTab(DoraCreativeTab.INSTANCE);
    }

    public ItemBase(String name, int maxStackSize) {
        this(name);
        setMaxStackSize(maxStackSize);
    }
}
