package vice.sol_valheim.forge;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.TickEvent;
import vice.sol_valheim.ConfigSync;
import vice.sol_valheim.PlayerLifecycle;
import vice.sol_valheim.SOLValheim;
import vice.sol_valheim.client.HudPositionScreen;
@Mod.EventBusSubscriber(modid = SOLValheim.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent public static void login(ClientPlayerNetworkEvent.LoggingIn e) {
        PlayerLifecycle.updateAttributes(e.getPlayer()); e.getPlayer().setHealth(e.getPlayer().getMaxHealth());
    }
    @SubscribeEvent public static void clonePlayer(ClientPlayerNetworkEvent.Clone e) {
        var old = e.getOldPlayer(); var player = e.getNewPlayer();
        PlayerLifecycle.clonePlayer(old, player, old.isDeadOrDying());
        player.setHealth(old.isDeadOrDying() ? player.getMaxHealth() : old.getHealth());
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        var client = Minecraft.getInstance();
        while (ForgeClientInitializer.POSITION_KEY.consumeClick())
            if (client.player != null && client.screen == null) client.setScreen(new HudPositionScreen(null));
    }
    @SubscribeEvent public static void tooltip(ItemTooltipEvent e) { SOLValheim.addTooltip(e.getItemStack(), e.getToolTip(), e.getEntity()); }
    @SubscribeEvent public static void hideHunger(RenderGuiOverlayEvent.Pre e) {
        var id = e.getOverlay().id();
        if (id.equals(VanillaGuiOverlay.FOOD_LEVEL.id()) || id.equals(vice.sol_valheim.FarmersDelightCompat.NOURISHMENT) || id.getNamespace().equals("appleskin")) e.setCanceled(true);
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e) { ConfigSync.disconnect(); }
}
