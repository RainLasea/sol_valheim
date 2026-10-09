package vice.sol_valheim;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import vice.sol_valheim.accessors.LunchContainerAccessor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ValheimFoodData
{
    public List<EatenFoodItem> ItemEntries = new ArrayList<>();
    public EatenFoodItem DrinkSlot;
    public int MaxItemSlots = SOLValheim.Config.common.maxSlots;
    private int nourishmentProgress;
    private double pendingHungerTicks;

    public boolean eatItem(Item food)
    {
        return eatItem(food.getDefaultInstance(), null);
    }

    public boolean eatItem(ItemStack stack, LivingEntity entity)
    {
        if (stack.is(Items.ROTTEN_FLESH))
            return false;

        var config = ModConfig.getFoodConfig(stack, entity);
        return eatConfiguredItem(stack, config);
    }

    public boolean eatConfiguredItem(ItemStack stack, ModConfig.Common.FoodConfig config)
    {
        if (stack.isEmpty() || stack.is(Items.ROTTEN_FLESH) || config == null)
            return false;

        // Settle costs against the foods that existed when the cost was requested.
        applyHungerCosts();

        var isDrink = stack.getUseAnimation() == UseAnim.DRINK;
        if (isDrink) {
            if (DrinkSlot != null && !DrinkSlot.canEatEarly())
                return false;

            if (DrinkSlot == null && ItemEntries.size() >= MaxItemSlots) {
                var replaceable = ItemEntries.stream().filter(EatenFoodItem::canEatEarly).findFirst().orElse(null);
                if (replaceable == null) return false;
                ItemEntries.remove(replaceable);
            }

            if (DrinkSlot == null)
                DrinkSlot = new EatenFoodItem(stack, config.getTime(), config);
            else {
                DrinkSlot.ticksLeft = config.getTime();
                DrinkSlot.setFood(stack, config);
            }

            return true;
        }

        var existing = getEatenFood(stack, config);
        if (existing != null)
        {
            if (!existing.canEatEarly())
                return false;

            existing.ticksLeft = config.getTime();
            existing.setFood(stack, config);
            return true;
        }

        if (ItemEntries.size() < foodSlotCapacity())
        {
            ItemEntries.add(new EatenFoodItem(stack, config.getTime(), config));
            return true;
        }

        for (var item : ItemEntries)
        {
            if (item.canEatEarly())
            {
                item.ticksLeft = config.getTime();
                item.setFood(stack, config);
                return true;
            }
        }
        if (DrinkSlot != null && DrinkSlot.canEatEarly()) {
            DrinkSlot = null;
            ItemEntries.add(new EatenFoodItem(stack, config.getTime(), config));
            return true;
        }
        return false;
    }

    public boolean canEat(Item food)
    {
        return canEat(food.getDefaultInstance(), null);
    }

    public boolean canEat(ItemStack stack, LivingEntity entity)
    {
        if (stack.is(Items.ROTTEN_FLESH))
            return true;

        var config = ModConfig.getFoodConfig(stack, entity);
        if (config == null) return false;
        if (stack.getUseAnimation() == UseAnim.DRINK)
            return DrinkSlot != null ? DrinkSlot.canEatEarly()
                    : ItemEntries.size() < MaxItemSlots || ItemEntries.stream().anyMatch(EatenFoodItem::canEatEarly);

        var existing = getEatenFood(stack, config);
        if (existing != null)
            return existing.canEatEarly();

        if (ItemEntries.size() < foodSlotCapacity())
            return true;

        return ItemEntries.stream().anyMatch(EatenFoodItem::canEatEarly)
                || DrinkSlot != null && DrinkSlot.canEatEarly();
    }

    private int foodSlotCapacity() {
        return Math.max(0, MaxItemSlots - (DrinkSlot == null ? 0 : 1));
    }

    public void setMaxSlots(int slots) {
        MaxItemSlots = net.minecraft.util.Mth.clamp(slots, 2, 5);
        int capacity = foodSlotCapacity();
        if (ItemEntries.size() > capacity) ItemEntries.subList(capacity, ItemEntries.size()).clear();
    }

    public EatenFoodItem getEatenFood(Item food) {
        return ItemEntries.stream()
                .filter((item) -> item.item == food)
                .findFirst()
                .orElse(null);
    }

    private EatenFoodItem getEatenFood(ItemStack stack, ModConfig.Common.FoodConfig config) {
        boolean dynamic = config != ModConfig.getFoodConfig(stack.getItem());
        return ItemEntries.stream().filter(entry -> entry.item == stack.getItem()
                && (stack.getItem() instanceof LunchContainerAccessor
                    || !(dynamic || entry.dynamicConfig != null) || sameFoodComponents(entry.stack, stack)))
                .findFirst().orElse(null);
    }

    private static boolean sameFoodComponents(ItemStack first, ItemStack second) {
        // Cosmetic names and lore must not turn the same recipe into another food slot.
        return GameVersion.sameFoodComponents(first, second);
    }

    public void clear()
    {
        ItemEntries.clear();
        DrinkSlot = null;
        nourishmentProgress = 0;
        pendingHungerTicks = 0;
    }

    public void applyDeathPenalty() {
        applyHungerCosts();
        if (SOLValheim.Config.common.keepFoodOnDeath) return;
        nourishmentProgress = 0;
        ItemEntries.removeIf(item -> !retainAfterDeath(item));
        if (DrinkSlot != null && !retainAfterDeath(DrinkSlot)) DrinkSlot = null;
    }

    private static boolean retainAfterDeath(EatenFoodItem item) {
        var config = item.getConfig();
        // Check the pre-death timer against the full duration, then keep one fifth.
        if (config == null || (long) item.ticksLeft * 5 < config.getTime()) return false;
        item.ticksLeft /= 5;
        return item.ticksLeft > 0;
    }

    public void tick()
    {
        tick(false);
    }

    public void tick(boolean nourished)
    {
        applyHungerCosts();
        if (!hasFood()) {
            nourishmentProgress = 0;
            return;
        }
        if (consumeTicks(1, nourished) == 0) return;
        for (var item : ItemEntries)
        {
            item.ticksLeft--;
        }

        if (DrinkSlot != null) {
            DrinkSlot.ticksLeft--;
            if (DrinkSlot.ticksLeft <= 0)
                DrinkSlot = null;
        }

        ItemEntries.removeIf(item -> item.ticksLeft <= 0);
        ItemEntries.sort(Comparator.comparingInt(a -> a.ticksLeft));
    }

    /** Accumulate actual costs, calculating the curve at most once per food tick. */
    public void queueHungerCost(double seconds) {
        if (!SOLValheim.Config.common.convertHungerCosts || !Double.isFinite(seconds) || seconds <= 0
                || hungerSlotCount() == 0) return;
        // Bound corrupt or extreme requests without overflowing the tick budget.
        pendingHungerTicks = Math.min(1.0e12, pendingHungerTicks + seconds * 20.0);
    }

    public void discardHungerCosts() {
        pendingHungerTicks = 0;
    }

    public void applyHungerCosts() {
        if (pendingHungerTicks <= 0) return;
        double budget = pendingHungerTicks;
        pendingHungerTicks = 0;
        int slots = hungerSlotCount();
        if (!SOLValheim.Config.common.convertHungerCosts || slots == 0) return;
        double perSlot = budget / slots;
        for (var item : ItemEntries) consumeHungerTime(item, perSlot);
        if (SOLValheim.Config.common.hungerConsumesDrinks && DrinkSlot != null)
            consumeHungerTime(DrinkSlot, perSlot);
        ItemEntries.removeIf(item -> item.ticksLeft <= 0);
        if (DrinkSlot != null && DrinkSlot.ticksLeft <= 0) DrinkSlot = null;
    }

    private int hungerSlotCount() {
        return ItemEntries.size() + (SOLValheim.Config.common.hungerConsumesDrinks && DrinkSlot != null ? 1 : 0);
    }

    private static void consumeHungerTime(EatenFoodItem item, double budget) {
        var config = item.getConfig();
        if (config == null || item.ticksLeft <= 0) return;
        double fraction = Math.min(1.0, (double) item.ticksLeft / config.getTime());
        double loss = budget * Math.pow(fraction, SOLValheim.Config.common.hungerConsumptionExponent)
                + item.hungerRemainder;
        int wholeTicks = (int) Math.min(item.ticksLeft, Math.floor(loss));
        item.ticksLeft -= wholeTicks;
        item.hungerRemainder = item.ticksLeft > 0 ? loss - wholeTicks : 0;
    }

    public float getTotalFoodNutrition()
    {
        float nutrition = 0f;
        for (var item : ItemEntries)
        {
            ModConfig.Common.FoodConfig food = item.getConfig();
            if (food == null)
                continue;

            nutrition += food.getHearts();
        }

        if (DrinkSlot != null)
        {
            ModConfig.Common.FoodConfig food = DrinkSlot.getConfig();
            if (food != null)
            {
                nutrition += food.getHearts();
            }

            nutrition = nutrition * (1.0f + SOLValheim.Config.common.drinkSlotFoodEffectivenessBonus);
        }

        return nutrition;
    }

    public float getRegenSpeed()
    {
        float regen = 0.25f;
        for (var item : ItemEntries)
        {
            ModConfig.Common.FoodConfig food = item.getConfig();
            if (food == null)
                continue;

            regen += food.getHealthRegen();
        }

        if (DrinkSlot != null)
        {
            ModConfig.Common.FoodConfig food = DrinkSlot.getConfig();
            if (food != null)
            {
                regen += food.getHealthRegen();
            }

            regen = regen * (1.0f + SOLValheim.Config.common.drinkSlotFoodEffectivenessBonus);
        }

        return regen;
    }

    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        int count = 0;
        tag.putInt("max_slots", MaxItemSlots);
        tag.putInt("count", ItemEntries.size());
        tag.putInt("nourishment_progress", nourishmentProgress);
        tag.putDouble("pending_hunger_ticks", pendingHungerTicks);
        for (var item : ItemEntries)
        {
            tag.putString("id" + count, BuiltInRegistries.ITEM.getKey(item.item).toString());
            tag.putInt("ticks" + count, item.ticksLeft);
            tag.putDouble("hunger_remainder" + count, item.hungerRemainder);
            tag.put("stack" + count, GameVersion.saveStack(item.stack, registries));
            if (item.dynamicConfig != null) tag.put("values" + count, saveValues(item.dynamicConfig));
            count++;
        }

        if (DrinkSlot != null)
        {
            tag.putString("drink", BuiltInRegistries.ITEM.getKey(DrinkSlot.item).toString());
            tag.putInt("drinkticks", DrinkSlot.ticksLeft);
            tag.putDouble("drink_hunger_remainder", DrinkSlot.hungerRemainder);
            tag.put("drinkstack", GameVersion.saveStack(DrinkSlot.stack, registries));
            if (DrinkSlot.dynamicConfig != null) tag.put("drinkvalues", saveValues(DrinkSlot.dynamicConfig));
        }

        return tag;
    }

    public static ValheimFoodData read(CompoundTag tag, HolderLookup.Provider registries) {
        var instance = new ValheimFoodData();

        if (tag == null) return instance;
        instance.nourishmentProgress = net.minecraft.util.Mth.clamp(tag.getInt("nourishment_progress"), 0, 19);
        instance.pendingHungerTicks = finiteDouble(tag.getDouble("pending_hunger_ticks"), 1.0e12);
        int size = Math.min(Math.max(0, tag.getInt("count")), 5);
        for (int count = 0; count < size; count++) {
            var stack = readStack(tag, "stack" + count, "id" + count, registries);
            var config = readValues(tag, "values" + count, stack);
            int ticks = tag.getInt("ticks" + count);
            if (config != null && ticks > 0 && instance.getEatenFood(stack, config) == null
                    && instance.ItemEntries.size() < instance.MaxItemSlots)
            {
                var entry = new EatenFoodItem(stack, ticks, config);
                entry.hungerRemainder = finiteDouble(tag.getDouble("hunger_remainder" + count), Math.nextDown(1.0));
                instance.ItemEntries.add(entry);
            }
        }
        var drink = readStack(tag, "drinkstack", "drink", registries);
        var config = readValues(tag, "drinkvalues", drink);
        int ticks = tag.getInt("drinkticks");
        if (config != null && ticks > 0) {
            instance.DrinkSlot = new EatenFoodItem(drink, ticks, config);
            instance.DrinkSlot.hungerRemainder = finiteDouble(tag.getDouble("drink_hunger_remainder"), Math.nextDown(1.0));
        }
        instance.setMaxSlots(SOLValheim.Config.common.maxSlots);
        return instance;
    }

    private static ItemStack readStack(CompoundTag tag, String stackKey, String legacyKey, HolderLookup.Provider registries) {
        if (tag.contains(stackKey, Tag.TAG_COMPOUND))
            return GameVersion.readStack(tag.getCompound(stackKey), registries);
        var id = ResourceLocation.tryParse(tag.getString(legacyKey));
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) return ItemStack.EMPTY;
        return BuiltInRegistries.ITEM.get(id).getDefaultInstance();
    }

    private static CompoundTag saveValues(ModConfig.Common.FoodConfig config) {
        var values = new CompoundTag();
        values.putInt("nutrition", config.nutrition);
        values.putFloat("saturation", config.saturationModifier);
        values.putFloat("regen", config.healthRegenModifier);
        return values;
    }

    private static ModConfig.Common.FoodConfig readValues(CompoundTag tag, String key, ItemStack stack) {
        if (stack.isEmpty()) return null;
        var config = ModConfig.getFoodConfig(stack, null);
        if (!tag.contains(key, Tag.TAG_COMPOUND)) return config;
        var values = tag.getCompound(key);
        var saved = config == null ? new ModConfig.Common.FoodConfig() : config.copy();
        saved.nutrition = net.minecraft.util.Mth.clamp(values.getInt("nutrition"), 0, 1000);
        saved.saturationModifier = finiteValue(values.getFloat("saturation"), 0, 100);
        saved.healthRegenModifier = finiteValue(values.getFloat("regen"), 0, 100);
        return saved;
    }

    private static float finiteValue(float value, float min, float max) {
        return Float.isFinite(value) ? net.minecraft.util.Mth.clamp(value, min, max) : 1f;
    }

    private static double finiteDouble(double value, double max) {
        return Double.isFinite(value) ? Math.max(0, Math.min(value, max)) : 0;
    }

    public boolean hasFood() {
        return !ItemEntries.isEmpty() || DrinkSlot != null;
    }

    public void passNight(long ticks) {
        passNight(ticks, false);
    }

    public void passNight(long ticks, boolean nourished) {
        applyHungerCosts();
        if (!hasFood()) {
            nourishmentProgress = 0;
            return;
        }
        ticks = consumeTicks(Math.max(0, ticks), nourished);
        for (var item : ItemEntries) item.ticksLeft = (int) Math.min(item.ticksLeft, Math.max(1200L, item.ticksLeft - ticks));
        if (DrinkSlot != null) DrinkSlot.ticksLeft = (int) Math.min(DrinkSlot.ticksLeft, Math.max(1200L, DrinkSlot.ticksLeft - ticks));
    }

    private long consumeTicks(long ticks, boolean nourished) {
        if (!nourished) {
            nourishmentProgress = 0;
            return ticks;
        }
        int partial = (int) (ticks % 20) * 11 + nourishmentProgress;
        nourishmentProgress = partial % 20;
        return ticks / 20 * 11 + partial / 20;
    }

    public static class EatenFoodItem {
        public Item item;
        public ItemStack stack;
        public int ticksLeft;
        private ModConfig.Common.FoodConfig dynamicConfig;
        private double hungerRemainder;

        public ModConfig.Common.FoodConfig getConfig() {
            return dynamicConfig != null ? dynamicConfig : ModConfig.getFoodConfig(item);
        }

        public boolean canEatEarly() {
            if (ticksLeft < 1200)
                return true;

            var config = getConfig();
            if (config == null)
                return false;

            return ((float) this.ticksLeft / config.getTime()) < SOLValheim.Config.common.eatAgainPercentage;
        }

        public EatenFoodItem(Item item, int ticksLeft)
        {
            this(item.getDefaultInstance(), ticksLeft, ModConfig.getFoodConfig(item));
        }

        public EatenFoodItem(ItemStack stack, int ticksLeft, ModConfig.Common.FoodConfig config) {
            setFood(stack, config);
            this.ticksLeft = ticksLeft;
        }

        private void setFood(ItemStack stack, ModConfig.Common.FoodConfig config) {
            this.hungerRemainder = 0;
            this.item = stack.getItem();
            this.stack = stack.copyWithCount(1);
            this.dynamicConfig = config == ModConfig.getFoodConfig(item) ? null : config.copy();
        }

        public EatenFoodItem(EatenFoodItem eaten)
        {
            this.item = eaten.item;
            this.stack = eaten.stack.copy();
            this.dynamicConfig = eaten.dynamicConfig == null ? null : eaten.dynamicConfig.copy();
            this.ticksLeft = eaten.ticksLeft;
            this.hungerRemainder = eaten.hungerRemainder;
        }
    }
}
