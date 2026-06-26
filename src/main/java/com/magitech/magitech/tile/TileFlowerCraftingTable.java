package com.magitech.magitech.tile;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.oredict.OreDictionary;
import vazkii.botania.api.BotaniaAPI;
import vazkii.botania.api.recipe.RecipePetals;

import javax.annotation.Nullable;
import java.util.*;

public class TileFlowerCraftingTable extends TileBase implements IFluidHandler {

    public static final int ENERGY_CAPACITY = 20000;
    public static final int ENERGY_PER_TICK = 8;
    public static final int WATER_PER_CRAFT = 1000;
    public static final int TANK_CAPACITY = 4000;

    private static final int MATERIAL_SLOTS = 32;
    private static final int SEED_SLOT = 32;
    private static final int OUTPUT_SLOT = 33;

    // ---------- 配方缓存 ----------
    private static class CachedRequirement {
        final int count;
        final List<ItemStack> options;
        CachedRequirement(int count, List<ItemStack> options) {
            this.count = count;
            this.options = options;
        }
    }

    private static Map<RecipePetals, List<CachedRequirement>> recipeCache = null;
    private static boolean recipesInitialized = false;

    // ---------- 实例字段 ----------
    private final FluidTank waterTank;

    public TileFlowerCraftingTable() {
        super(33, 1, ENERGY_CAPACITY);
        this.waterTank = new FluidTank(TANK_CAPACITY) {
            @Override
            protected void onContentsChanged() {
                TileFlowerCraftingTable.this.markDirty();
            }
        };
    }

    // ==========================================
    // 核心合成逻辑（无状态机，瞬间完成）
    // ==========================================
    @Override
    public void update() {
        if (world.isRemote) return;
        tryCraft();
    }

    private void tryCraft() {
        // 1. 能量检查
        if (energyStorage.getEnergyStored() < ENERGY_PER_TICK) return;

        // 2. 水检查：必须存在，且注册名包含 "water"（兼容所有模组的水）
        FluidStack tankFluid = waterTank.getFluid();
        if (tankFluid == null || tankFluid.amount < WATER_PER_CRAFT) return;
        if (tankFluid.getFluid() == null ||
                !tankFluid.getFluid().getName().toLowerCase().contains("water")) {
            return;
        }

        // 3. 种子
        ItemStack seed = itemHandler.getStackInSlot(SEED_SLOT);
        if (seed.isEmpty()) return;

        // 4. 收集材料（0~31）
        List<ItemStack> materials = new ArrayList<>();
        for (int i = 0; i < MATERIAL_SLOTS; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                materials.add(stack.copy());
            }
        }
        if (materials.isEmpty()) return;

        // 5. 初始化配方缓存（仅一次）
        initRecipeCache();

        // 6. 匹配配方
        RecipePetals matchedRecipe = null;
        List<CachedRequirement> matchedRequirements = null;
        for (Map.Entry<RecipePetals, List<CachedRequirement>> entry : recipeCache.entrySet()) {
            if (canMatch(materials, entry.getValue())) {
                matchedRecipe = entry.getKey();
                matchedRequirements = entry.getValue();
                break;
            }
        }
        if (matchedRecipe == null) return;

        // 7. 计算能量消耗：(需求项数 + 种子) * 每次能耗
        int totalEnergy = (matchedRequirements.size() + 1) * ENERGY_PER_TICK;
        if (energyStorage.getEnergyStored() < totalEnergy) return;

        // 8. 输出槽检查
        ItemStack output = matchedRecipe.getOutput().copy();
        ItemStack outSlot = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (!outSlot.isEmpty()) {
            if (!ItemHandlerHelper.canItemStacksStack(outSlot, output)
                    || outSlot.getCount() + output.getCount() > outSlot.getMaxStackSize()) {
                return;
            }
        }

