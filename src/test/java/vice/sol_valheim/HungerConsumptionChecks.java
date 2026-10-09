package vice.sol_valheim;

import com.google.gson.Gson;
import com.mojang.authlib.GameProfile;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vice.sol_valheim.accessors.PlayerEntityMixinDataAccessor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HungerConsumptionChecks {
    @BeforeEach void setup() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        SOLValheim.Config = new ModConfig();
    }

    private static RegistryAccess registries() {
        return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    private static TestPlayer player(boolean client) throws Exception {
        var level = mock(Level.class);
        var clientField = Level.class.getDeclaredField("isClientSide");
        clientField.setAccessible(true);
        clientField.setBoolean(level, client);
        when(level.registryAccess()).thenReturn(registries());
        when(level.getDifficulty()).thenReturn(Difficulty.NORMAL);
        var rules = mock(GameRules.class);
        when(rules.getBoolean(GameRules.RULE_NATURAL_REGENERATION)).thenReturn(true);
        when(level.getGameRules()).thenReturn(rules);
        return new TestPlayer(level);
    }

    private static ValheimFoodData food(Player player) {
        return ((PlayerEntityMixinDataAccessor) player).sol_valheim$getFoodData();
    }

    @Test void squareRootCurveSharesTheBudgetAndProtectsShortTimers() {
        var data = new ValheimFoodData();
        for (var item : new net.minecraft.world.item.Item[]{Items.BREAD, Items.APPLE, Items.CARROT}) {
            var config = ModConfig.getFoodConfig(item);
            config.nutrition = 10;
            config.saturationModifier = 1;
            assertTrue(data.eatItem(item));
        }
        var first = data.ItemEntries.get(0);
        var second = data.ItemEntries.get(1);
        var third = data.ItemEntries.get(2);
        assertEquals(36000, first.ticksLeft);
        second.ticksLeft = 9000;
        third.ticksLeft = 1440;
        data.queueHungerCost(60);
        data.applyHungerCosts();
        assertEquals(35600, first.ticksLeft); // 20 seconds
        assertEquals(8800, second.ticksLeft); // 10 seconds
        assertEquals(1360, third.ticksLeft); // 4 seconds
    }

    @Test void realFoodLevelApiChargesEveryDeductionWithoutWaitingForATick() throws Exception {
        var player = player(false);
        var data = food(player);
        data.eatItem(Items.BREAD);
        int before = data.ItemEntries.getFirst().ticksLeft;
        player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);
        assertEquals(20, player.getFoodData().getFoodLevel());
        player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);
        data.applyHungerCosts();
        assertEquals(before - 2400, data.ItemEntries.getFirst().ticksLeft);
        player.getFoodData().tick(player);
        data.applyHungerCosts();
        assertEquals(before - 2400, data.ItemEntries.getFirst().ticksLeft); // no double charge
    }

    @Test void exhaustionApisUseTheirOwnRateAndPreserveLargeRequests() throws Exception {
        var player = player(false);
        var data = food(player);
        data.eatItem(Items.BREAD);
        int before = data.ItemEntries.getFirst().ticksLeft;
        player.getFoodData().addExhaustion(4);
        player.getFoodData().setExhaustion(4);
        player.getFoodData().addExhaustion(80); // beyond the vanilla buffer
        assertEquals(0, player.getFoodData().getExhaustionLevel());
        data.applyHungerCosts();
        assertEquals(before - 4400, data.ItemEntries.getFirst().ticksLeft);
        player.getFoodData().tick(player);
        data.applyHungerCosts();
        assertEquals(before - 4400, data.ItemEntries.getFirst().ticksLeft);
    }

    @Test void rawFieldChangesAreCapturedOnceIncludingWhenFollowedByAnApiCall() throws Exception {
        var player = player(false);
        var data = food(player);
        data.eatItem(Items.BREAD);
        int before = data.ItemEntries.getFirst().ticksLeft;
        var foodField = FoodData.class.getDeclaredField("foodLevel");
        var exhaustionField = FoodData.class.getDeclaredField("exhaustionLevel");
        foodField.setAccessible(true);
        exhaustionField.setAccessible(true);
        foodField.setInt(player.getFoodData(), 19);
        exhaustionField.setFloat(player.getFoodData(), 4);
        player.getFoodData().tick(player);
        player.getFoodData().tick(player);
        data.applyHungerCosts();
        assertEquals(before - 1400, data.ItemEntries.getFirst().ticksLeft);

        data.clear();
        data.eatItem(Items.BREAD);
        foodField.setInt(player.getFoodData(), 19);
        exhaustionField.setFloat(player.getFoodData(), 4);
        player.getFoodData().setFoodLevel(18);
        player.getFoodData().setExhaustion(8);
        data.applyHungerCosts();
        assertEquals(before - 2800, data.ItemEntries.getFirst().ticksLeft);
    }

    @Test void loadingVanillaHungerNbtDoesNotChargeOldCosts() throws Exception {
        var player = player(false);
        var data = food(player);
        data.eatItem(Items.BREAD);
        int before = data.ItemEntries.getFirst().ticksLeft;
        var old = new CompoundTag();
        old.putInt("foodLevel", 1);
        old.putInt("foodTickTimer", 79);
        old.putFloat("foodSaturationLevel", 5);
        old.putFloat("foodExhaustionLevel", 40);
        player.getFoodData().readAdditionalSaveData(old);
        player.getFoodData().tick(player);
        data.applyHungerCosts();
        assertEquals(before, data.ItemEntries.getFirst().ticksLeft);
        assertEquals(20, player.getFoodData().getFoodLevel());
        assertEquals(0, player.getFoodData().getExhaustionLevel());
        assertEquals(0, player.getFoodData().getSaturationLevel());
    }

    @Test void normalHungerTickStillRunsButDoesNotHealOrCallSetters() throws Exception {
        var player = player(false);
        var counting = new CountingFoodData();
        var field = Player.class.getDeclaredField("foodData");
        field.setAccessible(true);
        field.set(player, counting);
        HungerCompatibility.bind(player);
        player.setHealth(8);
        for (int i = 0; i < 200; i++) counting.tick(player);
        assertEquals(8, player.getHealth());
        assertEquals(0, counting.setterCalls);
        verify(player.level(), times(200)).getGameRules(); // vanilla method was not cancelled
    }

    @Test void fractionalCostsAndPendingCostsSurviveSaveAndReload() {
        var data = new ValheimFoodData();
        data.eatItem(Items.BREAD);
        int before = data.ItemEntries.getFirst().ticksLeft;
        data.queueHungerCost(0.0125); // one quarter of a tick
        data.applyHungerCosts();
        data.queueHungerCost(0.0375); // another three quarters, still pending at logout
        var saved = data.save(new CompoundTag(), registries());
        assertEquals(0.25, saved.getDouble("hunger_remainder0"));
        var restored = ValheimFoodData.read(saved, registries());
        restored.applyHungerCosts();
        assertEquals(before - 1, restored.ItemEntries.getFirst().ticksLeft);
        assertEquals(before, data.ItemEntries.getFirst().ticksLeft);
    }

    @Test void manySmallExhaustionCallsAccumulateInsteadOfRoundingAway() throws Exception {
        var player = player(false);
        var data = food(player);
        data.eatItem(Items.BREAD);
        int before = data.ItemEntries.getFirst().ticksLeft;
        for (int i = 0; i < 100; i++) player.getFoodData().addExhaustion(0.004f);
        data.applyHungerCosts();
        assertEquals(20, before - data.ItemEntries.getFirst().ticksLeft, 1);
    }

    @Test void costsBeforeEatingDoNotApplyToNewFoodAndClearDoesNotKeepDebt() {
        var data = new ValheimFoodData();
        data.queueHungerCost(60);
        data.eatItem(Items.BREAD);
        int before = data.ItemEntries.getFirst().ticksLeft;
        data.applyHungerCosts();
        assertEquals(before, data.ItemEntries.getFirst().ticksLeft);
        data.queueHungerCost(60);
        data.eatItem(Items.APPLE);
        assertEquals(before - 1200, data.getEatenFood(Items.BREAD).ticksLeft);
        assertEquals(ModConfig.getFoodConfig(Items.APPLE).getTime(), data.getEatenFood(Items.APPLE).ticksLeft);
        data.queueHungerCost(60);
        data.clear();
        data.eatItem(Items.BREAD);
        data.applyHungerCosts();
        assertEquals(before, data.ItemEntries.getFirst().ticksLeft);
    }

    @Test void emptyDrinkOnlyAndDisabledModesDiscardCosts() {
        SOLValheim.Config.common.hungerConsumesDrinks = false;
        var data = new ValheimFoodData();
        data.eatItem(Items.POTION);
        int drinkBefore = data.DrinkSlot.ticksLeft;
        data.queueHungerCost(60);
        data.eatItem(Items.BREAD);
        int foodBefore = data.ItemEntries.getFirst().ticksLeft;
        data.applyHungerCosts();
        assertEquals(foodBefore, data.ItemEntries.getFirst().ticksLeft);
        data.queueHungerCost(60);
        data.applyHungerCosts();
        assertEquals(foodBefore - 1200, data.ItemEntries.getFirst().ticksLeft);
        assertEquals(drinkBefore, data.DrinkSlot.ticksLeft);
        SOLValheim.Config.common.convertHungerCosts = false;
        data.queueHungerCost(60);
        data.applyHungerCosts();
        assertEquals(foodBefore - 1200, data.ItemEntries.getFirst().ticksLeft);
        data.tick();
        assertEquals(drinkBefore - 1, data.DrinkSlot.ticksLeft);
    }

    @Test void drinksParticipateAndExplicitCostsIgnoreNourishmentDiscount() {
        var data = new ValheimFoodData();
        data.eatItem(Items.BREAD);
        data.eatItem(Items.POTION);
        int before = data.ItemEntries.getFirst().ticksLeft;
        int drinkBefore = data.DrinkSlot.ticksLeft;
        data.queueHungerCost(60);
        data.tick(true); // first nourished tick does not consume ordinary time
        assertEquals(before - 600, data.ItemEntries.getFirst().ticksLeft);
        assertEquals(drinkBefore - 600, data.DrinkSlot.ticksLeft);
    }

    @Test void clientCreativeSpectatorAndDeadPlayersCannotSpendFood() throws Exception {
        for (int mode = 0; mode < 4; mode++) {
            var player = player(mode == 0);
            player.creative = mode == 1;
            player.spectator = mode == 2;
            if (mode == 3) player.setHealth(0);
            var data = food(player);
            data.eatItem(Items.BREAD);
            int before = data.ItemEntries.getFirst().ticksLeft;
            player.getFoodData().setFoodLevel(10);
            player.getFoodData().addExhaustion(40);
            data.applyHungerCosts();
            assertEquals(before, data.ItemEntries.getFirst().ticksLeft);
        }
    }

    @Test void expiringCostsRemoveFoodAndUpdateHealthAndDeathPenalties() throws Exception {
        SOLValheim.Config.common.hungerConsumptionExponent = 0;
        var original = player(false);
        food(original).eatItem(Items.BREAD);
        original.getFoodData().setFoodLevel(19);
        int full = food(original).ItemEntries.getFirst().ticksLeft;
        var target = player(false);
        PlayerLifecycle.clonePlayer(original, target, true);
        assertEquals((full - 1200) / 5, food(target).ItemEntries.getFirst().ticksLeft);
        food(target).queueHungerCost(86400);
        food(target).applyHungerCosts();
        PlayerLifecycle.updateAttributes(target);
        assertFalse(food(target).hasFood());
        assertEquals(6, target.getMaxHealth());
    }

    @Test void positiveValuesAndInvalidCostsCannotRefillOrCorruptTimers() throws Exception {
        var player = player(false);
        var data = food(player);
        data.eatItem(Items.BREAD);
        int before = data.ItemEntries.getFirst().ticksLeft;
        player.getFoodData().setFoodLevel(30);
        player.getFoodData().setSaturation(20);
        player.getFoodData().addExhaustion(-4);
        player.getFoodData().setExhaustion(Float.NaN);
        data.queueHungerCost(Double.POSITIVE_INFINITY);
        data.queueHungerCost(Double.NaN);
        data.queueHungerCost(-60);
        player.getFoodData().tick(player);
        data.applyHungerCosts();
        assertEquals(before, data.ItemEntries.getFirst().ticksLeft);
        var tag = data.save(new CompoundTag(), registries());
        tag.putDouble("pending_hunger_ticks", Double.NaN);
        tag.putDouble("hunger_remainder0", Double.POSITIVE_INFINITY);
        var restored = ValheimFoodData.read(tag, registries());
        restored.applyHungerCosts();
        assertEquals(before, restored.ItemEntries.getFirst().ticksLeft);
        assertEquals(0, restored.save(new CompoundTag(), registries()).getDouble("hunger_remainder0"));
    }

    @Test void oldConfigsGetDefaultsAndNewSettingsValidateAndRoundTrip() {
        var gson = new Gson();
        var common = gson.fromJson("{}", ModConfig.Common.class);
        assertTrue(common.convertHungerCosts);
        assertEquals(60, common.hungerSecondsPerPoint);
        assertEquals(10, common.exhaustionSecondsPerPoint);
        assertEquals(0.5f, common.hungerConsumptionExponent);
        assertTrue(common.hungerConsumesDrinks);
        common.hungerSecondsPerPoint = Float.NaN;
        common.exhaustionSecondsPerPoint = -1;
        common.hungerConsumptionExponent = 20;
        common.hungerConsumesDrinks = false;
        SOLValheim.Config.common = common;
        SOLValheim.Config.validate();
        assertEquals(60, common.hungerSecondsPerPoint);
        assertEquals(0, common.exhaustionSecondsPerPoint);
        assertEquals(2, common.hungerConsumptionExponent);
        assertEquals(gson.toJsonTree(common), gson.toJsonTree(gson.fromJson(gson.toJson(common), ModConfig.Common.class)));
    }

    private static class TestPlayer extends Player {
        boolean creative;
        boolean spectator;
        TestPlayer(Level level) {
            super(level, BlockPos.ZERO, 0f, new GameProfile(UUID.randomUUID(), "HungerTest"));
        }
        @Override public boolean isCreative() { return creative; }
        @Override public boolean isSpectator() { return spectator; }
    }

    private static class CountingFoodData extends FoodData {
        int setterCalls;
        @Override public void setFoodLevel(int value) { setterCalls++; super.setFoodLevel(value); }
        @Override public void setExhaustion(float value) { setterCalls++; super.setExhaustion(value); }
        @Override public void setSaturation(float value) { setterCalls++; super.setSaturation(value); }
    }
}
