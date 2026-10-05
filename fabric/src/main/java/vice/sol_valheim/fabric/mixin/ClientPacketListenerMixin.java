package vice.sol_valheim.fabric.mixin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vice.sol_valheim.PlayerLifecycle;
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Unique private LocalPlayer sol_valheim$oldPlayer;
    @Inject(method = "handleRespawn", at = @At("HEAD"))
    private void beforeRespawn(ClientboundRespawnPacket packet, CallbackInfo ci) {
        var client = Minecraft.getInstance();
        if (client.isSameThread()) sol_valheim$oldPlayer = client.player;
    }
    @Inject(method = "handleRespawn", at = @At("TAIL"))
    private void afterRespawn(ClientboundRespawnPacket packet, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (sol_valheim$oldPlayer != null && player != null) {
            boolean death = sol_valheim$oldPlayer.isDeadOrDying();
            PlayerLifecycle.clonePlayer(sol_valheim$oldPlayer, player, death);
            if (death) player.setHealth(player.getMaxHealth());
        }
        sol_valheim$oldPlayer = null;
    }
}
