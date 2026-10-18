package vice.sol_valheim;

import com.google.gson.Gson;
import com.mojang.authlib.GameProfile;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import vice.sol_valheim.accessors.PlayerEntityMixinDataAccessor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FoodExclusionChecks {
    @TempDir Path directory;
    private Map<TagKey<Item>, List<Holder<Item>>> originalTags;

    @BeforeEach void setup() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        SOLValheim.Config = new ModConfig();
        originalTags = new HashMap<>();
        BuiltInRegistries.ITEM.getTags().forEach(pair -> originalTags.put(pair.getFirst(), pair.getSecond().stream().toList()));
    }

    @AfterEach void restoreTags() {
        BuiltInRegistries.ITEM.resetTags();
        BuiltInRegistries.ITEM.bindTags(originalTags);
        SOLValheim.Config = new ModConfig();
    }

    @Test void defaultsKeepExistingFoodAndNonFoodDrinks() {
        assertTrue(SOLValheim.Config.common.foodBlacklist.isEmpty());
        assertNull(vice.sol_valheim.platform.Platform.foodProperties(Items.POTION.getDefaultInstance(), null));
        assertEquals(4, ModConfig.getFoodConfig(Items.POTION).nutrition);
        var data = new ValheimFoodData();
        assertTrue(data.eatItem(Items.BREAD));
        assertTrue(data.eatItem(Items.POTION));
        assertNotNull(data.DrinkSlot);
    }

    @Test void blacklistOverridesGeneratedValuesAndCachedConfigsWithoutDeletingOverrides() {
        var potion = Items.POTION.getDefaultInstance();
        var original = ModConfig.getFoodConfig(Items.POTION);
        SOLValheim.Config.common.foodBlacklist.add("minecraft:potion");
        assertNull(ModConfig.getFoodConfig(Items.POTION));
        assertNull(ModConfig.getFoodConfig(potion, null));
        var data = new ValheimFoodData();
        assertFalse(data.canEat(Items.POTION));
        assertFalse(data.eatConfiguredItem(potion, original));
        assertFalse(data.hasFood());
        var tooltip = new ArrayList<Component>();
        SOLValheim.addTooltip(potion, tooltip);
        assertTrue(tooltip.isEmpty());
        assertSame(original, SOLValheim.Config.common.foodConfigs.get("minecraft:potion"));
        SOLValheim.Config.common.foodBlacklist.clear();
        assertSame(original, ModConfig.getFoodConfig(Items.POTION));
    }

    @Test void excludedItemsRetainTheirOriginalUseAndFoodProperties() throws Exception {
        var player = player();
        var bread = Items.BREAD.getDefaultInstance();
        var properties = vice.sol_valheim.platform.Platform.foodProperties(bread, player);
        player.setItemInHand(InteractionHand.MAIN_HAND, bread);
        assertEquals(InteractionResult.CONSUME, bread.use(player.level(), player, InteractionHand.MAIN_HAND).getResult());
        player.stopUsingItem();
        SOLValheim.Config.common.foodBlacklist.add("minecraft:bread");
        assertEquals(InteractionResult.FAIL, bread.use(player.level(), player, InteractionHand.MAIN_HAND).getResult());
        assertSame(properties, vice.sol_valheim.platform.Platform.foodProperties(bread, player));

        SOLValheim.Config.common.foodBlacklist.add("minecraft:milk_bucket");
        var milk = Items.MILK_BUCKET.getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND, milk);
        assertEquals(InteractionResult.CONSUME, milk.use(player.level(), player, InteractionHand.MAIN_HAND).getResult());
        var result = milk.finishUsingItem(player.level(), player);
        assertTrue(result.is(Items.BUCKET));
        SOLValheim.consume(player, Items.MILK_BUCKET.getDefaultInstance());
        assertFalse(data(player).hasFood());
    }

    @Test void exclusionsAlsoDisableRottenFleshSpecialHandling() throws Exception {
        var player = player();
        assertTrue(data(player).eatItem(Items.BREAD));
        SOLValheim.Config.common.foodBlacklist.add("minecraft:rotten_flesh");
        SOLValheim.consume(player, Items.ROTTEN_FLESH.getDefaultInstance());
        assertTrue(data(player).hasFood());
        var tooltip = new ArrayList<Component>();
        SOLValheim.addTooltip(Items.ROTTEN_FLESH.getDefaultInstance(), tooltip);
        assertTrue(tooltip.isEmpty());
    }

    @Test void dynamicAndLegacySavedValuesCannotRestoreExcludedFoodsOrDrinks() {
        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var data = new ValheimFoodData();
        var rich = Items.BREAD.getDefaultInstance();
        rich.set(DataComponents.FOOD, new FoodProperties.Builder().nutrition(18).saturationModifier(0.8f).build());
        assertTrue(data.eatItem(rich, null));
        assertTrue(data.eatItem(Items.POTION));
        var saved = data.save(new CompoundTag(), registries);
        SOLValheim.Config.common.foodBlacklist.addAll(List.of("minecraft:bread", "minecraft:potion"));
        assertEquals(0f, data.getTotalFoodNutrition());
        assertEquals(0.25f, data.getRegenSpeed());
        assertNull(ModConfig.getFoodConfig(rich, null));
        assertFalse(ValheimFoodData.read(saved, registries).hasFood());
        saved.remove("stack0");
        saved.remove("drinkstack");
        assertFalse(ValheimFoodData.read(saved, registries).hasFood());
        assertTrue(data.removeExcludedFoods());
        assertTrue(data.ItemEntries.isEmpty());
        assertNull(data.DrinkSlot);
    }

    @Test void componentOnlyFoodCannotBypassTheBlacklist() {
        var stack = Items.STICK.getDefaultInstance();
        stack.set(DataComponents.FOOD, new FoodProperties.Builder().nutrition(8).build());
        assertNotNull(ModConfig.getFoodConfig(stack, null));
        SOLValheim.Config.common.foodBlacklist.add("minecraft:stick");
        assertNull(ModConfig.getFoodConfig(stack, null));
        assertFalse(new ValheimFoodData().eatItem(stack, null));
    }

    @Test void containerSnapshotsRetainTheConsumedFoodsIdentity() {
        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var container = Items.APPLE.getDefaultInstance();
        var consumed = Items.BREAD.getDefaultInstance();
        for (boolean dynamic : new boolean[]{true, false}) {
            SOLValheim.Config.common.foodBlacklist.clear();
            ModConfig.getFoodConfig(Items.APPLE).useStackFoodValues = dynamic;
            var snapshot = ModConfig.getContainerFoodConfig(container, consumed, null);
            var data = new ValheimFoodData();
            assertTrue(data.eatConfiguredItem(container, snapshot));
            var saved = data.save(new CompoundTag(), registries);
            var restored = ValheimFoodData.read(saved, registries);
            assertTrue(restored.hasFood());
            SOLValheim.Config.common.foodBlacklist.add("minecraft:bread");
            assertFalse(data.eatConfiguredItem(container, snapshot));
            assertNull(ModConfig.getContainerFoodConfig(container, consumed, null));
            assertTrue(restored.removeExcludedFoods());
            assertFalse(restored.hasFood());
            assertFalse(ValheimFoodData.read(saved, registries).hasFood());
        }
        SOLValheim.Config.common.foodBlacklist.clear();
        SOLValheim.Config.common.foodBlacklist.add("minecraft:apple");
        assertNull(ModConfig.getContainerFoodConfig(container, consumed, null));
    }

    @Test void tagMembershipChangesApplyWithoutRebuildingSelectors() {
        var tag = TagKey.create(Registries.ITEM, ResourceLocation.parse("example:ingredients"));
        SOLValheim.Config.common.foodBlacklist.add("#example:ingredients");
        assertFalse(FoodExclusions.isExcluded(Items.BREAD));
        var data = new ValheimFoodData();
        assertTrue(data.eatItem(Items.BREAD));
        bindTag(tag, Items.BREAD);
        assertTrue(FoodExclusions.isExcluded(Items.BREAD));
        data.tick();
        assertFalse(data.hasFood());
        bindTag(tag);
        assertFalse(FoodExclusions.isExcluded(Items.BREAD));
        assertTrue(data.canEat(Items.BREAD));
        bindTag(FoodExclusions.EXCLUDED_FOODS, Items.POTION);
        assertTrue(FoodExclusions.isExcluded(Items.POTION));
        assertNull(ModConfig.getFoodConfig(Items.POTION));
    }

    @Test void configRoundTripsAndInvalidOrMissingSelectorsAreHarmless() throws Exception {
        SOLValheim.Config.common.foodBlacklist.addAll(java.util.Arrays.asList(
                null, "", "#", "BAD ID", "missing:oil", "#missing:ingredients", " minecraft:bread "));
        assertDoesNotThrow(SOLValheim.Config::validate);
        assertDoesNotThrow(FoodExclusions::warnUnknownItems);
        assertTrue(FoodExclusions.isExcluded(Items.BREAD));
        assertFalse(FoodExclusions.isExcluded(Items.APPLE));
        var serializer = new ReadableConfigSerializer<>(directory.resolve("common.json5"), ModConfig.Common.class);
        serializer.serialize(SOLValheim.Config.common);
        assertEquals(SOLValheim.Config.common.foodBlacklist, serializer.deserialize().foodBlacklist);
        var json = ConfigSync.encode();
        assertFalse(json.contains("exclusionRules"));
        assertEquals(SOLValheim.Config.common.foodBlacklist,
                new Gson().fromJson(json, ModConfig.Common.class).foodBlacklist);
        assertTrue(new Gson().fromJson("{}", ModConfig.Common.class).foodBlacklist.isEmpty());
        SOLValheim.Config.common.foodBlacklist = null;
        SOLValheim.Config.validate();
        assertTrue(SOLValheim.Config.common.foodBlacklist.isEmpty());
    }

    @Test void serverTickRemovesExcludedSlotsAndRecalculatesAttributes() throws Exception {
        var player = player();
        assertTrue(data(player).eatItem(Items.BREAD));
        assertTrue(data(player).eatItem(Items.POTION));
        PlayerLifecycle.updateAttributes(player);
        assertTrue(player.getMaxHealth() > 6f);
        SOLValheim.Config.common.foodBlacklist.addAll(List.of("minecraft:bread", "minecraft:potion"));
        PlayerLifecycle.tick(player);
        assertFalse(data(player).hasFood());
        assertEquals(6f, player.getMaxHealth());
    }

    private void bindTag(TagKey<Item> tag, Item... items) {
        var tags = new HashMap<>(originalTags);
        tags.put(tag, java.util.Arrays.stream(items).<Holder<Item>>map(Item::builtInRegistryHolder).toList());
        BuiltInRegistries.ITEM.resetTags();
        BuiltInRegistries.ITEM.bindTags(tags);
    }

    private static ValheimFoodData data(Player player) {
        return ((PlayerEntityMixinDataAccessor) player).sol_valheim$getFoodData();
    }

    private static TestPlayer player() throws Exception {
        var level = mock(Level.class);
        when(level.registryAccess()).thenReturn(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        var side = Level.class.getDeclaredField("isClientSide");
        side.setAccessible(true);
        side.setBoolean(level, false);
        return new TestPlayer(level);
    }

    private static class TestPlayer extends Player {
        TestPlayer(Level level) { super(level, BlockPos.ZERO, 0f, new GameProfile(UUID.randomUUID(), "ExclusionTest")); }
        @Override public boolean isSpectator() { return false; }
        @Override public boolean isCreative() { return false; }
    }
}
