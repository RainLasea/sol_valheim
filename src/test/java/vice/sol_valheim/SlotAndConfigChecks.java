package vice.sol_valheim;

import com.google.gson.Gson;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SlotAndConfigChecks {
    @TempDir Path directory;

    @BeforeEach void setup() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        SOLValheim.Config = new ModConfig();
    }

    @Test void drinksShareTheLimitInEitherConsumptionOrder() {
        var data = new ValheimFoodData();
        assertTrue(data.eatItem(Items.BREAD));
        assertTrue(data.eatItem(Items.APPLE));
        assertTrue(data.eatItem(Items.CARROT));
        assertFalse(data.canEat(Items.POTION));
        assertFalse(data.eatItem(Items.POTION));
        data.ItemEntries.getFirst().ticksLeft = 500;
        assertTrue(data.canEat(Items.POTION));
        assertTrue(data.eatItem(Items.POTION));
        assertEquals(2, data.ItemEntries.size());
        assertNotNull(data.DrinkSlot);

        data.clear();
        assertTrue(data.eatItem(Items.POTION));
        assertTrue(data.eatItem(Items.BREAD));
        assertTrue(data.eatItem(Items.APPLE));
        assertFalse(data.canEat(Items.CARROT));
        assertFalse(data.eatItem(Items.CARROT));
        assertFalse(data.eatItem(Items.MILK_BUCKET));
        data.DrinkSlot.ticksLeft = 500;
        assertTrue(data.canEat(Items.CARROT));
        assertTrue(data.eatItem(Items.CARROT));
        assertNull(data.DrinkSlot);
        assertEquals(3, data.ItemEntries.size());
    }

    @Test void oldSavesAndReducedLimitsCannotRetainExtraSlots() {
        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var old = new CompoundTag();
        old.putInt("count", 3);
        for (int i = 0; i < 3; i++) {
            old.putString("id" + i, new String[]{"minecraft:bread", "minecraft:apple", "minecraft:carrot"}[i]);
            old.putInt("ticks" + i, 2400);
        }
        old.putString("drink", "minecraft:potion");
        old.putInt("drinkticks", 1800);
        var data = ValheimFoodData.read(old, registries);
        assertEquals(2, data.ItemEntries.size());
        assertNotNull(data.DrinkSlot);
        data.setMaxSlots(2);
        assertEquals(1, data.ItemEntries.size());
        var restored = ValheimFoodData.read(data.save(new CompoundTag(), registries), registries);
        assertEquals(1, restored.ItemEntries.size());
        assertNotNull(restored.DrinkSlot);
    }

    @Test void deathPenaltyUsesCurrentTimersAndStrictFullDurationThreshold() {
        for (var item : new net.minecraft.world.item.Item[]{Items.BREAD, Items.POTION}) {
            // A duration not divisible by five also exercises exact threshold comparison.
            var config = new ModConfig.Common.FoodConfig();
            config.nutrition = 2;
            config.saturationModifier = 1.001f;
            int duration = config.getTime();
            int boundary = (duration + 4) / 5;
            for (int ticks : new int[]{0, boundary - 1, boundary, duration / 2 + 3, duration}) {
                var data = new ValheimFoodData();
                var entry = new ValheimFoodData.EatenFoodItem(item.getDefaultInstance(), ticks, config);
                if (item == Items.POTION) data.DrinkSlot = entry;
                else data.ItemEntries.add(entry);
                data.applyDeathPenalty();
                if ((long) ticks * 5 < duration) {
                    assertFalse(data.hasFood());
                } else {
                    assertTrue(data.hasFood());
                    assertEquals(ticks / 5, entry.ticksLeft);
                }
            }
        }
    }

    @Test void reEatingSupportsHalfDurationAndStillHonorsStrictThresholds() {
        SOLValheim.Config.common.eatAgainPercentage = 0.5f;
        for (var item : new net.minecraft.world.item.Item[]{Items.BREAD, Items.POTION}) {
            var data = new ValheimFoodData();
            assertTrue(data.eatItem(item));
            var entry = item == Items.POTION ? data.DrinkSlot : data.ItemEntries.getFirst();
            int duration = entry.getConfig().getTime();
            entry.ticksLeft = duration / 2;
            assertFalse(data.canEat(item));
            assertFalse(data.eatItem(item));
            entry.ticksLeft--;
            assertTrue(data.canEat(item));
            assertTrue(data.eatItem(item));
            assertEquals(duration, entry.ticksLeft);
            assertEquals(1, data.ItemEntries.size() + (data.DrinkSlot == null ? 0 : 1));

            SOLValheim.Config.common.eatAgainPercentage = 0.2f;
            entry.ticksLeft = duration / 2 - 1;
            assertFalse(data.canEat(item));
            entry.ticksLeft = 1199;
            assertTrue(data.canEat(item));
            SOLValheim.Config.common.eatAgainPercentage = 0.5f;
        }
    }

    @Test void keepFoodOnDeathRetainsLowTimersAndNourishmentProgress() {
        SOLValheim.Config.common.keepFoodOnDeath = true;
        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var data = new ValheimFoodData();
        assertTrue(data.eatItem(Items.BREAD));
        assertTrue(data.eatItem(Items.POTION));
        data.ItemEntries.getFirst().ticksLeft = 101;
        data.DrinkSlot.ticksLeft = 201;
        data.tick(true); // Accumulate a partial nourishment tick.
        var before = data.save(new CompoundTag(), registries);
        data.applyDeathPenalty();
        assertEquals(before, data.save(new CompoundTag(), registries));
        SOLValheim.Config.common.keepFoodOnDeath = false;
        data.applyDeathPenalty();
        assertFalse(data.hasFood());
    }

    @Test void commonSettingsAndFoodOverridesLoadWithoutCommentsOrFixedOrder() throws Exception {
        var path = directory.resolve("common.json5");
        var serializer = new ReadableConfigSerializer<>(path, ModConfig.Common.class);
        var text = """
                {
                  // Deliberately use a different order and short decimals.
                  foodConfigs: {
                    'minecraft:bread': { healthRegenModifier: 1.5, nutrition: 9,
                      extraEffects: [{ ID: 'minecraft:speed', amplifier: 2, duration: 0.5 }],
                      saturationModifier: 0.7, useStackFoodValues: false, },
                  },
                  maxSlots: 4, startingHealth: 5, maxHealth: 35, defaultTimer: 240,
                  regenSpeedModifier: 0.5, regenDelay: 60, respawnGracePeriod: 120,
                  speedBoost: 0.3, eatAgainPercentage: 0.25, keepFoodOnDeath: true,
                  drinkSlotFoodEffectivenessBonus: 0.15, passTicksDuringNight: false,
                }
                """;
        Files.writeString(path, text);
        var common = serializer.deserialize();
        assertEquals(4, common.maxSlots);
        assertEquals(5, common.startingHealth);
        assertEquals(35, common.maxHealth);
        assertEquals(240, common.defaultTimer);
        assertEquals(0.5f, common.regenSpeedModifier);
        assertEquals(60, common.regenDelay);
        assertEquals(120, common.respawnGracePeriod);
        assertEquals(0.3f, common.speedBoost);
        assertEquals(0.25f, common.eatAgainPercentage);
        assertTrue(common.keepFoodOnDeath);
        assertEquals(0.15f, common.drinkSlotFoodEffectivenessBonus);
        assertFalse(common.passTicksDuringNight);
        SOLValheim.Config.common = common;
        var bread = ModConfig.getFoodConfig(Items.BREAD);
        assertEquals(9, bread.nutrition);
        assertEquals(0.7f, bread.saturationModifier);
        assertEquals(1.5f, bread.healthRegenModifier);
        assertFalse(bread.useStackFoodValues);
        assertEquals("minecraft:speed", bread.extraEffects.getFirst().ID);
        assertEquals(2, bread.extraEffects.getFirst().amplifier);
        assertEquals(0.5f, bread.extraEffects.getFirst().duration);
        assertEquals(30240, bread.getTime(), 1); // Timer conversion truncates a fractional tick.

        Files.writeString(path, text.replace("// Deliberately use a different order and short decimals.", ""));
        var gson = new Gson();
        assertEquals(gson.toJsonTree(common), gson.toJsonTree(serializer.deserialize()));
        serializer.serialize(common);
        assertTrue(Files.readString(path).contains("\"speedBoost\": 0.3"));
        assertEquals(gson.toJsonTree(common), gson.toJsonTree(serializer.deserialize()));
        Files.writeString(path, "{ startingHealth: 7 }");
        var partial = serializer.deserialize();
        assertEquals(7, partial.startingHealth);
        assertEquals(3, partial.maxSlots);
        assertEquals(30, partial.maxHealth);
        assertEquals(180, partial.defaultTimer);
        assertEquals(1f, partial.regenSpeedModifier);
        assertFalse(partial.keepFoodOnDeath);
        Files.writeString(path, "{ startingHealth: ");
        assertThrows(me.shedaniel.autoconfig.serializer.ConfigSerializer.SerializationException.class, serializer::deserialize);
    }

    @Test void maximumHealthValidationKeepsStartingHeartsWithinTheCap() {
        SOLValheim.Config.common.maxHealth = 0;
        SOLValheim.Config.validate();
        assertEquals(1, SOLValheim.Config.common.maxHealth);
        assertEquals(1, SOLValheim.Config.common.startingHealth);
    }

    @Test void clientSettingsRoundTripAndRetainDefaults() throws Exception {
        var path = directory.resolve("client.json5");
        var serializer = new ReadableConfigSerializer<>(path, ModConfig.Client.class);
        Files.writeString(path, "{ hudPositionY: 0.4, customHudPosition: true, hudPositionX: 0.2, useProgressBar: false, useLargeIcons: false }");
        var client = serializer.deserialize();
        assertFalse(client.useLargeIcons);
        assertFalse(client.useProgressBar);
        assertTrue(client.customHudPosition);
        assertEquals(0.2f, client.hudPositionX);
        assertEquals(0.4f, client.hudPositionY);
        serializer.serialize(client);
        assertEquals(new Gson().toJsonTree(client), new Gson().toJsonTree(serializer.deserialize()));
        Files.writeString(path, "{useLargeIcons: false}");
        assertTrue(serializer.deserialize().useProgressBar);
    }
}
