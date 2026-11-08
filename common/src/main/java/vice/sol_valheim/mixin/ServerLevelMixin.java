package vice.sol_valheim.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vice.sol_valheim.PlayerLifecycle;

import java.util.function.BooleanSupplier;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Unique private long sol_valheim$dayTimeBeforeTick;
    @Unique private long sol_valheim$gameTimeBeforeTick;
    @Unique private boolean sol_valheim$hadSleepingPlayers;

    @Inject(method = "tick", at = @At("HEAD"))
    private void sol_valheim$captureSleepTime(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        var level = (ServerLevel) (Object) this;
        sol_valheim$dayTimeBeforeTick = level.getDayTime();
        sol_valheim$gameTimeBeforeTick = level.getGameTime();
        sol_valheim$hadSleepingPlayers = level.players().stream().anyMatch(Player::isSleeping);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void sol_valheim$passSleepTime(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        if (!sol_valheim$hadSleepingPlayers) return;
        var level = (ServerLevel) (Object) this;
        // Read the final wake time after other mods; simulated ticks already advance food normally.
        long elapsed = level.getDayTime() - sol_valheim$dayTimeBeforeTick
                - (level.getGameTime() - sol_valheim$gameTimeBeforeTick);
        PlayerLifecycle.slept(level, elapsed);
    }
}
