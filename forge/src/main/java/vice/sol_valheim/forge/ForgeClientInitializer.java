package vice.sol_valheim.forge;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import org.lwjgl.glfw.GLFW;
import vice.sol_valheim.FoodHUD;
import vice.sol_valheim.SOLValheim;
import vice.sol_valheim.client.ValheimConfigScreen;
@Mod.EventBusSubscriber(modid = SOLValheim.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ForgeClientInitializer {
    public static final KeyMapping POSITION_KEY = new KeyMapping("key.sol_valheim.position", GLFW.GLFW_KEY_UNKNOWN, "key.categories.sol_valheim");
    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) { event.register(POSITION_KEY); }
    @SubscribeEvent public static void layers(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.FOOD_LEVEL.id(), "food_slots", (gui, graphics, partialTick, width, height) -> FoodHUD.render(graphics));
    }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> new ValheimConfigScreen(parent)));
    }
}
