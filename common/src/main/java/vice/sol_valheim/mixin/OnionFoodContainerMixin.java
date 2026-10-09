package vice.sol_valheim.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vice.sol_valheim.accessors.LunchContainerAccessor;
import vice.sol_valheim.accessors.PlayerEntityMixinDataAccessor;

@Pseudo
@Mixin(targets = "team.creative.solonion.common.item.foodcontainer.FoodContainerItem", remap = false)
public abstract class OnionFoodContainerMixin implements LunchContainerAccessor {
    @Shadow(remap = false) public abstract ItemStack getActualFood(Player player, ItemStack stack);

    @Override public ItemStack sol_valheim$getActualFood(Player player, ItemStack container) {
        return getActualFood(player, container);
    }

    @Inject(method = "processRightClick", at = @At("HEAD"), cancellable = true, remap = false)
    private void valheimUseContainer(Level level, Player player, InteractionHand hand,
                                    CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        var stack = player.getItemInHand(hand);
        if (getActualFood(player, stack).isEmpty()) {
            cir.setReturnValue(InteractionResultHolder.pass(stack));
        } else if (player.isCreative() || player.isSpectator()
                || ((PlayerEntityMixinDataAccessor) player).sol_valheim$getFoodData().canEat(stack, player)) {
            player.startUsingItem(hand);
            cir.setReturnValue(InteractionResultHolder.consume(stack));
        } else cir.setReturnValue(InteractionResultHolder.fail(stack));
    }
}
