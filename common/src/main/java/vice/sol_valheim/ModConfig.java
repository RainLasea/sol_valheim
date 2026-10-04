package vice.sol_valheim;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.PartitioningSerializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;

import java.util.ArrayList;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.LinkedHashMap;
import java.util.List;

@Config(name = SOLValheim.MOD_ID)
@Config.Gui.Background("minecraft:textures/block/stone.png")
public class ModConfig extends PartitioningSerializer.GlobalData {

    public static Common.FoodConfig getFoodConfig(Item item) {
        var isDrink = item.getDefaultInstance().getUseAnimation() == UseAnim.DRINK;
        if(item != Items.CAKE && !item.getDefaultInstance().has(DataComponents.FOOD) && !isDrink)
            return null;

        var existing = SOLValheim.Config.common.foodConfigs.get(BuiltInRegistries.ITEM.getKey(item).toString());
        if (existing == null)
        {
            var registry = BuiltInRegistries.ITEM.getKey(item).toString();

            var food = item == Items.CAKE
                    ? new FoodProperties.Builder().nutrition(10).saturationModifier(0.7f).build()
                    : item.getDefaultInstance().get(DataComponents.FOOD);

            if (isDrink) {
                if (registry.contains("potion")) {
                    food = new FoodProperties.Builder().nutrition(4).saturationModifier(0.75f).build();
                }
                else if (registry.contains("milk")) {
                    food = new FoodProperties.Builder().nutrition(6).saturationModifier(1f).build();
                }
                else {
                    food = new FoodProperties.Builder().nutrition(2).saturationModifier(0.5f).build();
                }
            }

            existing = new Common.FoodConfig();
            existing.nutrition = food.nutrition();
            existing.healthRegenModifier = 1f;
            existing.saturationModifier = (food.nutrition() == 0 ? 0f : food.saturation() / (food.nutrition() * 2f));

            if (registry.startsWith("farmers"))
            {
                existing.nutrition = (int) ((existing.nutrition * 1.25));
                existing.saturationModifier = existing.saturationModifier * 1.10f;
                existing.healthRegenModifier = 1.25f;
            }

            if (registry.equals("minecraft:golden_apple") || registry.equals("minecraft:enchanted_golden_apple")) {
                existing.nutrition = 10;
                existing.healthRegenModifier = 1.5f;
            }

            SOLValheim.Config.common.foodConfigs.put(BuiltInRegistries.ITEM.getKey(item).toString(), existing);
        }

        return existing;
    }

    public void validate() {
        if (common == null) common = new Common();
        if (client == null) client = new Client();
        common.maxSlots = Mth.clamp(common.maxSlots, 2, 5);
        common.startingHealth = Mth.clamp(common.startingHealth, 1, 20);
        common.defaultTimer = Mth.clamp(common.defaultTimer, 1, 86400);
        common.regenDelay = Math.max(0, common.regenDelay);
        common.respawnGracePeriod = Math.max(0, common.respawnGracePeriod);
        common.regenSpeedModifier = finite(common.regenSpeedModifier, 1f, 0.01f, 100f);
        common.speedBoost = finite(common.speedBoost, 0.2f, 0f, 10f);
        common.eatAgainPercentage = finite(common.eatAgainPercentage, 0.2f, 0f, 1f);
        common.drinkSlotFoodEffectivenessBonus = finite(common.drinkSlotFoodEffectivenessBonus, 0.1f, 0f, 10f);
        if (common.foodConfigs == null) common.foodConfigs = new LinkedHashMap<>();
        common.foodConfigs.values().removeIf(java.util.Objects::isNull);
        for (var food : common.foodConfigs.values()) {
            food.nutrition = Mth.clamp(food.nutrition, 0, 1000);
            food.saturationModifier = finite(food.saturationModifier, 1f, 0f, 100f);
            food.healthRegenModifier = finite(food.healthRegenModifier, 1f, 0f, 100f);
            if (food.extraEffects == null) food.extraEffects = new ArrayList<>();
            food.extraEffects.removeIf(java.util.Objects::isNull);
        }
    }

    private static float finite(float value, float fallback, float min, float max) {
        return Float.isFinite(value) ? Mth.clamp(value, min, max) : fallback;
    }

    @ConfigEntry.Category("common")
    @ConfigEntry.Gui.TransitiveObject()
    public Common common = new Common();

    @ConfigEntry.Category("client")
    @ConfigEntry.Gui.TransitiveObject()
    public Client client = new Client();

    @Config(name = "common")
    public static final class Common implements ConfigData {

        @ConfigEntry.Gui.Tooltip()
        public int defaultTimer = 180;

        @ConfigEntry.Gui.Tooltip()
        public float regenSpeedModifier = 1f;

        @ConfigEntry.Gui.Tooltip()
        public int regenDelay = 20 * 10;

        @ConfigEntry.Gui.Tooltip()
        public int respawnGracePeriod = 60 * 5;

        @ConfigEntry.Gui.Tooltip()
        public float speedBoost = 0.20f;

        @ConfigEntry.Gui.Tooltip()
        public int startingHealth = 3;

        @ConfigEntry.Gui.Tooltip()
        public int maxSlots = 3;

        @ConfigEntry.Gui.Tooltip()
        public float eatAgainPercentage = 0.2F;

        @ConfigEntry.Gui.Tooltip()
        public float drinkSlotFoodEffectivenessBonus = 0.10F;

        @ConfigEntry.Gui.Tooltip()
        public boolean passTicksDuringNight = true;

        @ConfigEntry.Gui.Tooltip(count = 5)
        @ConfigEntry.Gui.Excluded
        public Map<String, FoodConfig> foodConfigs = new LinkedHashMap<>();

        public static final class FoodConfig implements ConfigData {
            public int nutrition;
            public float saturationModifier = 1f;
            public float healthRegenModifier = 1f;
            public List<MobEffectConfig> extraEffects = new ArrayList<>();

            public int getTime() {
                var time = (int) Math.min(Integer.MAX_VALUE, SOLValheim.Config.common.defaultTimer * 20.0 * saturationModifier * nutrition);
                return Math.max(time, 6000);
            }

            public int getHearts() {
                return Math.max(nutrition, 2);
            }

            public float getHealthRegen()
            {
                return Mth.clamp(nutrition * 0.10f * healthRegenModifier, 0.25f, 2f);
            }
        }

        public static final class MobEffectConfig implements ConfigData {
            @ConfigEntry.Gui.Tooltip()
            public String ID;

            @ConfigEntry.Gui.Tooltip()
            public float duration = 1f;

            @ConfigEntry.Gui.Tooltip()
            public int amplifier = 1;

            public Holder<MobEffect> getEffect() {
                var id = ID == null ? null : ResourceLocation.tryParse(ID);
                return id == null ? null : BuiltInRegistries.MOB_EFFECT.getHolder(id).orElse(null);
            }
        }

    }

    @Config(name = "client")
    public static final class Client implements ConfigData {
        @ConfigEntry.Gui.Tooltip

        public boolean useLargeIcons = true;
    }
}
