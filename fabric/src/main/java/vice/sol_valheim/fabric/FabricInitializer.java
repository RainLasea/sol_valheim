package vice.sol_valheim.fabric;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import vice.sol_valheim.PlayerLifecycle;
import vice.sol_valheim.SOLValheim;
import vice.sol_valheim.platform.Platform;
public final class FabricInitializer implements ModInitializer {
    @Override public void onInitialize() {
        SOLValheim.init(); FoodNetworking.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, selection) -> PlayerLifecycle.commands(dispatcher));
        ServerLifecycleEvents.SERVER_STARTING.register(server -> { Platform.server = server; SOLValheim.generateFoodConfigs(); });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> Platform.server = null);
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> PlayerLifecycle.clonePlayer(oldPlayer, newPlayer, !alive));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> PlayerLifecycle.syncAll(newPlayer));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> PlayerLifecycle.syncAll(handler.player));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (var player : server.getPlayerList().getPlayers()) PlayerLifecycle.tick(player);
        });
    }
}
