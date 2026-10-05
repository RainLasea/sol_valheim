package vice.sol_valheim.fabric;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import vice.sol_valheim.ConfigSync;
import vice.sol_valheim.PlayerLifecycle;
import vice.sol_valheim.client.HudPositionScreen;
public final class FabricClientInitializer implements ClientModInitializer {
    private static final KeyMapping POSITION_KEY = new KeyMapping("key.sol_valheim.position", GLFW.GLFW_KEY_UNKNOWN, "key.categories.sol_valheim");
    @Override public void onInitializeClient() {
        FoodClientNetworking.register(); FabricClientVersion.registerTooltip();
        KeyBindingHelper.registerKeyBinding(POSITION_KEY);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.player != null) { PlayerLifecycle.updateAttributes(client.player); client.player.setHealth(client.player.getMaxHealth()); }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ConfigSync.disconnect());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (POSITION_KEY.consumeClick()) {
                if (client.player != null && client.screen == null) client.setScreen(new HudPositionScreen(null));
            }
        });
    }
}
