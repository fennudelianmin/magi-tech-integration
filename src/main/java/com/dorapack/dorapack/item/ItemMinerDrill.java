package com.dorapack.dorapack.item;

import com.dorapack.dorapack.init.DoraCreativeTab;
import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

import javax.annotation.Nonnull;

/**
 * Miner drill ("矿工钻头"). An IC2-powered drill: efficiency 3 + fortune 1, consuming IC2 energy per
 * block broken. Implements {@link IElectricItem} so IC2 charging (and the charging station) can
 * top it up. When empty it cannot mine faster than by hand.
 */
public class ItemMinerDrill extends Item implements IElectricItem {

    private static final double MAX_CHARGE = 30_000.0D;
    private static final double TRANSFER_LIMIT = 100.0D;
    private static final int TIER = 1;
    private static final double ENERGY_PER_BLOCK = 50.0D;
    private static final float DIG_SPEED = 12.0F;

    public ItemMinerDrill(String name) {
        setRegistryName(name);
        setTranslationKey(name);
        setCreativeTab(DoraCreativeTab.INSTANCE);
        setMaxStackSize(1);
        setMaxDamage(0);
        setHasSubtypes(false);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, IBlockState state) {
        return ElectricItem.manager.canUse(stack, ENERGY_PER_BLOCK) ? DIG_SPEED : 1.0F;
    }

    @Override
    public boolean onBlockDestroyed(ItemStack stack, net.minecraft.world.World world, IBlockState state,
                                    net.minecraft.util.math.BlockPos pos, EntityLivingBase miner) {
        if (!world.isRemote && state.getBlockHardness(world, pos) > 0.0F) {
            ElectricItem.manager.use(stack, ENERGY_PER_BLOCK, miner);
        }
        return true;
    }

    @Override
    public int getItemEnchantability() {
        return 0;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return enchantment == Enchantments.EFFICIENCY || enchantment == Enchantments.FORTUNE;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public void getSubItems(@Nonnull net.minecraft.creativetab.CreativeTabs tab, @Nonnull NonNullList<ItemStack> items) {
        if (!isInCreativeTab(tab)) {
            return;
        }
        // Provide both an empty and a fully charged variant in the creative tab.
        items.add(new ItemStack(this));
        ItemStack charged = new ItemStack(this);
        ElectricItem.manager.charge(charged, MAX_CHARGE, TIER, true, false);
        items.add(charged);
    }

    /** Applies the built-in efficiency/fortune enchantments to a freshly crafted drill. */
    public static void applyBuiltInEnchants(ItemStack stack) {
        java.util.Map<Enchantment, Integer> enchants = new java.util.HashMap<>(4);
        enchants.put(Enchantments.EFFICIENCY, 3);
        enchants.put(Enchantments.FORTUNE, 1);
        EnchantmentHelper.setEnchantments(enchants, stack);
    }

    // --- IElectricItem ---

    @Override
    public boolean canProvideEnergy(ItemStack stack) {
        return false;
    }

    @Override
    public double getMaxCharge(ItemStack stack) {
        return MAX_CHARGE;
    }

    @Override
    public int getTier(ItemStack stack) {
        return TIER;
    }

    @Override
    public double getTransferLimit(ItemStack stack) {
        return TRANSFER_LIMIT;
    }
}
