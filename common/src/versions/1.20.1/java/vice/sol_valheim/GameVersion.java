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
    public static FoodProperties makeFood(int nutrition, float saturation) { return new FoodProperties.Builder().nutrition(nutrition).saturationMod(saturation).build(); }
    private static final java.util.UUID HEALTH_ID = java.util.UUID.nameUUIDFromBytes("sol_valheim:food_health".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    private static final java.util.UUID SPEED_ID = java.util.UUID.nameUUIDFromBytes("sol_valheim:food_speed".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    public static FoodProperties baseFoodProperties(ItemStack stack, LivingEntity entity) { return stack.getItem().getFoodProperties(); }
    public static int nutrition(FoodProperties food) { return food.getNutrition(); }
    public static float saturation(FoodProperties food) { return food.getNutrition() * food.getSaturationModifier() * 2f; }
    public static CompoundTag saveStack(ItemStack stack, HolderLookup.Provider registries) { return stack.save(new CompoundTag()); }
    public static ItemStack readStack(CompoundTag tag, HolderLookup.Provider registries) { return ItemStack.of(tag); }
    public static boolean sameFoodComponents(ItemStack first, ItemStack second) {
        var a = first.copyWithCount(1); var b = second.copyWithCount(1);
        stripCosmetics(a); stripCosmetics(b);
        return ItemStack.isSameItemSameTags(a, b);
    }
    private static void stripCosmetics(ItemStack stack) {
        var tag = stack.getTag();
        if (tag == null || !tag.contains("display", 10)) return;
        var display = tag.getCompound("display"); display.remove("Name"); display.remove("Lore");
        if (display.isEmpty()) tag.remove("display");
        if (tag.isEmpty()) stack.setTag(null);
    }
    public static MobEffectInstance effectInstance(Holder<MobEffect> effect, int ticks, int amplifier) { return new MobEffectInstance(effect.value(), ticks, amplifier); }
    public static boolean hasEffect(Player player, Holder<MobEffect> effect) { return player.hasEffect(effect.value()); }
    private static void replace(AttributeInstance attribute, AttributeModifier modifier) {
        replace(attribute, modifier, false);
    }
    private static void replace(AttributeInstance attribute, AttributeModifier modifier, boolean permanent) {
        var old = attribute.getModifier(modifier.getId());
        if (old != null && old.getAmount() == modifier.getAmount() && old.getOperation() == modifier.getOperation()) return;
        attribute.removeModifier(modifier.getId());
        if (permanent) attribute.addPermanentModifier(modifier);
        else attribute.addTransientModifier(modifier);
    }
    // Called from Player's constructor before the client's connection and game mode exist.
    public static void initializeClientHealth(Player player) {
        replace(player.getAttribute(Attributes.MAX_HEALTH), new AttributeModifier(
                HEALTH_ID, "sol_valheim.food_health", SOLValheim.Config.common.startingHealth * 2 - 20,
                AttributeModifier.Operation.ADDITION));
    }
    public static void updateAttributes(Player player) {
        var health = player.getAttribute(Attributes.MAX_HEALTH);
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        var food = ((vice.sol_valheim.accessors.PlayerEntityMixinDataAccessor) player).sol_valheim$getFoodData();
        double target = Math.min(SOLValheim.Config.common.maxHealth * 2, SOLValheim.Config.common.startingHealth * 2 + food.getTotalFoodNutrition());
        replace(health, new AttributeModifier(HEALTH_ID, "sol_valheim.food_health", target - 20, AttributeModifier.Operation.ADDITION),
                !player.level().isClientSide);
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
        if (player.isCreative() || player.isSpectator()) {
            speed.removeModifier(SPEED_ID); return;
        }
        if (target >= 20 && SOLValheim.Config.common.speedBoost > 0)
            replace(speed, new AttributeModifier(SPEED_ID, "sol_valheim.food_speed", SOLValheim.Config.common.speedBoost, AttributeModifier.Operation.MULTIPLY_BASE));
        else speed.removeModifier(SPEED_ID);
    }
}
