package vice.sol_valheim.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vice.sol_valheim.GameVersion;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerHealthMixin {
    @Unique private LocalPlayer sol_valheim$healthPlayer;
    @Unique private float sol_valheim$previousMaxHealth;
    @Unique private double sol_valheim$previousFoodHealthModifier;

    @Inject(method = "handleUpdateAttributes", at = @At("HEAD"))
    private void rememberHealth(ClientboundUpdateAttributesPacket packet, CallbackInfo ci) {
        var client = Minecraft.getInstance();
        if (!client.isSameThread() || client.player == null || packet.getEntityId() != client.player.getId()) return;
        sol_valheim$previousMaxHealth = client.player.getMaxHealth();
        sol_valheim$previousFoodHealthModifier = GameVersion.foodHealthModifierAmount(client.player);
    }

    @Inject(method = "handleUpdateAttributes", at = @At("TAIL"))
    private void adjustHealth(ClientboundUpdateAttributesPacket packet, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player == null || packet.getEntityId() != player.getId() || !GameVersion.updatesMaxHealth(packet)) return;
        // Login and respawn initialize health through vanilla's first health update.
        if (sol_valheim$healthPlayer != player) {
            sol_valheim$healthPlayer = player;
            return;
        }
        if (Double.compare(sol_valheim$previousFoodHealthModifier, GameVersion.foodHealthModifierAmount(player)) != 0) {
            // Only rescale the baseline: the following health packet still reports real damage.
            player.setHealth(player.getHealth() * player.getMaxHealth() / sol_valheim$previousMaxHealth);
        }
    }
}
