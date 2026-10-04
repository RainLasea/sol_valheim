package vice.sol_valheim;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ValheimFoodData
{
    public List<EatenFoodItem> ItemEntries = new ArrayList<>();
    public EatenFoodItem DrinkSlot;
    public int MaxItemSlots = SOLValheim.Config.common.maxSlots;
    private int nourishmentProgress;

    public boolean eatItem(Item food)
    {
        if (food == Items.ROTTEN_FLESH)
            return false;

        var config = ModConfig.getFoodConfig(food);
        if (config == null)
            return false;

        var isDrink = food.getDefaultInstance().getUseAnimation() == UseAnim.DRINK;
        if (isDrink) {
            if (DrinkSlot != null && !DrinkSlot.canEatEarly())
                return false;

            if (DrinkSlot == null)
                DrinkSlot = new EatenFoodItem(food, config.getTime());
            else {
                DrinkSlot.ticksLeft = config.getTime();
                DrinkSlot.item = food;
            }

            return true;
        }

        var existing = getEatenFood(food);
        if (existing != null)
        {
            if (!existing.canEatEarly())
                return false;

            existing.ticksLeft = config.getTime();
            return true;
        }

        if (ItemEntries.size() < MaxItemSlots)
        {
            ItemEntries.add(new EatenFoodItem(food, config.getTime()));
            return true;
        }

        for (var item : ItemEntries)
        {
            if (item.canEatEarly())
            {
                item.ticksLeft = config.getTime();
                item.item = food;
                return true;
            }
        }
        return false;
    }

    public boolean canEat(Item food)
    {
        if (food == Items.ROTTEN_FLESH)
            return true;

        if (food.getDefaultInstance().getUseAnimation() == UseAnim.DRINK)
            return DrinkSlot == null || DrinkSlot.canEatEarly();

        var existing = getEatenFood(food);
        if (existing != null)
            return existing.canEatEarly();

        if (ItemEntries.size() < MaxItemSlots)
            return true;

        return ItemEntries.stream().anyMatch(EatenFoodItem::canEatEarly);
    }

    public EatenFoodItem getEatenFood(Item food) {
        return ItemEntries.stream()
                .filter((item) -> item.item == food)
                .findFirst()
                .orElse(null);
    }

    public void clear()
    {
        ItemEntries.clear();
        DrinkSlot = null;
        nourishmentProgress = 0;
    }

    public void tick()
    {
        tick(false);
    }

    public void tick(boolean nourished)
    {
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

    public float getTotalFoodNutrition()
    {
        float nutrition = 0f;
        for (var item : ItemEntries)
        {
            ModConfig.Common.FoodConfig food = ModConfig.getFoodConfig(item.item);
            if (food == null)
                continue;

            nutrition += food.getHearts();
        }

        if (DrinkSlot != null)
        {
            ModConfig.Common.FoodConfig food = ModConfig.getFoodConfig(DrinkSlot.item);
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
            ModConfig.Common.FoodConfig food = ModConfig.getFoodConfig(item.item);
            if (food == null)
                continue;

            regen += food.getHealthRegen();
        }

        if (DrinkSlot != null)
        {
            ModConfig.Common.FoodConfig food = ModConfig.getFoodConfig(DrinkSlot.item);
            if (food != null)
            {
                regen += food.getHealthRegen();
            }

            regen = regen * (1.0f + SOLValheim.Config.common.drinkSlotFoodEffectivenessBonus);
        }

        return regen;
    }

    public CompoundTag save(CompoundTag tag) {
        int count = 0;
        tag.putInt("max_slots", MaxItemSlots);
        tag.putInt("count", ItemEntries.size());
        tag.putInt("nourishment_progress", nourishmentProgress);
        for (var item : ItemEntries)
        {
            tag.putString("id" + count, BuiltInRegistries.ITEM.getKey(item.item).toString());
            tag.putInt("ticks" + count, item.ticksLeft);
            count++;
        }

        if (DrinkSlot != null)
        {
            tag.putString("drink", BuiltInRegistries.ITEM.getKey(DrinkSlot.item).toString());
            tag.putInt("drinkticks", DrinkSlot.ticksLeft);
        }

        return tag;
    }

    public static ValheimFoodData read(CompoundTag tag) {
        var instance = new ValheimFoodData();

        if (tag == null) return instance;
        instance.nourishmentProgress = Math.clamp(tag.getInt("nourishment_progress"), 0, 19);
        int size = Math.min(Math.max(0, tag.getInt("count")), 5);
        for (int count = 0; count < size; count++) {
            var item = readItem(tag.getString("id" + count));
            int ticks = tag.getInt("ticks" + count);
            if (item != null && ticks > 0 && instance.getEatenFood(item) == null
                    && instance.ItemEntries.size() < instance.MaxItemSlots)
                instance.ItemEntries.add(new EatenFoodItem(item, ticks));
        }
        var drink = readItem(tag.getString("drink"));
        int ticks = tag.getInt("drinkticks");
        if (drink != null && ticks > 0) instance.DrinkSlot = new EatenFoodItem(drink, ticks);
        return instance;
    }

    private static Item readItem(String value) {
        var id = ResourceLocation.tryParse(value);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) return null;
        var item = BuiltInRegistries.ITEM.get(id);
        return ModConfig.getFoodConfig(item) == null ? null : item;
    }

    public boolean hasFood() {
        return !ItemEntries.isEmpty() || DrinkSlot != null;
    }

    public void passNight(long ticks) {
        passNight(ticks, false);
    }

    public void passNight(long ticks, boolean nourished) {
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
        public int ticksLeft;

        public boolean canEatEarly() {
            if (ticksLeft < 1200)
                return true;

            var config = ModConfig.getFoodConfig(item);
            if (config == null)
                return false;

            return ((float) this.ticksLeft / config.getTime()) < SOLValheim.Config.common.eatAgainPercentage;
        }

        public EatenFoodItem(Item item, int ticksLeft)
        {
            this.item = item;
            this.ticksLeft = ticksLeft;
        }

        public EatenFoodItem(EatenFoodItem eaten)
        {
            this.item = eaten.item;
            this.ticksLeft = eaten.ticksLeft;
        }
    }
}
