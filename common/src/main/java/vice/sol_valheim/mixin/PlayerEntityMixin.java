package vice.sol_valheim.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vice.sol_valheim.ValheimFoodData;
import vice.sol_valheim.PlayerHealthData;
import vice.sol_valheim.GameVersion;
import vice.sol_valheim.SOLValheim;
import vice.sol_valheim.accessors.PlayerEntityMixinDataAccessor;
import vice.sol_valheim.platform.Platform;

@Mixin(Player.class)
public abstract class PlayerEntityMixin implements PlayerEntityMixinDataAccessor {
    @Unique private ValheimFoodData sol_valheim$data;
    @Unique private PlayerHealthData sol_valheim$healthData;

    @Override public ValheimFoodData sol_valheim$getFoodData() {
        if (sol_valheim$data == null) sol_valheim$data = new ValheimFoodData();
        return sol_valheim$data;
    }
    @Override public void sol_valheim$setFoodData(ValheimFoodData data) { sol_valheim$data = data; }
    @Override public void sol_valheim$syncFoodData() { Platform.sync((Player) (Object) this); }
    @Override public PlayerHealthData sol_valheim$getHealthData() {
        if (sol_valheim$healthData == null) sol_valheim$healthData = new PlayerHealthData();
        return sol_valheim$healthData;
    }
    @Override public void sol_valheim$setHealthData(PlayerHealthData data) { sol_valheim$healthData = data; }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void initializeClientHealth(CallbackInfo ci) {
        var player = (Player) (Object) this;
        vice.sol_valheim.HungerCompatibility.bind(player);
        // The server initializes before tracking; the client must also start with the configured hearts.
        if (!player.level().isClientSide) return;
        vice.sol_valheim.GameVersion.initializeClientHealth(player);
        player.setHealth(player.getMaxHealth());
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void saveFood(CompoundTag tag, CallbackInfo ci) {
        tag.put("sol_food_data", sol_valheim$getFoodData().save(new CompoundTag(), ((Player) (Object) this).level().registryAccess()));
        tag.put("sol_health_data", sol_valheim$getHealthData().save());
    }
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void loadFood(CompoundTag tag, CallbackInfo ci) {
        sol_valheim$data = ValheimFoodData.read(tag.getCompound("sol_food_data"), ((Player) (Object) this).level().registryAccess());
        sol_valheim$healthData = PlayerHealthData.read(tag.getCompound("sol_health_data"));
        var player = (Player) (Object) this;
        if (player.level().isClientSide) return;
        // Old saves had transient attributes: vanilla may already have clamped Health to 20.
        // Do not restore the survival snapshot here; the saved game mode loads later.
        GameVersion.updateAttributes(player);
        if (tag.contains("Health", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)) {
            float health = tag.getFloat("Health");
            if (Float.isFinite(health) && health >= 0 && player.getHealth() != Math.min(health, player.getMaxHealth()))
                player.setHealth(health);
        }
    }
}
