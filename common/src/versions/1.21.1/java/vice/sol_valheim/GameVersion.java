package vice.sol_valheim;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

/** Minecraft API differences for shared food logic, compiled separately for each target version. */
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
    private static void replace(AttributeInstance attribute, AttributeModifier modifier, boolean permanent) {
        if (modifier.equals(attribute.getModifier(modifier.id()))) return;
        attribute.removeModifier(modifier.id());
        if (permanent) attribute.addPermanentModifier(modifier);
        else attribute.addTransientModifier(modifier);
    }
    public static double foodHealthModifierAmount(Player player) {
        var modifier = player.getAttribute(Attributes.MAX_HEALTH).getModifier(SOLValheim.HEALTH_MODIFIER);
        return modifier == null ? Double.NaN : modifier.amount();
    }
    public static boolean updatesMaxHealth(ClientboundUpdateAttributesPacket packet) {
        return packet.getValues().stream().anyMatch(value -> value.attribute().equals(Attributes.MAX_HEALTH));
    }
    public static void setFoodHealthBonus(Player player, double bonus, boolean permanent) {
        replace(player.getAttribute(Attributes.MAX_HEALTH), new AttributeModifier(
                SOLValheim.HEALTH_MODIFIER, bonus, AttributeModifier.Operation.ADD_VALUE), permanent);
    }
    public static void setFoodSpeedBonus(Player player, double bonus) {
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (bonus > 0)
            replace(speed, new AttributeModifier(SOLValheim.SPEED_MODIFIER, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), false);
        else speed.removeModifier(SOLValheim.SPEED_MODIFIER);
    }
}
