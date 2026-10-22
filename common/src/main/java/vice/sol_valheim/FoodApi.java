package vice.sol_valheim;

import net.minecraft.world.entity.player.Player;
import vice.sol_valheim.accessors.PlayerEntityMixinDataAccessor;

/** Stable entry points for server scripts; mutations must run on the server thread. */
public final class FoodApi {
    private FoodApi() {}

    public static int getMaxSlots(Player player) {
        return data(player).MaxItemSlots;
    }

    public static int getOccupiedSlots(Player player) {
        var food = data(player);
        return food.ItemEntries.size() + (food.DrinkSlot == null ? 0 : 1);
    }

    public static void setMaxSlots(Player player, int slots) {
        requireServer(player);
        if (slots < 2 || slots > 5) throw new IllegalArgumentException("Food slots must be between 2 and 5");
        data(player).setMaxSlotsOverride(slots);
        sync(player);
    }

    public static void resetMaxSlots(Player player) {
        requireServer(player);
        data(player).resetMaxSlotsOverride();
        sync(player);
    }

    /** Same threshold as the original speed bonus, independent of speedBoost. */
    public static boolean isFull(Player player) {
        return !player.isDeadOrDying() && !player.isCreative() && !player.isSpectator()
                && Math.min(SOLValheim.Config.common.maxHealth * 2,
                    SOLValheim.Config.common.startingHealth * 2 + data(player).getTotalFoodNutrition()) >= 20;
    }

    private static ValheimFoodData data(Player player) {
        return ((PlayerEntityMixinDataAccessor) player).sol_valheim$getFoodData();
    }

    private static void requireServer(Player player) {
        if (player.level().isClientSide) throw new IllegalStateException("Food slots must be changed on the server");
    }

    private static void sync(Player player) {
        PlayerLifecycle.updateAttributes(player);
        ((PlayerEntityMixinDataAccessor) player).sol_valheim$syncFoodData();
    }
}
