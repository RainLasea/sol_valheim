package vice.sol_valheim;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Exercise the injected continuation in the transformed MinecraftServer class. */
class ConfigReloadChecks {
    @BeforeEach void setup() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        SOLValheim.Config = new ModConfig();
    }

    @SuppressWarnings("unchecked")
    @Test void vanillaReloadWaitsForDatapacksThenReloadsCommonConfig() throws Exception {
        var previous = SOLValheim.Config;
        var replacement = new ModConfig();
        replacement.common.foodBlacklist.add("minecraft:potion");
        var holder = (ConfigHolder<ModConfig>) mock(ConfigHolder.class);
        when(holder.load()).thenReturn(true);
        when(holder.getConfig()).thenReturn(replacement);
        var server = server();
        try (var config = mockStatic(AutoConfig.class)) {
            config.when(() -> AutoConfig.getConfigHolder(ModConfig.class)).thenReturn(holder);
            var datapackReload = new CompletableFuture<Void>();
            var result = continueReload(server, datapackReload);
            assertFalse(result.isDone());
            verify(holder, never()).load();
            assertSame(previous, SOLValheim.Config);
            datapackReload.complete(null);
            result.join();
            verify(holder).load();
            verify(holder).save();
            assertSame(replacement, SOLValheim.Config);
            assertNull(ModConfig.getFoodConfig(Items.POTION));
        }
    }

    @SuppressWarnings("unchecked")
    @Test void failedDatapackReloadDoesNotReloadSolConfig() throws Exception {
        var previous = SOLValheim.Config;
        var holder = (ConfigHolder<ModConfig>) mock(ConfigHolder.class);
        try (var config = mockStatic(AutoConfig.class)) {
            config.when(() -> AutoConfig.getConfigHolder(ModConfig.class)).thenReturn(holder);
            var failure = new IllegalStateException("Invalid datapack");
            var result = continueReload(server(), CompletableFuture.failedFuture(failure));
            assertSame(failure, assertThrows(CompletionException.class, result::join).getCause());
            verifyNoInteractions(holder);
            assertSame(previous, SOLValheim.Config);
        }
    }

    @SuppressWarnings("unchecked")
    @Test void invalidConfigKeepsPreviousSettingsAndReportsReloadFailure() throws Exception {
        var previous = SOLValheim.Config;
        var holder = (ConfigHolder<ModConfig>) mock(ConfigHolder.class);
        when(holder.load()).thenReturn(false);
        try (var config = mockStatic(AutoConfig.class)) {
            config.when(() -> AutoConfig.getConfigHolder(ModConfig.class)).thenReturn(holder);
            var result = continueReload(server(), CompletableFuture.completedFuture(null));
            assertThrows(CompletionException.class, result::join);
            assertSame(previous, SOLValheim.Config);
            verify(holder, never()).save();
        }
    }

    private static MinecraftServer server() {
        var server = mock(MinecraftServer.class);
        var players = mock(PlayerList.class);
        when(server.getPlayerList()).thenReturn(players);
        when(players.getPlayers()).thenReturn(List.of());
        doAnswer(call -> { call.<Runnable>getArgument(0).run(); return null; })
                .when(server).execute(any(Runnable.class));
        return server;
    }

    private static CompletableFuture<Void> continueReload(MinecraftServer server, CompletableFuture<Void> datapacks) throws Exception {
        Method continuation = Arrays.stream(MinecraftServer.class.getDeclaredMethods())
                .filter(method -> method.getName().contains("sol_valheim$reloadConfig"))
                .filter(method -> Arrays.equals(method.getParameterTypes(), new Class<?>[]{CallbackInfoReturnable.class}))
                .findFirst().orElseThrow(() -> new AssertionError("MinecraftServer reload mixin was not applied"));
        continuation.setAccessible(true);
        var callback = new CallbackInfoReturnable<>("reloadResources", true, datapacks);
        continuation.invoke(server, callback);
        return callback.getReturnValue();
    }
}
