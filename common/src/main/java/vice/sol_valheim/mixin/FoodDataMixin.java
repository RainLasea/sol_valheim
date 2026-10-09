package vice.sol_valheim.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vice.sol_valheim.HungerCompatibility;
import vice.sol_valheim.accessors.HungerDataAccessor;

@Mixin(FoodData.class)
public abstract class FoodDataMixin implements HungerDataAccessor {
    @Shadow private int foodLevel;
    @Shadow private float saturationLevel;
    @Shadow private float exhaustionLevel;
    @Shadow private int tickTimer;
    @Unique private Player sol_valheim$player;
    @Unique private int sol_valheim$previousFood;
    @Unique private float sol_valheim$previousExhaustion;

    @Override public void sol_valheim$bindPlayer(Player player) {
        if (sol_valheim$player == player) return;
        sol_valheim$player = player;
        sol_valheim$normalize();
    }

    @Inject(method = "setFoodLevel", at = @At("HEAD"))
    private void rememberFood(int value, CallbackInfo ci) {
        sol_valheim$previousFood = foodLevel;
        if (foodLevel < 20) HungerCompatibility.consume(sol_valheim$player, 20.0 - foodLevel, 0);
    }

    @Inject(method = "setFoodLevel", at = @At("TAIL"))
    private void consumeFood(int value, CallbackInfo ci) {
        HungerCompatibility.consume(sol_valheim$player, (double) sol_valheim$previousFood - foodLevel, 0);
        if (foodLevel != 20) foodLevel = 20;
    }

    @Inject(method = "addExhaustion", at = @At("HEAD"))
    private void rememberAddedExhaustion(float value, CallbackInfo ci) {
        if (exhaustionLevel > 0) HungerCompatibility.consume(sol_valheim$player, 0, exhaustionLevel);
    }

    @Inject(method = "addExhaustion", at = @At("TAIL"))
    private void consumeExhaustion(float value, CallbackInfo ci) {
        // Use the requested amount, including costs larger than vanilla's 40-point buffer.
        HungerCompatibility.consume(sol_valheim$player, 0, value);
        if (exhaustionLevel != 0) exhaustionLevel = 0;
    }

    @Inject(method = "setExhaustion", at = @At("HEAD"))
    private void rememberExhaustion(float value, CallbackInfo ci) {
        sol_valheim$previousExhaustion = exhaustionLevel;
        if (exhaustionLevel > 0) HungerCompatibility.consume(sol_valheim$player, 0, exhaustionLevel);
    }

    @Inject(method = "setExhaustion", at = @At("TAIL"))
    private void consumeSetExhaustion(float value, CallbackInfo ci) {
        HungerCompatibility.consume(sol_valheim$player, 0, (double) exhaustionLevel - sol_valheim$previousExhaustion);
        if (exhaustionLevel != 0) exhaustionLevel = 0;
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void loadHunger(CompoundTag tag, CallbackInfo ci) {
        // Loading an old hunger value is not a new gameplay cost.
        sol_valheim$normalize();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void valheimHunger(Player player, CallbackInfo ci) {
        sol_valheim$bindPlayer(player);
        // Cheap fallback for mods writing fields directly. Normal ticks perform no restoration.
        if (foodLevel < 20 || exhaustionLevel > 0)
            HungerCompatibility.consume(player, 20.0 - foodLevel, exhaustionLevel);
        sol_valheim$normalize();
    }

    @ModifyVariable(method = "tick", at = @At("STORE"), ordinal = 0)
    private boolean disableVanillaRegeneration(boolean naturalRegeneration) {
        // Leave tick running so other mods' injections still execute. SOL supplies regeneration.
        return false;
    }

    @Unique private void sol_valheim$normalize() {
        if (foodLevel != 20) foodLevel = 20;
        if (saturationLevel != 0) saturationLevel = 0;
        if (exhaustionLevel != 0) exhaustionLevel = 0;
        if (tickTimer != 0) tickTimer = 0;
    }
}
