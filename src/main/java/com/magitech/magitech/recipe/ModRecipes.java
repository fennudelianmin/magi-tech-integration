package com.magitech.magitech.recipe;

import com.magitech.magitech.block.ModBlocks;
import com.magitech.magitech.item.ModItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

@Mod.EventBusSubscriber
public class ModRecipes {

    private static final String BOTANIA = "botania";
    private static final String AE2 = "appliedenergistics2";

    public static void register() {
        // 中间件配方通过JSON自动加载
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        registerMachineRecipes(event);
    }

    private static void registerMachineRecipes(RegistryEvent.Register<IRecipe> event) {
        // --- 符文祭坛机器: SRS / RAR / SRS ---
        // S=stone (ore), R=any rune, A=rune_altar_core
        event.getRegistry().register(new ShapedOreRecipe(
            null, new ItemStack(ModBlocks.RUNE_ALTAR_MACHINE),
            "SRS", "RAR", "SRS",
            'S', "stone",
            'R', "runeWaterB",  // 使用水符文作为占位符文
            'A', ModItems.RUNE_ALTAR_CORE
        ).setRegistryName("magitech:rune_altar_machine_recipe"));

        // --- 花药台机器: P_P / WFC / P_P ---
        // P=any petal, W=water_bucket, F=flower_bag, C=calculation_processor
        Item calcProcessor = Item.getByNameOrId(AE2 + ":material");
        Item flowerBag = Item.getByNameOrId(BOTANIA + ":flower_bag");
        if (calcProcessor != null && flowerBag != null) {
            event.getRegistry().register(new ShapedOreRecipe(
                null, new ItemStack(ModBlocks.FLOWER_CRAFTING_TABLE),
                "P P", "WFC", "P P",
                'P', "petalWhite",  // 使用白色花瓣作为占位
                'W', new ItemStack(Item.getByNameOrId("minecraft:water_bucket")),
                'F', new ItemStack(flowerBag),
                'C', new ItemStack(calcProcessor, 1, 26) // Calculation Processor
            ).setRegistryName("magitech:flower_crafting_table_recipe"));
        }

        // --- 精灵交易器: QEQ / ELE / QEQ ---
        // Q=elven quartz, E=elementium block, L=elven_trade_core
        Item quartz = Item.getByNameOrId(BOTANIA + ":quartz");
        Item storage = Item.getByNameOrId(BOTANIA + ":storage");
        if (quartz != null && storage != null) {
            event.getRegistry().register(new ShapedOreRecipe(
                null, new ItemStack(ModBlocks.ELVEN_TRADE_MACHINE),
                "QEQ", "ELE", "QEQ",
                'Q', new ItemStack(quartz, 1, 5),   // elf quartz (精灵石英)
                'E', new ItemStack(storage, 1, 2),    // elementium block
                'L', ModItems.ELVEN_TRADE_CORE
            ).setRegistryName("magitech:elven_trade_machine_recipe"));
        }

        // --- 泰拉凝聚器: TMT / MCM / TMT ---
        // T=terrasteel block, M=manasteel block, C=terra_assembly_core
        if (storage != null) {
            event.getRegistry().register(new ShapedOreRecipe(
                null, new ItemStack(ModBlocks.TERRA_PLATE_ASSEMBLER),
                "TMT", "MCM", "TMT",
                'T', new ItemStack(storage, 1, 1),    // terrasteel block
                'M', new ItemStack(storage, 1, 0),    // manasteel block
                'C', ModItems.TERRA_ASSEMBLY_CORE
            ).setRegistryName("magitech:terra_plate_assembler_recipe"));
        }

        // --- 活石培育器: LSL / SGS / LSL ---
        // L=livingrock, S=sky_stone_block, G=engineering_processor
        Item skyStone = Item.getByNameOrId(AE2 + ":sky_stone_block");
        if (calcProcessor != null && skyStone != null) {
            event.getRegistry().register(new ShapedOreRecipe(
                null, new ItemStack(ModBlocks.LIVINGROCK_CULTIVATOR),
                "LSL", "SGS", "LSL",
                'L', "livingrock",
                'S', new ItemStack(skyStone),
                'G', new ItemStack(calcProcessor, 1, 27) // Engineering Processor
            ).setRegistryName("magitech:livingrock_cultivator_recipe"));
        }
    }
}
