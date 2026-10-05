package vice.sol_valheim.neoforge;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import vice.sol_valheim.FoodHUD;
import vice.sol_valheim.SOLValheim;
import vice.sol_valheim.client.ValheimConfigScreen;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = SOLValheim.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientModEvents {
    public static final KeyMapping POSITION_KEY = new KeyMapping("key.sol_valheim.position", GLFW.GLFW_KEY_UNKNOWN, "key.categories.sol_valheim");

    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) {
        event.register(POSITION_KEY);
    }
    @SubscribeEvent public static void layers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, ResourceLocation.fromNamespaceAndPath(SOLValheim.MOD_ID, "food_slots"), (graphics, delta) -> FoodHUD.render(graphics));
    }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        ModList.get().getModContainerById(SOLValheim.MOD_ID).orElseThrow().registerExtensionPoint(
                IConfigScreenFactory.class, (container, parent) -> new ValheimConfigScreen(parent));
    }
}
