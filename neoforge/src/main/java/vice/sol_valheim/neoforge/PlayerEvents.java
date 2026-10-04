package vice.sol_valheim.neoforge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import me.shedaniel.autoconfig.AutoConfig;
import vice.sol_valheim.ModConfig;
import vice.sol_valheim.SOLValheim;
import vice.sol_valheim.ValheimFoodData;
import vice.sol_valheim.accessors.PlayerEntityMixinDataAccessor;
import vice.sol_valheim.mixin.LivingEntityDamageAccessor;

public final class PlayerEvents {
    @SubscribeEvent public void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("sol_valheim")
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
    @SubscribeEvent public void finishUsing(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof Player player) SOLValheim.consume(player, event.getItem());
    }
    @SubscribeEvent public void clonePlayer(PlayerEvent.Clone event) {
        var target = (PlayerEntityMixinDataAccessor) event.getEntity();
        target.sol_valheim$setFoodData(event.isWasDeath() ? new ValheimFoodData() : ValheimFoodData.read(
                ((PlayerEntityMixinDataAccessor) event.getOriginal()).sol_valheim$getFoodData().save(new CompoundTag())));
    }
    @SubscribeEvent public void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncAll(player);
    }
    @SubscribeEvent public void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncAll(player);
    }
    @SubscribeEvent public void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncAll(player);
    }
    private static void syncAll(ServerPlayer player) {
        FoodNetworking.syncConfig(player);
        updateAttributes(player);
        FoodNetworking.sync(player);
    }
    @SubscribeEvent public void tick(PlayerTickEvent.Post event) {
        var player = event.getEntity();
        if (player.level().isClientSide) return;
        var accessor = (PlayerEntityMixinDataAccessor) player;
        var food = accessor.sol_valheim$getFoodData();
        food.MaxItemSlots = SOLValheim.Config.common.maxSlots;
        if (player.isDeadOrDying()) {
            if (food.hasFood()) { food.clear(); accessor.sol_valheim$syncFoodData(); }
            return;
        }
        if (!player.isCreative() && !player.isSpectator()) {
            boolean wasActive = food.hasFood();
            food.tick(FarmersDelightCompat.hasNourishment(player));
            if (wasActive && !food.hasFood()) accessor.sol_valheim$syncFoodData();
            long sinceHurt = player.level().getGameTime() - ((LivingEntityDamageAccessor) player).getLastDamageStamp();
            int interval = Math.max(1, Math.round(5 * SOLValheim.Config.common.regenSpeedModifier));
            if (sinceHurt > SOLValheim.Config.common.regenDelay && player.tickCount % interval == 0)
                player.heal(food.getRegenSpeed() / 20f);
        }
        updateAttributes(player);

        if (player.tickCount % 20 == 0) accessor.sol_valheim$syncFoodData();
    }
    public static void updateAttributes(Player player) {
        var health = player.getAttribute(Attributes.MAX_HEALTH);
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (player.isCreative() || player.isSpectator()) {
            health.removeModifier(SOLValheim.HEALTH_MODIFIER);
            speed.removeModifier(SOLValheim.SPEED_MODIFIER);
            return;
        }
        var food = ((PlayerEntityMixinDataAccessor) player).sol_valheim$getFoodData();
        double target = Math.min(40, SOLValheim.Config.common.startingHealth * 2 + food.getTotalFoodNutrition());

        replace(health, new AttributeModifier(SOLValheim.HEALTH_MODIFIER, target - 20, AttributeModifier.Operation.ADD_VALUE));
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
        if (target >= 20 && SOLValheim.Config.common.speedBoost > 0)
            replace(speed, new AttributeModifier(SOLValheim.SPEED_MODIFIER, SOLValheim.Config.common.speedBoost, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        else speed.removeModifier(SOLValheim.SPEED_MODIFIER);
    }
    private static void replace(AttributeInstance attribute, AttributeModifier modifier) {
        var previous = attribute.getModifier(modifier.id());
        if (modifier.equals(previous)) return;
        attribute.removeModifier(modifier.id());
        attribute.addTransientModifier(modifier);
    }
    @SubscribeEvent public void slept(SleepFinishedTimeEvent event) {
        if (!SOLValheim.Config.common.passTicksDuringNight || !(event.getLevel() instanceof ServerLevel level)) return;
        long elapsed = Math.max(0, event.getNewTime() - level.getDayTime());
        for (var player : level.players()) {
            ((PlayerEntityMixinDataAccessor) player).sol_valheim$getFoodData()
                    .passNight(elapsed, FarmersDelightCompat.hasNourishment(player));
            FoodNetworking.sync(player);
        }
    }
}
