package vice.sol_valheim.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vice.sol_valheim.ValheimFoodData;
import vice.sol_valheim.SOLValheim;
import vice.sol_valheim.accessors.PlayerEntityMixinDataAccessor;
import vice.sol_valheim.platform.Platform;

@Mixin(Player.class)
public abstract class PlayerEntityMixin implements PlayerEntityMixinDataAccessor {
    @Unique private ValheimFoodData sol_valheim$data;

    @Override public ValheimFoodData sol_valheim$getFoodData() {
        if (sol_valheim$data == null) sol_valheim$data = new ValheimFoodData();
        return sol_valheim$data;
    }
    @Override public void sol_valheim$setFoodData(ValheimFoodData data) { sol_valheim$data = data; }
    @Override public void sol_valheim$syncFoodData() { Platform.sync((Player) (Object) this); }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void initializeClientHealth(CallbackInfo ci) {
        var player = (Player) (Object) this;
        // The server initializes before tracking; the client must also start with the configured hearts.
        if (!player.level().isClientSide) return;
        vice.sol_valheim.GameVersion.initializeClientHealth(player);
        player.setHealth(player.getMaxHealth());
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void saveFood(CompoundTag tag, CallbackInfo ci) {
        tag.put("sol_food_data", sol_valheim$getFoodData().save(new CompoundTag(), ((Player) (Object) this).level().registryAccess()));
    }
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void loadFood(CompoundTag tag, CallbackInfo ci) {
        sol_valheim$data = ValheimFoodData.read(tag.getCompound("sol_food_data"), ((Player) (Object) this).level().registryAccess());
    }
}
