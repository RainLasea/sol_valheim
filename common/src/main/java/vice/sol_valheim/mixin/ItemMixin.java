package vice.sol_valheim.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vice.sol_valheim.accessors.PlayerEntityMixinDataAccessor;

@Mixin(Item.class)
public class ItemMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void valheimCanEat(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        var stack = player.getItemInHand(hand);
        var food = stack.get(DataComponents.FOOD);

        if (food == null || food.canAlwaysEat() || player.isCreative() || player.isSpectator()) return;
        var data = ((PlayerEntityMixinDataAccessor) player).sol_valheim$getFoodData();
        if (stack.is(Items.ROTTEN_FLESH) || data.canEat(stack.getItem())) {
            player.startUsingItem(hand);
            cir.setReturnValue(InteractionResultHolder.consume(stack));
        } else cir.setReturnValue(InteractionResultHolder.fail(stack));
    }
}
