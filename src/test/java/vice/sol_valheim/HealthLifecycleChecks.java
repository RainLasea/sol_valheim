package vice.sol_valheim;

import com.mojang.authlib.GameProfile;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vice.sol_valheim.accessors.PlayerEntityMixinDataAccessor;
import vice.sol_valheim.neoforge.PlayerEvents;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HealthLifecycleChecks {
    @BeforeEach void setup() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        SOLValheim.Config = new ModConfig();
    }

    private static Level level(boolean clientSide) throws Exception {
        var level = mock(Level.class);
        var clientField = Level.class.getDeclaredField("isClientSide");
        clientField.setAccessible(true);
        clientField.setBoolean(level, clientSide);
        when(level.registryAccess()).thenReturn(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        return level;
    }

    private static TestPlayer player(boolean clientSide) throws Exception {
        return new TestPlayer(level(clientSide));
    }

    @Test void clientConstructionDoesNotQueryUninitializedGameMode() throws Exception {
        var level = level(true);
        var client = assertDoesNotThrow(() -> new ConstructionGuardPlayer(level));
        assertEquals(6f, client.getMaxHealth());
        assertEquals(6f, client.getHealth());
        assertEquals(0, client.tickCount);
    }

    @Test void clientStartsWithConfiguredHealthBeforeItsFirstTick() throws Exception {
        var client = player(true);
        assertEquals(6f, client.getMaxHealth());
        assertEquals(6f, client.getHealth());
        assertEquals(0, client.tickCount);
        SOLValheim.Config.common.startingHealth = 5;
        var custom = player(true);
        assertEquals(10f, custom.getMaxHealth());
        assertEquals(10f, custom.getHealth());
    }

    @Test void deathCloneRetainsFoodAndInitializesHealthBeforeRespawnPackets() throws Exception {
        var original = player(false);
        var food = ((PlayerEntityMixinDataAccessor) original).sol_valheim$getFoodData();
        food.eatItem(Items.BREAD);
        int ticks = food.ItemEntries.getFirst().ticksLeft;
        PlayerEvents.updateAttributes(original);
        original.setHealth(0);
        for (int i = 0; i < 40; i++) PlayerLifecycle.tick(original);
        assertEquals(ticks, food.ItemEntries.getFirst().ticksLeft);
        var respawned = player(false);
        new PlayerEvents().clonePlayer(new PlayerEvent.Clone(respawned, original, true));
        assertEquals(11f, respawned.getMaxHealth());
        assertEquals(11f, respawned.getHealth());
        var retained = ((PlayerEntityMixinDataAccessor) respawned).sol_valheim$getFoodData();
        assertEquals(ticks / 5, retained.ItemEntries.getFirst().ticksLeft);
        assertEquals(ticks, food.ItemEntries.getFirst().ticksLeft);

        // A second death exactly at the threshold keeps one fifth again.
        respawned.setHealth(0);
        var second = player(false);
        PlayerLifecycle.clonePlayer(respawned, second, true);
        var secondFood = ((PlayerEntityMixinDataAccessor) second).sol_valheim$getFoodData();
        assertEquals(ticks / 25, secondFood.ItemEntries.getFirst().ticksLeft);
        second.setHealth(0);
        var third = player(false);
        PlayerLifecycle.clonePlayer(second, third, true);
        assertFalse(((PlayerEntityMixinDataAccessor) third).sol_valheim$getFoodData().hasFood());
        assertEquals(6f, third.getMaxHealth());
        assertEquals(6f, third.getHealth());
    }

    @Test void deathCloneWithKeepFoodPreservesDrinksDynamicValuesAndTimers() throws Exception {
        SOLValheim.Config.common.keepFoodOnDeath = true;
        var original = player(false);
        var food = ((PlayerEntityMixinDataAccessor) original).sol_valheim$getFoodData();
        var rich = Items.BREAD.getDefaultInstance();
        rich.set(net.minecraft.core.component.DataComponents.FOOD,
                new net.minecraft.world.food.FoodProperties.Builder().nutrition(9).saturationModifier(0.9f).build());
        assertTrue(food.eatItem(rich, original));
        assertTrue(food.eatItem(Items.POTION));
        food.ItemEntries.getFirst().ticksLeft = 100;
        food.DrinkSlot.ticksLeft = 200;
        original.setHealth(0);
        var target = player(false);
        new PlayerEvents().clonePlayer(new PlayerEvent.Clone(target, original, true));
        var retained = ((PlayerEntityMixinDataAccessor) target).sol_valheim$getFoodData();
        assertEquals(100, retained.ItemEntries.getFirst().ticksLeft);
        assertEquals(200, retained.DrinkSlot.ticksLeft);
        assertEquals(9, retained.ItemEntries.getFirst().getConfig().nutrition);
        assertEquals(target.getMaxHealth(), target.getHealth());
        retained.tick();
        assertEquals(100, food.ItemEntries.getFirst().ticksLeft);
        assertEquals(200, food.DrinkSlot.ticksLeft);
    }

    @Test void foodHealthDefaultsToThreeRowsAndHonorsConfiguredCap() throws Exception {
        for (var item : new net.minecraft.world.item.Item[]{Items.BREAD, Items.APPLE, Items.CARROT})
            ModConfig.getFoodConfig(item).nutrition = 30;
        var player = player(false);
        var food = ((PlayerEntityMixinDataAccessor) player).sol_valheim$getFoodData();
        food.eatItem(Items.BREAD);
        food.eatItem(Items.APPLE);
        food.eatItem(Items.CARROT);
        PlayerLifecycle.updateAttributes(player);
        assertEquals(60f, player.getMaxHealth());
        player.setHealth(60f);
        SOLValheim.Config.common.maxHealth = 15;
        PlayerLifecycle.updateAttributes(player);
        assertEquals(30f, player.getMaxHealth());
        assertEquals(30f, player.getHealth());
    }

    @Test void nonDeathClonePreservesHealthAndOtherModsBonuses() throws Exception {
        var original = player(false);
        ((PlayerEntityMixinDataAccessor) original).sol_valheim$getFoodData().eatItem(Items.BREAD);
        var otherBonus = new AttributeModifier(ResourceLocation.fromNamespaceAndPath("test", "extra_health"),
                20, AttributeModifier.Operation.ADD_VALUE);
        original.getAttribute(Attributes.MAX_HEALTH).addTransientModifier(otherBonus);
        PlayerEvents.updateAttributes(original);
        original.setHealth(25);
        var target = player(false);
        target.getAttribute(Attributes.MAX_HEALTH).addTransientModifier(otherBonus);
        new PlayerEvents().clonePlayer(new PlayerEvent.Clone(target, original, false));
        assertEquals(31f, target.getMaxHealth());
        assertEquals(25f, target.getHealth());
        assertTrue(((PlayerEntityMixinDataAccessor) target).sol_valheim$getFoodData().hasFood());
        target.creative = true;
        PlayerEvents.updateAttributes(target);
        assertEquals(31f, target.getMaxHealth());
        assertNotNull(target.getAttribute(Attributes.MAX_HEALTH).getModifier(otherBonus.id()));
    }

    private static class TestPlayer extends Player {
        private boolean creative;
        TestPlayer(Level level) {
            super(level, BlockPos.ZERO, 0f, new GameProfile(UUID.randomUUID(), "TestPlayer"));
        }
        @Override public boolean isSpectator() { return false; }
        @Override public boolean isCreative() { return creative; }
    }

    /** Client players cannot resolve their game mode until their connection is installed. */
    private static final class ConstructionGuardPlayer extends Player {
        private boolean connectionReady;
        ConstructionGuardPlayer(Level level) {
            super(level, BlockPos.ZERO, 0f, new GameProfile(UUID.randomUUID(), "ConnectingPlayer"));
            connectionReady = true;
        }
        @Override public boolean isCreative() {
            if (!connectionReady) throw new IllegalStateException("Client connection is not initialized");
            return false;
        }
        @Override public boolean isSpectator() {
            if (!connectionReady) throw new IllegalStateException("Client connection is not initialized");
            return false;
        }
    }
}
