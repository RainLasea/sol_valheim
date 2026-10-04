package vice.sol_valheim.mixin;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vice.sol_valheim.SOLValheim;
import vice.sol_valheim.accessors.PlayerEntityMixinDataAccessor;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
    @Inject(method = "hasEnoughFoodToStartSprinting", at = @At("HEAD"), cancellable = true)
    private void valheimSprint(CallbackInfoReturnable<Boolean> cir) {
        var player = (LocalPlayer) (Object) this;
        cir.setReturnValue(player.isPassenger() || player.mayFly()
                || player.tickCount < (long) SOLValheim.Config.common.respawnGracePeriod * 20
                || ((PlayerEntityMixinDataAccessor) player).sol_valheim$getFoodData().hasFood());
    }
}
