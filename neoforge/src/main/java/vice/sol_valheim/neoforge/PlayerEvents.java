package vice.sol_valheim.neoforge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import vice.sol_valheim.PlayerLifecycle;
import vice.sol_valheim.SOLValheim;
public final class PlayerEvents {
    // Item callbacks can depend on datapack registries, which do not exist during common setup.
    @SubscribeEvent public void serverStarting(ServerStartingEvent e) { SOLValheim.generateFoodConfigs(); }
    @SubscribeEvent public void commands(RegisterCommandsEvent e) { PlayerLifecycle.commands(e.getDispatcher()); }
    @SubscribeEvent public void finishUsing(LivingEntityUseItemEvent.Finish e) { if (e.getEntity() instanceof Player p) SOLValheim.consume(p, e.getItem()); }
    @SubscribeEvent public void clonePlayer(PlayerEvent.Clone e) { PlayerLifecycle.clonePlayer(e.getOriginal(), e.getEntity(), e.isWasDeath()); }
    @SubscribeEvent public void joinLevel(EntityJoinLevelEvent e) { if (!e.getLevel().isClientSide() && e.getEntity() instanceof ServerPlayer p) updateAttributes(p); }
    @SubscribeEvent public void login(PlayerEvent.PlayerLoggedInEvent e) { sync(e.getEntity()); }
    @SubscribeEvent public void respawn(PlayerEvent.PlayerRespawnEvent e) { sync(e.getEntity()); }
    @SubscribeEvent public void dimension(PlayerEvent.PlayerChangedDimensionEvent e) { sync(e.getEntity()); }
    private static void sync(Player p) { if (p instanceof ServerPlayer s) PlayerLifecycle.syncAll(s); }
    @SubscribeEvent public void tick(PlayerTickEvent.Post e) { PlayerLifecycle.tick(e.getEntity()); }
    public static void updateAttributes(Player p) { PlayerLifecycle.updateAttributes(p); }
    @SubscribeEvent public void slept(SleepFinishedTimeEvent e) { if (e.getLevel() instanceof ServerLevel l) PlayerLifecycle.slept(l, e.getNewTime() - l.getDayTime()); }
}
