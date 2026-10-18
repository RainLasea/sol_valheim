package vice.sol_valheim.mixin;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vice.sol_valheim.PlayerLifecycle;

import java.util.concurrent.CompletableFuture;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "reloadResources", at = @At("RETURN"), cancellable = true)
    private void sol_valheim$reloadConfig(CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        var server = (MinecraftServer) (Object) this;
        // Chain onto successful completion so updated tags reach clients before food state.
        cir.setReturnValue(cir.getReturnValue().thenRunAsync(() -> {
            if (!PlayerLifecycle.reloadConfig(server))
                throw new IllegalStateException("SOL Valheim config reload failed; check the server log");
        }, server));
    }
}
