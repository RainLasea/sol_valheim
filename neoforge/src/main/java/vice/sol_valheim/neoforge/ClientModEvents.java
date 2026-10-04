package vice.sol_valheim.neoforge;

import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import vice.sol_valheim.FoodHUD;
import vice.sol_valheim.ModConfig;
import vice.sol_valheim.SOLValheim;

@EventBusSubscriber(modid = SOLValheim.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientModEvents {
    @SubscribeEvent public static void layers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, ResourceLocation.fromNamespaceAndPath(SOLValheim.MOD_ID, "food_slots"), FoodHUD::render);
    }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        ModList.get().getModContainerById(SOLValheim.MOD_ID).orElseThrow().registerExtensionPoint(
                IConfigScreenFactory.class, (container, parent) -> AutoConfig.getConfigScreen(ModConfig.class, parent).get());
    }
}
