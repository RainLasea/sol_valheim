package vice.sol_valheim;

import com.mojang.authlib.GameProfile;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vice.sol_valheim.accessors.PlayerEntityMixinDataAccessor;
import vice.sol_valheim.platform.Platform;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HealthGameModeChecks {
    @BeforeEach void setup() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        SOLValheim.Config = new ModConfig();
        ModConfig.getFoodConfig(Items.BREAD).nutrition = 34;
    }

    private static TestPlayer player() throws Exception {
        var level = mock(Level.class);
        when(level.registryAccess()).thenReturn(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        var side = Level.class.getDeclaredField("isClientSide");
        side.setAccessible(true);
        side.setBoolean(level, false);
        var player = new TestPlayer(level);
        data(player).sol_valheim$getFoodData().eatItem(Items.BREAD);
        PlayerLifecycle.updateAttributes(player);
        assertEquals(40f, player.getMaxHealth());
        player.setHealth(25f);
        return player;
    }

    private static PlayerEntityMixinDataAccessor data(Player player) {
        return (PlayerEntityMixinDataAccessor) player;
    }

    @Test void actualModeChangePreservesFoodCapAndRestoresHealthAcrossProtectedModes() throws Exception {
        var player = serverPlayer();
        assertTrue(player.setGameMode(GameType.CREATIVE));
        assertEquals(40f, player.getMaxHealth());
        assertEquals(25f, player.getHealth());
        player.setHealth(40f); // A command or another mod heals the player during creative.
        assertTrue(player.setGameMode(GameType.SPECTATOR));
        assertTrue(player.setGameMode(GameType.CREATIVE));
        assertTrue(player.setGameMode(GameType.ADVENTURE));
        assertEquals(25f, player.getHealth());
        assertTrue(data(player).sol_valheim$getHealthData().save().isEmpty());
        player.setHealth(15f);
        assertTrue(player.setGameMode(GameType.SPECTATOR));
        player.setHealth(40f);
        assertTrue(player.setGameMode(GameType.SURVIVAL));
        assertEquals(15f, player.getHealth());
    }

    @Test void rejectedModeChangeDoesNotCaptureOrRestoreHealth() throws Exception {
        var player = serverPlayer();
        doReturn(false).when(player.gameMode).changeGameModeForPlayer(GameType.CREATIVE);
        assertFalse(player.setGameMode(GameType.CREATIVE));
        assertTrue(data(player).sol_valheim$getHealthData().save().isEmpty());
        player.setHealth(30f);
        assertTrue(player.setGameMode(GameType.SPECTATOR));
        player.setHealth(40f);
        doReturn(false).when(player.gameMode).changeGameModeForPlayer(GameType.SURVIVAL);
        assertFalse(player.setGameMode(GameType.SURVIVAL));
        assertEquals(40f, player.getHealth());
        assertEquals(30f, data(player).sol_valheim$getHealthData().save().getFloat("survival_health"));
        assertTrue(player.setGameMode(GameType.ADVENTURE));
        assertEquals(30f, player.getHealth());
    }

    @Test void creativeSaveReloadRetainsSnapshotUntilTheSavedModeIsLoaded() throws Exception {
        var original = player();
        original.mode = GameType.CREATIVE;
        PlayerLifecycle.updateAttributes(original);
        original.setHealth(40f);
        var tag = new CompoundTag();
        original.addAdditionalSaveData(tag);
        var restored = player();
        restored.readAdditionalSaveData(tag); // ServerPlayer loads its actual game mode later.
        assertEquals(25f, data(restored).sol_valheim$getHealthData().save().getFloat("survival_health"));
        restored.mode = GameType.CREATIVE;
        PlayerLifecycle.updateAttributes(restored);
        assertEquals(40f, restored.getHealth());
        restored.mode = GameType.SURVIVAL;
        PlayerLifecycle.updateAttributes(restored);
        assertEquals(25f, restored.getHealth());
    }

    @Test void legacySaveWithoutPermanentModifierRetainsHealthAboveVanillaCap() throws Exception {
        var original = player();
        original.setHealth(35f);
        var tag = new CompoundTag();
        original.addAdditionalSaveData(tag);
        tag.remove("attributes");
        tag.remove("sol_health_data");
        var restored = new TestPlayer(original.level());
        restored.readAdditionalSaveData(tag);
        assertEquals(40f, restored.getMaxHealth());
        assertEquals(35f, restored.getHealth());
    }

    @Test void vanillaAttributeSerializationKeepsFoodHealthAndOtherModsModifiers() throws Exception {
        var original = player();
        var attribute = original.getAttribute(Attributes.MAX_HEALTH);
        var other = new AttributeModifier(ResourceLocation.parse("test:health"), 10,
                AttributeModifier.Operation.ADD_VALUE);
        attribute.addPermanentModifier(other);
        PlayerLifecycle.updateAttributes(original);
        original.setHealth(45f);
        var restored = new TestPlayer(original.level());
        restored.getAttribute(Attributes.MAX_HEALTH).load(attribute.save());
        restored.setHealth(original.getHealth());
        assertEquals(50f, restored.getMaxHealth());
        assertEquals(45f, restored.getHealth());
        assertEquals(other, restored.getAttribute(Attributes.MAX_HEALTH).getModifier(other.id()));
    }

    @Test void foodAndConfigChangesKeepAbsoluteHealthAndOnlyClampWhenRequired() throws Exception {
        var player = player();
        data(player).sol_valheim$getFoodData().eatItem(Items.APPLE);
        PlayerLifecycle.updateAttributes(player);
        assertEquals(25f, player.getHealth());
        assertTrue(player.getMaxHealth() > 40f);
        data(player).sol_valheim$getFoodData().clear();
        PlayerLifecycle.updateAttributes(player);
        assertEquals(6f, player.getHealth());
        data(player).sol_valheim$getFoodData().eatItem(Items.BREAD);
        PlayerLifecycle.updateAttributes(player);
        assertEquals(40f, player.getMaxHealth());
        assertEquals(6f, player.getHealth());
    }

    @Test void lowerCapClampsSnapshotAndLaterIncreaseDoesNotRestoreItAgain() throws Exception {
        var player = player();
        player.mode = GameType.CREATIVE;
        PlayerLifecycle.updateAttributes(player);
        SOLValheim.Config.common.maxHealth = 10;
        PlayerLifecycle.updateAttributes(player);
        assertEquals(20f, player.getHealth());
        player.mode = GameType.SURVIVAL;
        PlayerLifecycle.updateAttributes(player);
        assertEquals(20f, player.getHealth());
        SOLValheim.Config.common.maxHealth = 30;
        PlayerLifecycle.updateAttributes(player);
        assertEquals(40f, player.getMaxHealth());
        assertEquals(20f, player.getHealth());
    }

    @Test void nonDeathCloneCopiesSnapshotIndependentlyAndDeathStartsNewHealth() throws Exception {
        var original = player();
        original.mode = GameType.CREATIVE;
        PlayerLifecycle.updateAttributes(original);
        original.setHealth(40f);
        var cloned = player();
        cloned.mode = GameType.CREATIVE;
        PlayerLifecycle.clonePlayer(original, cloned, false);
        cloned.mode = GameType.SURVIVAL;
        PlayerLifecycle.updateAttributes(cloned);
        assertEquals(25f, cloned.getHealth());
        assertEquals(25f, data(original).sol_valheim$getHealthData().save().getFloat("survival_health"));
        original.setHealth(0f);
        var respawned = player();
        respawned.mode = GameType.CREATIVE;
        PlayerLifecycle.clonePlayer(original, respawned, true);
        respawned.mode = GameType.SURVIVAL;
        PlayerLifecycle.updateAttributes(respawned);
        assertEquals(respawned.getMaxHealth(), respawned.getHealth());
        assertNotEquals(25f, respawned.getHealth());
    }

    @Test void protectedModesFreezeFoodDuringTicksConsumptionAndSleep() throws Exception {
        var active = serverPlayer();
        var creative = serverPlayer();
        var spectator = serverPlayer();
        creative.setGameMode(GameType.CREATIVE);
        spectator.setGameMode(GameType.SPECTATOR);
        int ticks = data(creative).sol_valheim$getFoodData().ItemEntries.getFirst().ticksLeft;
        var level = mock(ServerLevel.class);
        when(level.players()).thenReturn(List.of(active, creative, spectator));
        try (var platform = mockStatic(Platform.class, CALLS_REAL_METHODS)) {
            platform.when(() -> Platform.sync(any(Player.class))).thenAnswer(call -> null);
            for (var player : List.of(creative, spectator)) {
                PlayerLifecycle.tick(player);
                SOLValheim.consume(player, Items.ROTTEN_FLESH.getDefaultInstance());
                SOLValheim.consume(player, Items.APPLE.getDefaultInstance());
            }
            PlayerLifecycle.slept(level, 100);
        }
        assertEquals(ticks - 100, data(active).sol_valheim$getFoodData().ItemEntries.getFirst().ticksLeft);
        for (var player : List.of(creative, spectator)) {
            var food = data(player).sol_valheim$getFoodData();
            assertEquals(1, food.ItemEntries.size());
            assertEquals(ticks, food.ItemEntries.getFirst().ticksLeft);
            assertEquals(25f, player.getHealth());
        }
    }

    @Test void corruptSnapshotAndDeadPlayersCannotBeRevivedByRestoration() throws Exception {
        var player = player();
        for (float invalid : new float[]{Float.NaN, Float.POSITIVE_INFINITY, -5f, 0f}) {
            var tag = new CompoundTag();
            tag.putFloat("survival_health", invalid);
            var health = PlayerHealthData.read(tag);
            health.restore(player);
            assertEquals(25f, player.getHealth());
            assertTrue(health.save().isEmpty());
        }
        var health = new PlayerHealthData();
        health.capture(25f);
        player.setHealth(0f);
        health.restore(player);
        assertEquals(0f, player.getHealth());
    }

    /** Execute the transformed ServerPlayer.setGameMode, mocking only its environment. */
    private static ServerPlayer serverPlayer() throws Exception {
        var backing = player();
        var player = mock(ServerPlayer.class);
        var mode = new AtomicReference<>(GameType.SURVIVAL);
        var gameMode = mock(ServerPlayerGameMode.class);
        var field = ServerPlayer.class.getDeclaredField("gameMode");
        field.setAccessible(true);
        field.set(player, gameMode);
        player.connection = mock(ServerGamePacketListenerImpl.class);
        when(gameMode.getGameModeForPlayer()).thenAnswer(call -> mode.get());
        when(gameMode.changeGameModeForPlayer(any())).thenAnswer(call -> {
            GameType next = call.getArgument(0);
            if (next == mode.get()) return false;
            mode.set(next);
            return true;
        });
        when(player.isCreative()).thenAnswer(call -> mode.get() == GameType.CREATIVE);
        when(player.isSpectator()).thenAnswer(call -> mode.get() == GameType.SPECTATOR);
        when(player.level()).thenReturn(backing.level());
        when(player.getItemBySlot(any())).thenReturn(ItemStack.EMPTY);
        when(player.getAttribute(Attributes.MAX_HEALTH)).thenReturn(backing.getAttribute(Attributes.MAX_HEALTH));
        when(player.getAttribute(Attributes.MOVEMENT_SPEED)).thenReturn(backing.getAttribute(Attributes.MOVEMENT_SPEED));
        when(player.getMaxHealth()).thenAnswer(call -> backing.getMaxHealth());
        when(player.getHealth()).thenAnswer(call -> backing.getHealth());
        when(player.isDeadOrDying()).thenAnswer(call -> backing.isDeadOrDying());
        doAnswer(call -> { backing.setHealth(call.getArgument(0)); return null; }).when(player).setHealth(anyFloat());
        when(data(player).sol_valheim$getFoodData()).thenReturn(data(backing).sol_valheim$getFoodData());
        when(data(player).sol_valheim$getHealthData()).thenReturn(data(backing).sol_valheim$getHealthData());
        doCallRealMethod().when(player).setGameMode(any());
        return player;
    }

    private static class TestPlayer extends Player {
        private GameType mode = GameType.SURVIVAL;
        TestPlayer(Level level) {
            super(level, BlockPos.ZERO, 0f, new GameProfile(UUID.randomUUID(), "HealthTest"));
        }
        @Override public boolean isSpectator() { return mode == GameType.SPECTATOR; }
        @Override public boolean isCreative() { return mode == GameType.CREATIVE; }
    }
}
