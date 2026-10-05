package vice.sol_valheim.fabric.mixin;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import vice.sol_valheim.PlayerLifecycle;
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setDayTime(J)V"))
    private void passNight(ServerLevel level, long time) {
        PlayerLifecycle.slept(level, time - level.getDayTime());
        level.setDayTime(time);
    }
}
