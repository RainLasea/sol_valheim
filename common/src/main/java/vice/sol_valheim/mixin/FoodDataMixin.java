package vice.sol_valheim.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class FoodDataMixin {
    @Shadow public abstract void setFoodLevel(int value);
    @Shadow public abstract void setSaturation(float value);
    @Shadow public abstract void setExhaustion(float value);

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void valheimHunger(Player player, CallbackInfo ci) {

        setFoodLevel(20);
        setSaturation(0f);
        setExhaustion(0f);
        ci.cancel();
    }
}
