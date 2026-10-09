package vice.sol_valheim;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

public final class GameVersion {
    private GameVersion() {}
    public static FoodProperties makeFood(int nutrition, float saturation) { return new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build(); }
    public static FoodProperties baseFoodProperties(ItemStack stack, LivingEntity entity) { return stack.get(net.minecraft.core.component.DataComponents.FOOD); }
    public static int nutrition(FoodProperties food) { return food.nutrition(); }
    public static float saturation(FoodProperties food) { return food.saturation(); }
    public static CompoundTag saveStack(ItemStack stack, HolderLookup.Provider registries) { return (CompoundTag) stack.save(registries); }
    public static ItemStack readStack(CompoundTag tag, HolderLookup.Provider registries) { return ItemStack.parse(registries, tag).orElse(ItemStack.EMPTY); }
    public static boolean sameFoodComponents(ItemStack first, ItemStack second) {
        var a = first.copyWithCount(1); var b = second.copyWithCount(1);
        a.remove(net.minecraft.core.component.DataComponents.CUSTOM_NAME); b.remove(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
        a.remove(net.minecraft.core.component.DataComponents.LORE); b.remove(net.minecraft.core.component.DataComponents.LORE);
        return ItemStack.isSameItemSameComponents(a, b);
    }
    public static MobEffectInstance effectInstance(Holder<MobEffect> effect, int ticks, int amplifier) { return new MobEffectInstance(effect, ticks, amplifier); }
    public static boolean hasEffect(Player player, Holder<MobEffect> effect) { return player.hasEffect(effect); }
    private static void replace(AttributeInstance attribute, AttributeModifier modifier) {
        replace(attribute, modifier, false);
    }
    private static void replace(AttributeInstance attribute, AttributeModifier modifier, boolean permanent) {
        if (modifier.equals(attribute.getModifier(modifier.id()))) return;
        attribute.removeModifier(modifier.id());
        if (permanent) attribute.addPermanentModifier(modifier);
        else attribute.addTransientModifier(modifier);
    }
    // Called from Player's constructor before the client's connection and game mode exist.
    public static void initializeClientHealth(Player player) {
        replace(player.getAttribute(Attributes.MAX_HEALTH), new AttributeModifier(
                SOLValheim.HEALTH_MODIFIER, SOLValheim.Config.common.startingHealth * 2 - 20,
                AttributeModifier.Operation.ADD_VALUE));
    }
    public static void updateAttributes(Player player) {
        var health = player.getAttribute(Attributes.MAX_HEALTH);
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        var food = ((vice.sol_valheim.accessors.PlayerEntityMixinDataAccessor) player).sol_valheim$getFoodData();
        double target = Math.min(SOLValheim.Config.common.maxHealth * 2, SOLValheim.Config.common.startingHealth * 2 + food.getTotalFoodNutrition());
        replace(health, new AttributeModifier(SOLValheim.HEALTH_MODIFIER, target - 20, AttributeModifier.Operation.ADD_VALUE),
                !player.level().isClientSide);
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
        if (player.isCreative() || player.isSpectator()) {
            speed.removeModifier(SOLValheim.SPEED_MODIFIER); return;
        }
        if (target >= 20 && SOLValheim.Config.common.speedBoost > 0)
            replace(speed, new AttributeModifier(SOLValheim.SPEED_MODIFIER, SOLValheim.Config.common.speedBoost, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        else speed.removeModifier(SOLValheim.SPEED_MODIFIER);
    }
}