        // 9. 执行消耗
        energyStorage.extractEnergy(totalEnergy, false);
        waterTank.drain(WATER_PER_CRAFT, true);
        consumeMaterials(matchedRequirements);
        itemHandler.getStackInSlot(SEED_SLOT).shrink(1);
        if (outSlot.isEmpty()) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, output);
        } else {
            outSlot.grow(output.getCount());
        }
        markDirty();
    }

    // ---------- 配方系统 ----------
    private static void initRecipeCache() {
        if (recipesInitialized) return;
        synchronized (TileFlowerCraftingTable.class) {
            if (recipesInitialized) return;
            recipeCache = new HashMap<>();
            for (RecipePetals recipe : BotaniaAPI.petalRecipes) {
                List<CachedRequirement> reqs = convertRecipe(recipe);
                if (reqs != null) recipeCache.put(recipe, reqs);
            }
            recipesInitialized = true;
        }
    }

    private static List<CachedRequirement> convertRecipe(RecipePetals recipe) {
        List<Object> inputs = recipe.getInputs();
        List<CachedRequirement> requirements = new ArrayList<>();
        for (Object obj : inputs) {
            if (obj instanceof ItemStack) {
                ItemStack stack = (ItemStack) obj;
                if (stack.isEmpty()) continue;
                ItemStack typeOnly = stack.copy();
                typeOnly.setCount(1);
                requirements.add(new CachedRequirement(stack.getCount(), Collections.singletonList(typeOnly)));
            } else if (obj instanceof String) {
                List<ItemStack> ores = OreDictionary.getOres((String) obj, false);
                if (ores.isEmpty()) return null;
                List<ItemStack> options = new ArrayList<>();
                for (ItemStack ore : ores) {
                    ItemStack option = ore.copy();
                    option.setCount(1);
                    options.add(option);
                }
                requirements.add(new CachedRequirement(1, options));
            } else {
                return null;
            }
        }
        return requirements;
    }

    private boolean canMatch(List<ItemStack> materials, List<CachedRequirement> requirements) {
        List<ItemStack> copy = new ArrayList<>();
        for (ItemStack s : materials) copy.add(s.copy());
        for (CachedRequirement req : requirements) {
            int needed = req.count;
            for (ItemStack mat : copy) {
                if (mat.isEmpty() || mat.getCount() <= 0) continue;
                for (ItemStack option : req.options) {
                    if (OreDictionary.itemMatches(option, mat, false)) {
                        int take = Math.min(needed, mat.getCount());
                        mat.shrink(take);
                        needed -= take;
                        if (needed == 0) break;
                    }
                }
            }
            if (needed > 0) return false;
        }
        return true;
    }

    private void consumeMaterials(List<CachedRequirement> requirements) {
        for (CachedRequirement req : requirements) {
            int remaining = req.count;
            for (int i = 0; i < MATERIAL_SLOTS && remaining > 0; i++) {
                ItemStack slotStack = itemHandler.getStackInSlot(i);
                if (slotStack.isEmpty()) continue;
                for (ItemStack option : req.options) {
                    if (OreDictionary.itemMatches(option, slotStack, false)) {
                        int take = Math.min(remaining, slotStack.getCount());
                        slotStack.shrink(take);
                        remaining -= take;
                        break;
                    }
                }
            }
        }
    }

    // ==========================================
    // 流体能力：直接实现 IFluidHandler 接口
    // ==========================================
    @Override
    public IFluidTankProperties[] getTankProperties() {
        IFluidTankProperties[] props = waterTank.getTankProperties();
        if (DEBUG) {
            System.out.println("[FlowerTable] getTankProperties called, length=" + props.length);
            for (int i = 0; i < props.length; i++) {
                System.out.println("[FlowerTable]   Tank " + i + ": contents=" + props[i].getContents() +
                        " capacity=" + props[i].getCapacity() + " canFill=" + props[i].canFill() +
                        " canDrain=" + props[i].canDrain());
            }
        }
        return props;
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (DEBUG) {
            System.out.println("[FlowerTable] fill called: resource=" + resource
                    + " doFill=" + doFill + " tankAmount=" + waterTank.getFluidAmount());
        }
        if (resource == null || resource.getFluid() == null) {
            if (DEBUG) System.out.println("[FlowerTable] fill rejected: null fluid");
            return 0;
        }

        // 临时移除过滤，打印流体名称
        String fluidName = resource.getFluid().getName();
        System.out.println("[FlowerTable] fluid name: " + fluidName);

        // 暂时允许所有流体，以便测试管道
        int filled = waterTank.fill(resource, doFill);
        if (DEBUG) System.out.println("[FlowerTable] fill result: " + filled);
        return filled;
    }

    @Nullable
    @Override
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        if (DEBUG) System.out.println("[FlowerTable] drain(stack) called: resource=" + resource + " doDrain=" + doDrain);
        return waterTank.drain(resource, doDrain);
    }

    @Nullable
    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        if (DEBUG) System.out.println("[FlowerTable] drain(amount) called: maxDrain=" + maxDrain + " doDrain=" + doDrain);
        return waterTank.drain(maxDrain, doDrain);
    }

    private static final boolean DEBUG = true;

    // ==========================================
    // Capability 暴露（完全覆盖基类逻辑）
    // ==========================================
    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (DEBUG) {
            System.out.println("[FlowerTable] hasCapability called: " + capability + " side=" + facing);
        }
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            if (DEBUG) System.out.println("[FlowerTable] Fluid capability requested -> true");
            return true;
        }
        boolean result = super.hasCapability(capability, facing);
        if (DEBUG) System.out.println("[FlowerTable] hasCapability other: " + result);
        return result;
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (DEBUG) {
            System.out.println("[FlowerTable] getCapability called: " + capability + " side=" + facing);
        }
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            if (DEBUG) System.out.println("[FlowerTable] Returning IFluidHandler: " + this);
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(this);
        }
        T result = super.getCapability(capability, facing);
        if (DEBUG) System.out.println("[FlowerTable] getCapability other: " + result);
        return result;
    }

    // ==========================================
    // NBT 读写
    // ==========================================
    @Override
    protected void readCustomNBT(NBTTagCompound compound) {
        if (compound.hasKey("water")) {
            waterTank.readFromNBT(compound.getCompoundTag("water"));
        }
    }

    @Override
    protected void writeCustomNBT(NBTTagCompound compound) {
        NBTTagCompound waterTag = new NBTTagCompound();
        waterTank.writeToNBT(waterTag);
        compound.setTag("water", waterTag);
    }

    public FluidTank getWaterTank() {
        return waterTank;
    }
}