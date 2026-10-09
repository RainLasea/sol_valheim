package vice.sol_valheim.accessors;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Optional bridge implemented only by Onion's food containers. */
public interface LunchContainerAccessor {
    ItemStack sol_valheim$getActualFood(Player player, ItemStack container);
}
