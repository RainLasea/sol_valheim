package vice.sol_valheim.fabric.mixin;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import vice.sol_valheim.SOLValheim;
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Redirect(method = "completeUsingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;finishUsingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack consumed(ItemStack stack, Level level, LivingEntity entity) {
        var snapshot = stack.copy();
        var result = stack.finishUsingItem(level, entity);
        if (entity instanceof Player player) SOLValheim.consume(player, snapshot);
        return result;
    }
}
