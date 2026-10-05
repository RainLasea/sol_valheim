package vice.sol_valheim.fabric;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import vice.sol_valheim.ConfigSync;
import vice.sol_valheim.ValheimFoodData;
import vice.sol_valheim.accessors.PlayerEntityMixinDataAccessor;
public final class FoodClientNetworking {
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(FoodNetworking.FoodState.TYPE, (payload, context) ->
            ((PlayerEntityMixinDataAccessor) context.player()).sol_valheim$setFoodData(ValheimFoodData.read(payload.data(), context.player().level().registryAccess())));
        ClientPlayNetworking.registerGlobalReceiver(FoodNetworking.CommonConfig.TYPE, (payload, context) -> ConfigSync.apply(payload.json()));
    }
}
