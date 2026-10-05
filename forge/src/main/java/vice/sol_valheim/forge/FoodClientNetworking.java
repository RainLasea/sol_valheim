package vice.sol_valheim.forge;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import vice.sol_valheim.ValheimFoodData;
import vice.sol_valheim.accessors.PlayerEntityMixinDataAccessor;
public final class FoodClientNetworking {
    public static void food(CompoundTag tag) {
        var player = Minecraft.getInstance().player;
        if (player != null) ((PlayerEntityMixinDataAccessor) player).sol_valheim$setFoodData(ValheimFoodData.read(tag, player.level().registryAccess()));
    }
}
