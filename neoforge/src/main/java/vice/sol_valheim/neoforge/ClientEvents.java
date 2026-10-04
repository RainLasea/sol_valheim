package vice.sol_valheim.neoforge;

import me.shedaniel.autoconfig.AutoConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import vice.sol_valheim.ModConfig;
import vice.sol_valheim.SOLValheim;

@EventBusSubscriber(modid = SOLValheim.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        SOLValheim.addTooltip(event.getItemStack(), event.getToolTip());
    }
    @SubscribeEvent public static void hideHunger(RenderGuiLayerEvent.Pre event) {
        if (event.getName().equals(VanillaGuiLayers.FOOD_LEVEL)) event.setCanceled(true);
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {

        var holder = AutoConfig.getConfigHolder(ModConfig.class);
        SOLValheim.remoteCommon = null;
        holder.load();
        SOLValheim.Config = holder.getConfig();
        SOLValheim.Config.validate();
    }
}
