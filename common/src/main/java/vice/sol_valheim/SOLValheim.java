package vice.sol_valheim;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.PartitioningSerializer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import vice.sol_valheim.accessors.PlayerEntityMixinDataAccessor;
import vice.sol_valheim.accessors.LunchContainerAccessor;
import java.util.List;
import java.util.Locale;

public final class SOLValheim {
    public static final String MOD_ID = "sol_valheim";
    public static ModConfig Config = new ModConfig();
    public static ModConfig.Common remoteCommon;
    public static final ResourceLocation HEALTH_MODIFIER = ResourceLocation.tryParse(MOD_ID + ":food_health");
    public static final ResourceLocation SPEED_MODIFIER = ResourceLocation.tryParse(MOD_ID + ":food_speed");

    public static void init() {
        AutoConfig.register(ModConfig.class, PartitioningSerializer.wrap(ReadableConfigSerializer::new));
        Config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        Config.validate();
        AutoConfig.getConfigHolder(ModConfig.class).registerSaveListener((holder, config) -> {
            config.validate();
            if (remoteCommon != null && !vice.sol_valheim.platform.Platform.serverRunning()) {
                var effective = new ModConfig();
                effective.common = remoteCommon;
                effective.client = config.client;
                Config = effective;
            } else Config = config;
            return net.minecraft.world.InteractionResult.PASS;
        });
    }

    public static void generateFoodConfigs() {

        BuiltInRegistries.ITEM.forEach(ModConfig::getFoodConfig);
        AutoConfig.getConfigHolder(ModConfig.class).save();
    }

    public static void consume(Player player, ItemStack stack) {
        if (player.level().isClientSide || player.isCreative() || player.isSpectator()) return;
        // Onion emits a nested finish event for the actual food. Ignore the outer
        // container event, whose contents may already be empty or select another food.
        if (stack.getItem() instanceof LunchContainerAccessor) return;
        var container = player.getUseItem();
        boolean fromContainer = container.getItem() instanceof LunchContainerAccessor;
        var food = fromContainer ? ModConfig.getContainerFoodConfig(container, stack, player)
                : ModConfig.getFoodConfig(stack, player);
        var accessor = (PlayerEntityMixinDataAccessor) player;
        var data = accessor.sol_valheim$getFoodData();
        if (stack.is(Items.ROTTEN_FLESH)) {
            data.clear();
        } else if (data.eatConfiguredItem(fromContainer ? container : stack, food)) {
            for (var effect : food.extraEffects) {
                var holder = effect.getEffect();
                if (holder == null || !Float.isFinite(effect.duration) || effect.duration <= 0) continue;
                int ticks = (int) Math.min(Integer.MAX_VALUE, food.getTime() * (double) effect.duration);
                player.addEffect(GameVersion.effectInstance(holder, Math.max(1, ticks), Math.max(0, effect.amplifier - 1)));
            }
        }
        accessor.sol_valheim$syncFoodData();
    }

    public static void addTooltip(ItemStack stack, List<Component> tooltip) {
        addTooltip(stack, tooltip, null);
    }

    public static void addTooltip(ItemStack stack, List<Component> tooltip, Player player) {
        if (stack.is(Items.ROTTEN_FLESH)) {
            tooltip.add(Component.translatable("tooltip.sol_valheim.rotten_flesh").withStyle(ChatFormatting.GREEN));
            return;
        }
        var food = ModConfig.getFoodConfig(stack, player);
        if (food == null) return;
        tooltip.add(Component.translatable("tooltip.sol_valheim.hearts", number(food.getHearts() / 2f)).withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.sol_valheim.regen", number(food.getHealthRegen())).withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable("tooltip.sol_valheim.duration", number(food.getTime() / 1200f)).withStyle(ChatFormatting.GOLD));
        for (var effect : food.extraEffects) {
            var holder = effect.getEffect();
            if (holder != null) tooltip.add(Component.translatable("tooltip.sol_valheim.effect", holder.value().getDisplayName(), effect.amplifier).withStyle(ChatFormatting.GREEN));
        }
        if (stack.getUseAnimation() == UseAnim.DRINK)
            tooltip.add(Component.translatable("tooltip.sol_valheim.refreshing").withStyle(ChatFormatting.AQUA));
    }

    private static String number(float value) {
        return value == (int) value ? Integer.toString((int) value) : String.format(Locale.ROOT, "%.1f", value);
    }
}
