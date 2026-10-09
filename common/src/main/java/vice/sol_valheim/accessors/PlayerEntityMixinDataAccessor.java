package vice.sol_valheim.accessors;
import vice.sol_valheim.ValheimFoodData;
import vice.sol_valheim.PlayerHealthData;
public interface PlayerEntityMixinDataAccessor {
    ValheimFoodData sol_valheim$getFoodData();
    void sol_valheim$setFoodData(ValheimFoodData data);
    void sol_valheim$syncFoodData();
    PlayerHealthData sol_valheim$getHealthData();
    void sol_valheim$setHealthData(PlayerHealthData data);
}
