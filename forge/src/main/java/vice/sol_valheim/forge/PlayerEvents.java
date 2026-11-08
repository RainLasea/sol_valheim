package vice.sol_valheim.forge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import vice.sol_valheim.PlayerLifecycle;
import vice.sol_valheim.FoodCommands;
import vice.sol_valheim.SOLValheim;
public final class PlayerEvents {
    @SubscribeEvent public void commands(RegisterCommandsEvent e) { FoodCommands.register(e.getDispatcher()); }
    @SubscribeEvent public void finishUsing(LivingEntityUseItemEvent.Finish e) { if (e.getEntity() instanceof Player p) SOLValheim.consume(p, e.getItem()); }
    @SubscribeEvent public void clonePlayer(PlayerEvent.Clone e) { PlayerLifecycle.clonePlayer(e.getOriginal(), e.getEntity(), e.isWasDeath()); }
    @SubscribeEvent public void joinLevel(EntityJoinLevelEvent e) { if (!e.getLevel().isClientSide() && e.getEntity() instanceof ServerPlayer p) PlayerLifecycle.updateAttributes(p); }
    @SubscribeEvent public void login(PlayerEvent.PlayerLoggedInEvent e) { sync(e.getEntity()); }
    @SubscribeEvent public void respawn(PlayerEvent.PlayerRespawnEvent e) { sync(e.getEntity()); }
    @SubscribeEvent public void dimension(PlayerEvent.PlayerChangedDimensionEvent e) { sync(e.getEntity()); }
    private static void sync(Player p) { if (p instanceof ServerPlayer s) PlayerLifecycle.syncAll(s); }
    @SubscribeEvent public void tick(TickEvent.PlayerTickEvent e) { if (e.phase == TickEvent.Phase.END) PlayerLifecycle.tick(e.player); }
}
