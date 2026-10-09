package vice.sol_valheim;

import net.minecraft.world.entity.player.Player;
import vice.sol_valheim.accessors.HungerDataAccessor;
import vice.sol_valheim.accessors.PlayerEntityMixinDataAccessor;

/** Shared by all loaders; no dependency on the mod requesting the hunger cost. */
public final class HungerCompatibility {
    private HungerCompatibility() {}

    public static void bind(Player player) {
        // A replacement FoodData can opt out by not implementing our accessor.
        if (player.getFoodData() instanceof HungerDataAccessor accessor)
            accessor.sol_valheim$bindPlayer(player);
    }

    public static void consume(Player player, double hungerPoints, double exhaustion) {
        if (player == null || player.level().isClientSide || !SOLValheim.Config.common.convertHungerCosts
                || player.isDeadOrDying() || player.isCreative() || player.isSpectator()) return;
        var config = SOLValheim.Config.common;
        double seconds = positive(hungerPoints) * config.hungerSecondsPerPoint
                + positive(exhaustion) / 4.0 * config.exhaustionSecondsPerPoint;
        ((PlayerEntityMixinDataAccessor) player).sol_valheim$getFoodData().queueHungerCost(seconds);
    }

    private static double positive(double value) {
        return Double.isFinite(value) && value > 0 ? value : 0;
    }
}
