package vice.sol_valheim;

import com.google.gson.Gson;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import vice.sol_valheim.client.HudLayout;
import org.junit.jupiter.api.Test;

/** Regression checks run inside the NeoForge unit-test environment. */
public final class CompatibilityChecks {
    @Test
    public void compatibility() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        SOLValheim.Config = new ModConfig();
        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

        var bread = new ItemStack(Items.BREAD);
        var base = ModConfig.getFoodConfig(Items.BREAD);
        check(ModConfig.getFoodConfig(bread, null) == base, "ordinary foods retain item overrides");
        var rich = bread.copy();
        rich.set(DataComponents.FOOD, new FoodProperties.Builder().nutrition(12).saturationModifier(0.9f).build());
        var richConfig = ModConfig.getFoodConfig(rich, null);
        check(richConfig.nutrition == 12, "stack nutrition is used");
        near(richConfig.saturationModifier, 0.9f, "stack saturation is used");
        check(base.nutrition == 5, "evaluating a stack does not overwrite the item config");

        base.nutrition = 10;
        check(ModConfig.getFoodConfig(rich, null).nutrition == 24, "configured scaling is preserved");
        base.useStackFoodValues = false;
        check(ModConfig.getFoodConfig(rich, null) == base, "explicit static overrides can disable stack values");
        base.nutrition = 5;
        base.useStackFoodValues = true;

        var data = new ValheimFoodData();
        check(data.eatItem(rich, null), "dynamic food can be eaten");
        check(!data.canEat(rich, null), "the same recipe cannot be stacked immediately");
        var renamed = rich.copy();
        renamed.set(DataComponents.CUSTOM_NAME, Component.literal("Renamed sandwich"));
        check(!data.canEat(renamed, null), "renaming does not bypass the recipe restriction");
        var otherRecipe = bread.copy();
        otherRecipe.set(DataComponents.FOOD, new FoodProperties.Builder().nutrition(8).saturationModifier(0.4f).build());
        check(data.eatItem(otherRecipe, null), "different stack recipes can occupy separate slots");
        check(data.ItemEntries.size() == 2, "both recipes are retained");
        check(data.getTotalFoodNutrition() == 20, "bonuses use both actual recipes");
        rich.set(DataComponents.FOOD, new FoodProperties.Builder().nutrition(1).build());
        check(data.ItemEntries.getFirst().getConfig().nutrition == 12, "the consumed stack is a snapshot");

        var saved = data.save(new CompoundTag(), registries);
        var restored = ValheimFoodData.read(saved, registries);
        check(restored.ItemEntries.size() == 2, "same-item variants survive save and load");
        check(restored.getTotalFoodNutrition() == 20, "dynamic bonuses survive save and load");
        near(restored.ItemEntries.getFirst().getConfig().saturationModifier, 0.9f, "dynamic saturation survives save and load");
        check(ItemStack.isSameItemSameComponents(data.ItemEntries.getFirst().stack, restored.ItemEntries.getFirst().stack),
                "full item components survive save and load");
        check(!restored.canEat(renamed, null), "restored variants still enforce the cooldown");

        var legacy = new CompoundTag();
        legacy.putInt("count", 1);
        legacy.putString("id0", "minecraft:bread");
        legacy.putInt("ticks0", 2400);
        legacy.putString("drink", "minecraft:potion");
        legacy.putInt("drinkticks", 1800);
        var migrated = ValheimFoodData.read(legacy, registries);
        check(migrated.ItemEntries.size() == 1 && migrated.ItemEntries.getFirst().ticksLeft == 2400,
                "legacy food IDs and timers migrate");
        check(migrated.DrinkSlot != null && migrated.DrinkSlot.ticksLeft == 1800, "legacy drinks migrate");

        var componentFood = new ItemStack(Items.STICK);
        componentFood.set(DataComponents.FOOD, new FoodProperties.Builder().nutrition(7).saturationModifier(0.5f).build());
        var componentData = new ValheimFoodData();
        check(componentData.eatItem(componentFood, null), "items edible only through components are supported");
        check(ValheimFoodData.read(componentData.save(new CompoundTag(), registries), registries).getTotalFoodNutrition() == 7,
                "component-only foods survive save and load");

        var oldClient = new Gson().fromJson("{\"useLargeIcons\":true}", ModConfig.Client.class);
        check(oldClient.useProgressBar && !oldClient.customHudPosition, "older configs retain sensible defaults");
        for (boolean large : new boolean[]{false, true}) {
            for (boolean bars : new boolean[]{false, true}) {
                for (int slots = 3; slots <= 6; slots++) {
                    for (int[] resolution : new int[][]{{320, 180}, {854, 480}, {1920, 1080}}) {
                        int w = resolution[0], h = resolution[1];
                        var corner = HudLayout.create(w, h, 39, slots, large, bars, true, 1, 1);
                        check(corner.x() + corner.width() == w && corner.y() + corner.height() == h,
                                "bottom-right placement remains anchored across sizes");
                        var center = HudLayout.create(w, h, 39, slots, large, bars, true, 0.5f, 0.5f);
                        check(Math.abs(center.x() + center.width() / 2.0 - w / 2.0) <= 0.5,
                                "horizontal center remains centered");
                        check(center.slotX(slots - 1) == center.x(), "slot bounds match the drag preview");
                    }
                }
            }
        }
        System.out.println("Compatibility checks passed: dynamic food values, variant identity, snapshots, persistence, legacy saves, config defaults, and HUD placement.");
    }

    private static void near(float actual, float expected, String message) {
        check(Math.abs(actual - expected) < 0.0001f, message);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
