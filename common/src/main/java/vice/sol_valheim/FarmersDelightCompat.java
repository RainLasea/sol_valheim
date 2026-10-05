package vice.sol_valheim;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public final class FarmersDelightCompat {
    public static final ResourceLocation NOURISHMENT = ResourceLocation.tryParse("farmersdelight:nourishment");

    private FarmersDelightCompat() {}

    public static boolean hasNourishment(Player player) {
        return BuiltInRegistries.MOB_EFFECT.getHolder(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.MOB_EFFECT, NOURISHMENT)).map(effect -> GameVersion.hasEffect(player, effect)).orElse(false);
    }
}
