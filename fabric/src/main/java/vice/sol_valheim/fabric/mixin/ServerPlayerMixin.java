package vice.sol_valheim.fabric.mixin;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vice.sol_valheim.PlayerLifecycle;
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Inject(method = "initInventoryMenu", at = @At("TAIL"))
    private void dimensionSync(CallbackInfo ci) { PlayerLifecycle.syncAll((ServerPlayer) (Object) this); }
}
