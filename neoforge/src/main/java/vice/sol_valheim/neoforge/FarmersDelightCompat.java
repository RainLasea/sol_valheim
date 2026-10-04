package vice.sol_valheim.neoforge;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public final class FarmersDelightCompat {
    public static final ResourceLocation NOURISHMENT = ResourceLocation.fromNamespaceAndPath("farmersdelight", "nourishment");

    private FarmersDelightCompat() {}

    public static boolean hasNourishment(Player player) {
        return BuiltInRegistries.MOB_EFFECT.getHolder(NOURISHMENT).map(player::hasEffect).orElse(false);
    }
}
