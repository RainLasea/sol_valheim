package vice.sol_valheim.neoforge;

import me.shedaniel.autoconfig.AutoConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import vice.sol_valheim.ModConfig;
import vice.sol_valheim.SOLValheim;
import vice.sol_valheim.PlayerLifecycle;
import vice.sol_valheim.client.HudPositionScreen;

@EventBusSubscriber(modid = SOLValheim.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent public static void login(ClientPlayerNetworkEvent.LoggingIn event) {
        PlayerEvents.updateAttributes(event.getPlayer());
        event.getPlayer().setHealth(event.getPlayer().getMaxHealth());
    }
    @SubscribeEvent public static void clonePlayer(ClientPlayerNetworkEvent.Clone event) {
        var oldPlayer = event.getOldPlayer();
        var newPlayer = event.getNewPlayer();
        PlayerLifecycle.clonePlayer(oldPlayer, newPlayer, oldPlayer.isDeadOrDying());
    }
    @SubscribeEvent public static void clientTick(ClientTickEvent.Post event) {
        var client = Minecraft.getInstance();
        while (ClientModEvents.POSITION_KEY.consumeClick()) {
            if (client.player != null && client.screen == null) client.setScreen(new HudPositionScreen(null));
        }
    }
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        SOLValheim.addTooltip(event.getItemStack(), event.getToolTip(), event.getEntity());
    }
    @SubscribeEvent public static void hideHunger(RenderGuiLayerEvent.Pre event) {
        if (event.getName().equals(VanillaGuiLayers.FOOD_LEVEL)
                || event.getName().equals(FarmersDelightCompat.NOURISHMENT)
                || event.getName().getNamespace().equals("appleskin")) event.setCanceled(true);
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {

        var holder = AutoConfig.getConfigHolder(ModConfig.class);
        SOLValheim.remoteCommon = null;
        holder.load();
        SOLValheim.Config = holder.getConfig();
        SOLValheim.Config.validate();
    }
}
