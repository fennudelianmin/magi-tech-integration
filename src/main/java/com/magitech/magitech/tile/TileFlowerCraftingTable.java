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

/**
 * 花瓣合成工作台。
 * <p>
 * 自动执行 Botania 的花瓣合成。将 Botania 原版花瓣配方缓存到内存中，
 * 将种子与对应的花瓣（及染色材料）合成为功能花。
 * 与其它机器不同，该机器采用"瞬间合成"模式（无状态机），
 * 每 tick 检查条件并在满足时一次性完成合成。
 * <p>
 * 额外需要水（通过 IFluidHandler 接口提供流体支持），
 * 每次合成消耗 1000mb 水（必须含 "water" 的流体）。
 * <p>
 * 槽位分配：
 *   0~31  — 材料槽（花瓣、染料等）
 *   32     — 种子槽
 *   33     — 输出槽
 */
public class TileFlowerCraftingTable extends TileBase implements IFluidHandler {

    /** 最大能量容量（FE） */
    public static final int ENERGY_CAPACITY = 20000;
    /** 每个需求项的基础能耗（FE），总能耗 = (需求项数 + 1(种子)) × 此值 */
    public static final int ENERGY_PER_TICK = 8;
    /** 每次合成消耗的水量（mb） */
    public static final int WATER_PER_CRAFT = 1000;
    /** 水箱容量（mb） */
    public static final int TANK_CAPACITY = 4000;

    /** 材料槽数量（0~31） */
    private static final int MATERIAL_SLOTS = 32;
    /** 种子槽索引（32） */
    private static final int SEED_SLOT = 32;
    /** 输出槽索引（33） */
    private static final int OUTPUT_SLOT = 33;

    // ---------- 配方缓存 ----------

    /**
     * 缓存中的配方需求项。
     * 每个需求项表示需要一个物品，可能有多种可选方案（矿物词典兼容）。
     * 例如"任意黄色花瓣"对应多个选项（黄菊花、向日葵等）。
     */
    private static class CachedRequirement {
        /** 需要的数量 */
        final int count;
        /** 可接受的物品列表（任一即可） */
        final List<ItemStack> options;

        CachedRequirement(int count, List<ItemStack> options) {
            this.count = count;
            this.options = options;
        }
    }

    /** 配方缓存缓存：Botania 花瓣配方 → 预处理后的需求列表 */
    private static Map<RecipePetals, List<CachedRequirement>> recipeCache = null;
    /** 配方是否已初始化的标志 */
    private static boolean recipesInitialized = false;

    // ---------- 实例字段 ----------

    /** 水箱 */
    private final FluidTank waterTank;

    public TileFlowerCraftingTable() {
        super(33, 1, ENERGY_CAPACITY); // 33 输入（32材料+1种子），1输出
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

    /**
     * 每 tick 更新。仅在服务端执行。
     * 每 tick 尝试一次配方匹配和合成（瞬间完成，无需等待多个 tick）。
     */
    @Override
    public void update() {
        if (world.isRemote) return;
        tryCraft();
    }

    /**
     * 尝试执行一次合成。
     * <p>
     * 检查顺序：
     *   1. FE 能量是否足够
     *   2. 水箱中是否有足量水（≥1000mb）
     *   3. 种子槽是否有种子
     *   4. 材料槽是否有物品
     *   5. 初始化和匹配配方缓存
     *   6. 检查输出槽空间
     *   7. 执行消耗（能量、水、材料、种子）和产物输出
     */
    private void tryCraft() {
        // 1. 能量检查
        if (energyStorage.getEnergyStored() < ENERGY_PER_TICK) return;

        // 2. 水检查：必须存在且流体名称包含 "water"
        FluidStack tankFluid = waterTank.getFluid();
        if (tankFluid == null || tankFluid.amount < WATER_PER_CRAFT) return;
        if (tankFluid.getFluid() == null ||
                !tankFluid.getFluid().getName().toLowerCase().contains("water")) {
            return;
        }

        // 3. 种子
        ItemStack seed = itemHandler.getStackInSlot(SEED_SLOT);
        if (seed.isEmpty()) return;

        // 4. 收集材料（槽 0~31）
        List<ItemStack> materials = new ArrayList<>();
        for (int i = 0; i < MATERIAL_SLOTS; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                materials.add(stack.copy());
            }
        }
        if (materials.isEmpty()) return;

        // 5. 初始化配方缓存（仅第一次执行）
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

        // 7. 计算能量消耗
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

    /**
     * 初始化配方缓存。
     * 线程安全的双重检查锁定，确保只执行一次。
     * 遍历 Botania 所有花瓣配方，将输入的原料列表转换为 CachedRequirement 列表。
     * ItemStack 类型的输入保留原样；String 类型（矿物词典）展开为所有可选物品。
     */
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

    /**
     * 将 Botania 花瓣配方转为 CachedRequirement 列表。
     * 输入中的 Object 可以是 ItemStack（具体物品）或 String（矿物词典名称）。
     * 对于矿物词典类型，展开为该矿物词典下所有物品作为可选方案。
     */
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

    /**
     * 检查输入的材料列表是否满足配方需求。
     * 对每个需求项，依次从材料列表中扣除，所有需求项都满足则返回 true。
     */
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

    /**
     * 消耗材料：遍历输入槽，按配方需求从各槽中扣除对应物品。
     * 使用 OreDictionary.itemMatches 进行兼容匹配。
     */
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

    private static final boolean DEBUG = true;

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

        String fluidName = resource.getFluid().getName();
        System.out.println("[FlowerTable] fluid name: " + fluidName);

        // 暂时允许所有流体以便测试管道连接
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
