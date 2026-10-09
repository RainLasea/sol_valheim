package vice.sol_valheim.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vice.sol_valheim.PlayerLifecycle;
import vice.sol_valheim.accessors.PlayerEntityMixinDataAccessor;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerHealthMixin {
    @Unique private float sol_valheim$healthBeforeModeChange;

    @Inject(method = "setGameMode", at = @At("HEAD"))
    private void rememberHealth(GameType mode, CallbackInfoReturnable<Boolean> ci) {
        var player = (ServerPlayer) (Object) this;
        sol_valheim$healthBeforeModeChange = player.isCreative() || player.isSpectator()
                ? Float.NaN : player.getHealth();
    }

    @Inject(method = "setGameMode", at = @At("RETURN"))
    private void restoreHealth(GameType mode, CallbackInfoReturnable<Boolean> ci) {
        if (!ci.getReturnValueZ()) return;
        var player = (ServerPlayer) (Object) this;
        if (player.isCreative() || player.isSpectator())
            ((PlayerEntityMixinDataAccessor) player).sol_valheim$getHealthData().capture(sol_valheim$healthBeforeModeChange);
        PlayerLifecycle.updateAttributes(player);
    }
}
