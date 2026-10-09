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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.entity.player.Player;
import vice.sol_valheim.accessors.LunchContainerAccessor;

import java.util.ArrayList;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.LinkedHashMap;
import java.util.List;

@Config(name = SOLValheim.MOD_ID)
@Config.Gui.Background("minecraft:textures/block/stone.png")
public class ModConfig extends PartitioningSerializer.GlobalData {

    public static Common.FoodConfig getFoodConfig(Item item) {
        var isDrink = item.getDefaultInstance().getUseAnimation() == UseAnim.DRINK;
        var properties = vice.sol_valheim.platform.Platform.foodProperties(item.getDefaultInstance(), null);
        if(item != Items.CAKE && properties == null && !isDrink)
            return null;

        var existing = SOLValheim.Config.common.foodConfigs.get(BuiltInRegistries.ITEM.getKey(item).toString());
        if (existing == null)
        {
            var registry = BuiltInRegistries.ITEM.getKey(item).toString();

            var food = item == Items.CAKE
                    ? GameVersion.makeFood(10, 0.7f)
                    : properties;

            if (isDrink) {
                if (registry.contains("potion")) {
                    food = GameVersion.makeFood(4, 0.75f);
                }
                else if (registry.contains("milk")) {
                    food = GameVersion.makeFood(6, 1f);
                }
                else {
                    food = GameVersion.makeFood(2, 0.5f);
                }
            }

            existing = new Common.FoodConfig();
            existing.nutrition = GameVersion.nutrition(food);
            existing.healthRegenModifier = 1f;
            existing.saturationModifier = (GameVersion.nutrition(food) == 0 ? 0f : GameVersion.saturation(food) / (GameVersion.nutrition(food) * 2f));

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

    public static Common.FoodConfig getFoodConfig(ItemStack stack, LivingEntity entity) {
        if (stack.isEmpty()) return null;
        if (stack.getItem() instanceof LunchContainerAccessor container && entity instanceof Player player)
            return getContainerFoodConfig(stack, container.sol_valheim$getActualFood(player, stack), entity);
        var actual = vice.sol_valheim.platform.Platform.foodProperties(stack, entity);
        boolean drink = stack.getUseAnimation() == UseAnim.DRINK;
        if (actual == null && !drink && !stack.is(Items.CAKE)) return null;
        var base = getFoodConfig(stack.getItem());
        // Some items become edible only after receiving data components.
        if (base == null) base = new Common.FoodConfig();
        if (actual == null || !base.useStackFoodValues) return base;
        var defaults = vice.sol_valheim.platform.Platform.foodProperties(stack.getItem().getDefaultInstance(), entity);
        if (defaults != null && GameVersion.nutrition(actual) == GameVersion.nutrition(defaults)
                && Float.compare(GameVersion.saturation(actual), GameVersion.saturation(defaults)) == 0) return base;

        var evaluated = base.copy();
        float actualModifier = GameVersion.nutrition(actual) == 0 ? 0f : GameVersion.saturation(actual) / (GameVersion.nutrition(actual) * 2f);
        float defaultModifier = defaults == null || GameVersion.nutrition(defaults) == 0
                ? 0f : GameVersion.saturation(defaults) / (GameVersion.nutrition(defaults) * 2f);
        // Preserve configured scaling while applying the actual stack's food values.
        evaluated.nutrition = Mth.clamp(defaults != null && GameVersion.nutrition(defaults) > 0
                ? Math.round(GameVersion.nutrition(actual) * ((float) base.nutrition / GameVersion.nutrition(defaults)))
                : GameVersion.nutrition(actual), 0, 1000);
        evaluated.saturationModifier = finite(defaultModifier > 0
                ? actualModifier * base.saturationModifier / defaultModifier : actualModifier, 1f, 0f, 100f);
        return evaluated;
    }

    public static Common.FoodConfig getContainerFoodConfig(ItemStack container, ItemStack consumed, LivingEntity entity) {
        if (consumed.isEmpty()) return null;
        var selected = getFoodConfig(consumed, entity);
        if (selected == null) return null;
        var base = getFoodConfig(container.getItem());
        if (base == null) return null;
        if (!base.useStackFoodValues) return base;
        // Store a snapshot of the eaten food's values, with the container as slot identity.
        var evaluated = selected.copy();
        evaluated.healthRegenModifier = finite(selected.healthRegenModifier * base.healthRegenModifier, 1f, 0f, 100f);
        evaluated.extraEffects.addAll(base.extraEffects);
        return evaluated;
    }

    public void validate() {
        if (common == null) common = new Common();
        if (client == null) client = new Client();
        client.hudPositionX = finite(client.hudPositionX, 0.5f, 0f, 1f);
        client.hudPositionY = finite(client.hudPositionY, 1f, 0f, 1f);
        common.maxSlots = Mth.clamp(common.maxSlots, 2, 5);
        common.maxHealth = Mth.clamp(common.maxHealth, 1, 1000);
        common.startingHealth = Mth.clamp(common.startingHealth, 1, Math.min(20, common.maxHealth));
        common.defaultTimer = Mth.clamp(common.defaultTimer, 1, 86400);
        common.regenDelay = Math.max(0, common.regenDelay);
        common.respawnGracePeriod = Math.max(0, common.respawnGracePeriod);
        common.regenSpeedModifier = finite(common.regenSpeedModifier, 1f, 0.01f, 100f);
        common.speedBoost = finite(common.speedBoost, 0.2f, 0f, 10f);
        common.eatAgainPercentage = finite(common.eatAgainPercentage, 0.2f, 0f, 1f);
        common.drinkSlotFoodEffectivenessBonus = finite(common.drinkSlotFoodEffectivenessBonus, 0.1f, 0f, 10f);
        common.hungerSecondsPerPoint = finite(common.hungerSecondsPerPoint, 60f, 0f, 86400f);
        common.exhaustionSecondsPerPoint = finite(common.exhaustionSecondsPerPoint, 10f, 0f, 86400f);
        common.hungerConsumptionExponent = finite(common.hungerConsumptionExponent, 0.5f, 0f, 2f);
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
        public boolean keepFoodOnDeath = false;

        @ConfigEntry.Gui.Tooltip()
        public float speedBoost = 0.20f;

        @ConfigEntry.Gui.Tooltip()
        public int startingHealth = 3;

        @ConfigEntry.Gui.Tooltip()
        public int maxHealth = 30;

        @ConfigEntry.Gui.Tooltip()
        public int maxSlots = 3;

        @ConfigEntry.Gui.Tooltip()
        public float eatAgainPercentage = 0.2F;

        @ConfigEntry.Gui.Tooltip()
        public float drinkSlotFoodEffectivenessBonus = 0.10F;

        @ConfigEntry.Gui.Tooltip()
        public boolean passTicksDuringNight = true;

        @ConfigEntry.Gui.Tooltip()
        public boolean convertHungerCosts = true;

        @ConfigEntry.Gui.Tooltip()
        public float hungerSecondsPerPoint = 60f;

        @ConfigEntry.Gui.Tooltip()
        public float exhaustionSecondsPerPoint = 10f;

        @ConfigEntry.Gui.Tooltip()
        public float hungerConsumptionExponent = 0.5f;

        @ConfigEntry.Gui.Tooltip()
        public boolean hungerConsumesDrinks = true;

        @ConfigEntry.Gui.Tooltip(count = 5)
        @ConfigEntry.Gui.Excluded
        public Map<String, FoodConfig> foodConfigs = new LinkedHashMap<>();

        public static final class FoodConfig implements ConfigData {
            public int nutrition;
            public float saturationModifier = 1f;
            public float healthRegenModifier = 1f;
            public boolean useStackFoodValues = true;
            public List<MobEffectConfig> extraEffects = new ArrayList<>();

            public FoodConfig copy() {
                var copy = new FoodConfig();
                copy.nutrition = nutrition;
                copy.saturationModifier = saturationModifier;
                copy.healthRegenModifier = healthRegenModifier;
                copy.useStackFoodValues = useStackFoodValues;
                copy.extraEffects = new ArrayList<>(extraEffects);
                return copy;
            }

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
                return id == null ? null : BuiltInRegistries.MOB_EFFECT.getHolder(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.MOB_EFFECT, id)).orElse(null);
            }
        }

    }

    @Config(name = "client")
    public static final class Client implements ConfigData {
        @ConfigEntry.Gui.Tooltip

        public boolean useLargeIcons = true;

        @ConfigEntry.Gui.Tooltip
        public boolean useProgressBar = true;

        @ConfigEntry.Gui.Tooltip
        public boolean customHudPosition = false;

        @ConfigEntry.Gui.Excluded
        public float hudPositionX = 0.5f;

        @ConfigEntry.Gui.Excluded
        public float hudPositionY = 1f;
    }
}
