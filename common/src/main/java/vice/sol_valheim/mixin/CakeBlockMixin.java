package vice.sol_valheim.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vice.sol_valheim.SOLValheim;
import vice.sol_valheim.accessors.PlayerEntityMixinDataAccessor;

@Mixin(CakeBlock.class)
public class CakeBlockMixin {
    @Redirect(method = "eat", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;canEat(Z)Z"))
    private static boolean canEatCake(Player player, boolean always) {
        return player.isCreative() || ((PlayerEntityMixinDataAccessor) player).sol_valheim$getFoodData().canEat(Items.CAKE);
    }
    @Inject(method = "eat", at = @At("RETURN"))
    private static void eatenCake(LevelAccessor level, BlockPos pos, BlockState state, Player player, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue() == InteractionResult.SUCCESS && !level.isClientSide())
            SOLValheim.consume(player, Items.CAKE.getDefaultInstance());
    }
}
