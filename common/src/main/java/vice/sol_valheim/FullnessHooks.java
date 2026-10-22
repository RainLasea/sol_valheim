package vice.sol_valheim;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import vice.sol_valheim.accessors.PlayerEntityMixinDataAccessor;

/** Datapack function tags run as the affected player, at their location, with permission level 2. */
public final class FullnessHooks {
    public static final ResourceLocation ENTER = ResourceLocation.tryParse("sol_valheim:full_enter");
    public static final ResourceLocation TICK = ResourceLocation.tryParse("sol_valheim:full_tick");
    public static final ResourceLocation EXIT = ResourceLocation.tryParse("sol_valheim:full_exit");

    private FullnessHooks() {}

    public static void tick(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        var food = ((PlayerEntityMixinDataAccessor) player).sol_valheim$getFoodData();
        boolean full = FoodApi.isFull(player);
        boolean changed = food.fullEffectActive != full;
        // Update before running functions, which may change this player's food or slots.
        food.fullEffectActive = full;
        if (changed) run(serverPlayer, full ? ENTER : EXIT);
        if (full) run(serverPlayer, TICK);
    }

    private static void run(ServerPlayer player, ResourceLocation tag) {
        var server = player.getServer();
        if (server.getFunctions().getTag(tag).isEmpty()) return;
        server.getCommands().performPrefixedCommand(
                player.createCommandSourceStack().withPermission(2).withSuppressedOutput(), "function #" + tag);
    }
}
