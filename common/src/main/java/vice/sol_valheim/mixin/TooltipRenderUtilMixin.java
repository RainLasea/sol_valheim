package vice.sol_valheim.mixin;

import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import vice.sol_valheim.FoodHUD;
import vice.sol_valheim.SOLValheim;

@Mixin(TooltipRenderUtil.class)
public class TooltipRenderUtilMixin {
    @ModifyVariable(method = "renderFrameGradient", at = @At("HEAD"), argsOnly = true, index = 6)
    private static int sol_valheim$slotBorderTopColor(int color) {
        return FoodHUD.isRenderingSlotBackground() ? SOLValheim.Config.client.slotBorderTopColor : color;
    }

    @ModifyVariable(method = "renderFrameGradient", at = @At("HEAD"), argsOnly = true, index = 7)
    private static int sol_valheim$slotBorderBottomColor(int color) {
        return FoodHUD.isRenderingSlotBackground() ? SOLValheim.Config.client.slotBorderBottomColor : color;
    }
}
