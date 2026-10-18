package vice.sol_valheim;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/** The basket forwards its selected food's properties but is never itself consumed. */
public final class SupplementariesCompat {
    private static final ResourceLocation LUNCH_BASKET = ResourceLocation.tryParse("supplementaries:lunch_basket");

    private SupplementariesCompat() {}

    public static boolean isLunchBasket(Item item) {
        return LUNCH_BASKET.equals(BuiltInRegistries.ITEM.getKey(item));
    }
}
