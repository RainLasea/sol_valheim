package vice.sol_valheim.fabric;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import vice.sol_valheim.ConfigSync;
import vice.sol_valheim.ValheimFoodData;
import vice.sol_valheim.accessors.PlayerEntityMixinDataAccessor;
public final class FoodClientNetworking {
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(FoodNetworking.FOOD, (client, handler, buffer, sender) -> {
            var tag = buffer.readNbt();
            client.execute(() -> {
                if (client.player != null) ((PlayerEntityMixinDataAccessor) client.player).sol_valheim$setFoodData(ValheimFoodData.read(tag, client.player.level().registryAccess()));
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(FoodNetworking.CONFIG, (client, handler, buffer, sender) -> {
            var json = buffer.readUtf(1048576); client.execute(() -> ConfigSync.apply(json));
        });
    }
}
