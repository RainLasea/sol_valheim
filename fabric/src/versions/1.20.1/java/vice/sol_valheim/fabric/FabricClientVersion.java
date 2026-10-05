package vice.sol_valheim.fabric;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.Minecraft;
import vice.sol_valheim.SOLValheim;
public final class FabricClientVersion {
    public static void registerTooltip() {
        ItemTooltipCallback.EVENT.register((stack, flags, tooltip) -> SOLValheim.addTooltip(stack, tooltip, Minecraft.getInstance().player));
    }
}
