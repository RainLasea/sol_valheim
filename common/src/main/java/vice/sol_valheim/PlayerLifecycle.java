package vice.sol_valheim;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import me.shedaniel.autoconfig.AutoConfig;
import vice.sol_valheim.ModConfig;
import vice.sol_valheim.SOLValheim;
import vice.sol_valheim.ValheimFoodData;
import vice.sol_valheim.accessors.PlayerEntityMixinDataAccessor;
import vice.sol_valheim.mixin.LivingEntityDamageAccessor;

public final class PlayerLifecycle {
    public static void commands(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sol_valheim")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload").executes(context -> {
                    var holder = AutoConfig.getConfigHolder(ModConfig.class);
                    if (!holder.load()) {
                        context.getSource().sendFailure(Component.translatable("commands.sol_valheim.reload.failed"));
                        return 0;
                    }
                    SOLValheim.Config = holder.getConfig();
                    SOLValheim.Config.validate();
                    SOLValheim.generateFoodConfigs();
                    for (var player : context.getSource().getServer().getPlayerList().getPlayers()) syncAll(player);
                    context.getSource().sendSuccess(() -> Component.translatable("commands.sol_valheim.reload.success"), true);
                    return 1;
                })));
    }
    public static void clonePlayer(Player original, Player player, boolean death) {
        var target = (PlayerEntityMixinDataAccessor) player;
        var food = ValheimFoodData.read(
                ((PlayerEntityMixinDataAccessor) original).sol_valheim$getFoodData().save(new CompoundTag(), original.level().registryAccess()),
                player.level().registryAccess());
        if (death) food.applyDeathPenalty();
        target.sol_valheim$setFoodData(food);
        target.sol_valheim$setHealthData(death ? new PlayerHealthData() : PlayerHealthData.read(
                ((PlayerEntityMixinDataAccessor) original).sol_valheim$getHealthData().save()));
        // Initialize attributes before current health, without capturing the new player's default health.
        GameVersion.updateAttributes(player);
        player.setHealth(death ? player.getMaxHealth() : original.getHealth());
        updateAttributes(player);
    }
    public static void syncAll(ServerPlayer player) {
        if (!player.isCreative() && !player.isSpectator())
            ((PlayerEntityMixinDataAccessor) player).sol_valheim$getFoodData().setMaxSlots(SOLValheim.Config.common.maxSlots);
        vice.sol_valheim.platform.Platform.syncConfig(player);
        updateAttributes(player);
        vice.sol_valheim.platform.Platform.sync(player);
    }
    public static void tick(Player player) {
        if (player.level().isClientSide) return;
        var accessor = (PlayerEntityMixinDataAccessor) player;
        var food = accessor.sol_valheim$getFoodData();
        // Freeze timers until cloning applies the death penalty once at respawn.
        if (player.isDeadOrDying()) return;
        if (!player.isCreative() && !player.isSpectator()) {
            food.setMaxSlots(SOLValheim.Config.common.maxSlots);
            boolean wasActive = food.hasFood();
            food.tick(FarmersDelightCompat.hasNourishment(player));
            if (wasActive && !food.hasFood()) accessor.sol_valheim$syncFoodData();
            long sinceHurt = player.level().getGameTime() - ((LivingEntityDamageAccessor) player).getLastDamageStamp();
            int interval = Math.max(1, Math.round(5 * SOLValheim.Config.common.regenSpeedModifier));
            if (sinceHurt > SOLValheim.Config.common.regenDelay && player.tickCount % interval == 0)
                player.heal(food.getRegenSpeed() / 20f);
        } else food.discardHungerCosts();
        updateAttributes(player);

        if (player.tickCount % 20 == 0) accessor.sol_valheim$syncFoodData();
    }
    public static void updateAttributes(Player player) {
        boolean protectedMode = player.isCreative() || player.isSpectator();
        var health = ((PlayerEntityMixinDataAccessor) player).sol_valheim$getHealthData();
        if (!player.level().isClientSide && protectedMode && !player.isDeadOrDying())
            health.capture(player.getHealth());
        GameVersion.updateAttributes(player);
        if (!player.level().isClientSide && !protectedMode) health.restore(player);
    }
    public static void slept(ServerLevel level, long elapsed) {
        if (!SOLValheim.Config.common.passTicksDuringNight) return;
        elapsed = Math.max(0, elapsed);
        for (var player : level.players()) {
            if (player.isDeadOrDying() || player.isCreative() || player.isSpectator()) continue;
            ((PlayerEntityMixinDataAccessor) player).sol_valheim$getFoodData()
                    .passNight(elapsed, FarmersDelightCompat.hasNourishment(player));
            updateAttributes(player);
            vice.sol_valheim.platform.Platform.sync(player);
        }
    }
}
