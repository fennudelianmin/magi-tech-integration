package com.magitech.magitech.creativetab;

import com.magitech.magitech.block.ModBlocks;
import com.magitech.magitech.item.ModItems;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class MagiTechTab extends CreativeTabs {

    public static final MagiTechTab INSTANCE = new MagiTechTab();

    public MagiTechTab() {
        super("magitech");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ItemStack createIcon() {
        return new ItemStack(ModBlocks.RUNE_ALTAR_MACHINE);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void displayAllRelevantItems(NonNullList<ItemStack> items) {
        // 机器方块（按顺序排列）
        addBlockItems(items, ModBlocks.LIVINGROCK_CULTIVATOR);
        addBlockItems(items, ModBlocks.FLOWER_CRAFTING_TABLE);
        addBlockItems(items, ModBlocks.RUNE_ALTAR_MACHINE);
        addBlockItems(items, ModBlocks.TERRA_PLATE_ASSEMBLER);
        addBlockItems(items, ModBlocks.ELVEN_TRADE_MACHINE);

        // 中间合成物品
        addItemStacks(items, ModItems.RUNE_ALTAR_CORE);
        addItemStacks(items, ModItems.TERRA_ASSEMBLY_CORE);
        addItemStacks(items, ModItems.ELVEN_TRADE_CORE);
    }

    private void addBlockItems(NonNullList<ItemStack> items, net.minecraft.block.Block block) {
        block.getSubBlocks(this, items);
    }

    private void addItemStacks(NonNullList<ItemStack> items, net.minecraft.item.Item item) {
        item.getSubItems(this, items);
    }
}
