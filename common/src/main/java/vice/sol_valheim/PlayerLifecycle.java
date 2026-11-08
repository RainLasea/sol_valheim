package vice.sol_valheim;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import me.shedaniel.autoconfig.AutoConfig;
import vice.sol_valheim.accessors.PlayerEntityMixinDataAccessor;
import vice.sol_valheim.mixin.LivingEntityDamageAccessor;

/**
 * Player hooks shared by loader events and mixins, called on the player's game thread.
 * {@link #updateAttributes(Player)} also adjusts current health; {@link #applyFoodModifiers(Player)}
 * only applies modifiers, for initialization before saved or cloned health is restored.
 */
public final class PlayerLifecycle {
    /** Invoked after vanilla has applied and synchronized a successful datapack reload. */
    public static boolean reloadConfig(MinecraftServer server) {
        var holder = AutoConfig.getConfigHolder(ModConfig.class);
        if (!holder.load()) return false;
        SOLValheim.Config = holder.getConfig();
        SOLValheim.Config.validate();
        SOLValheim.generateFoodConfigs();
        for (var player : server.getPlayerList().getPlayers()) syncAll(player);
        return true;
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
        applyFoodModifiers(player);
        player.setHealth(death ? player.getMaxHealth() : original.getHealth());
        updateAttributes(player);
    }
    public static void syncAll(ServerPlayer player) {
        ((PlayerEntityMixinDataAccessor) player).sol_valheim$getFoodData().removeExcludedFoods();
        ((PlayerEntityMixinDataAccessor) player).sol_valheim$getFoodData().refreshMaxSlots();
        vice.sol_valheim.platform.Platform.syncConfig(player);
        updateAttributes(player);
        vice.sol_valheim.platform.Platform.sync(player);
    }
    public static void tick(Player player) {
        if (player.level().isClientSide) return;
        var accessor = (PlayerEntityMixinDataAccessor) player;
        var food = accessor.sol_valheim$getFoodData();
        // Read live tag membership so /reload also invalidates slots in protected modes.
        if (food.removeExcludedFoods()) {
            updateAttributes(player);
            accessor.sol_valheim$syncFoodData();
        }
        // Freeze timers until cloning applies the death penalty once at respawn.
        if (player.isDeadOrDying()) {
            FullnessHooks.tick(player);
            return;
        }
        int previousSlots = food.MaxItemSlots;
        food.refreshMaxSlots();
        if (previousSlots != food.MaxItemSlots) accessor.sol_valheim$syncFoodData();
        if (!player.isCreative() && !player.isSpectator()) {
            boolean wasActive = food.hasFood();
            food.tick(FarmersDelightCompat.hasNourishment(player));
            if (wasActive && !food.hasFood()) accessor.sol_valheim$syncFoodData();
            long sinceHurt = player.level().getGameTime() - ((LivingEntityDamageAccessor) player).getLastDamageStamp();
            int interval = Math.max(1, Math.round(5 * SOLValheim.Config.common.regenSpeedModifier));
            if (sinceHurt > SOLValheim.Config.common.regenDelay && player.tickCount % interval == 0)
                player.heal(food.getRegenSpeed() / 20f);
        } else food.discardHungerCosts();
        updateAttributes(player);
        FullnessHooks.tick(player);

        if (player.tickCount % 20 == 0) accessor.sol_valheim$syncFoodData();
    }
    public static void updateAttributes(Player player) {
        float previousMaxHealth = player.getMaxHealth();
        boolean protectedMode = player.isCreative() || player.isSpectator();
        var health = ((PlayerEntityMixinDataAccessor) player).sol_valheim$getHealthData();
        if (!player.level().isClientSide && protectedMode && !player.isDeadOrDying())
            health.capture(player.getHealth());
        applyFoodModifiers(player);
        if (!player.level().isClientSide) {
            if (previousMaxHealth != player.getMaxHealth())
                player.setHealth(player.getHealth() * player.getMaxHealth() / previousMaxHealth);
            else if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
            if (!protectedMode) health.restore(player);
            // The client must adjust its health baseline before vanilla's health packet arrives.
            if (previousMaxHealth != player.getMaxHealth() && player instanceof ServerPlayer serverPlayer
                    && serverPlayer.connection != null)
                serverPlayer.connection.send(new ClientboundUpdateAttributesPacket(player.getId(),
                        java.util.List.of(player.getAttribute(Attributes.MAX_HEALTH))));
        }
    }
    // Called from Player's constructor before the client's connection and game mode exist.
    public static void initializeClientHealth(Player player) {
        GameVersion.setFoodHealthBonus(player, SOLValheim.Config.common.startingHealth * 2 - 20, false);
    }

    public static void applyFoodModifiers(Player player) {
        double target = FoodApi.getFoodMaxHealth(player);
        if (!player.level().isClientSide) GameVersion.setFoodHealthBonus(player, target - 20, true);
        double speedBonus = !player.isCreative() && !player.isSpectator() && FoodApi.isFull(player)
                ? SOLValheim.Config.common.speedBoost : 0;
        GameVersion.setFoodSpeedBonus(player, speedBonus);
    }

    public static void slept(ServerLevel level, long elapsed) {
        if (!SOLValheim.Config.common.passTicksDuringNight || elapsed <= 0) return;
        for (var player : level.players()) {
            if (player.isDeadOrDying() || player.isCreative() || player.isSpectator()) continue;
            ((PlayerEntityMixinDataAccessor) player).sol_valheim$getFoodData()
                    .passNight(elapsed, FarmersDelightCompat.hasNourishment(player));
            updateAttributes(player);
            vice.sol_valheim.platform.Platform.sync(player);
        }
    }
}
