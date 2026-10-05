package vice.sol_valheim.neoforge;

import com.google.gson.Gson;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import vice.sol_valheim.ModConfig;
import vice.sol_valheim.SOLValheim;
import vice.sol_valheim.ValheimFoodData;
import vice.sol_valheim.accessors.PlayerEntityMixinDataAccessor;

public final class FoodNetworking {
    private static final Gson GSON = new Gson();
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(FoodState.TYPE, FoodState.CODEC, (payload, context) ->
                ((PlayerEntityMixinDataAccessor) context.player()).sol_valheim$setFoodData(ValheimFoodData.read(payload.data(), context.player().registryAccess())));
        registrar.playToClient(CommonConfig.TYPE, CommonConfig.CODEC, (payload, context) -> applyConfig(payload));
    }
    public static void applyConfig(CommonConfig payload) {
        var effective = new ModConfig();
        effective.common = GSON.fromJson(payload.json(), ModConfig.Common.class);
        effective.client = me.shedaniel.autoconfig.AutoConfig.getConfigHolder(ModConfig.class).getConfig().client;

        SOLValheim.Config = effective;
        SOLValheim.Config.validate();
        SOLValheim.remoteCommon = effective.common;
    }
    public static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer)
            PacketDistributor.sendToPlayer(serverPlayer, new FoodState(((PlayerEntityMixinDataAccessor) player).sol_valheim$getFoodData().save(new CompoundTag(), player.registryAccess())));
    }
    public static void syncConfig(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new CommonConfig(GSON.toJson(SOLValheim.Config.common)));
    }
    public record FoodState(CompoundTag data) implements CustomPacketPayload {
        public static final Type<FoodState> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SOLValheim.MOD_ID, "food_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, FoodState> CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, FoodState::data, FoodState::new);
        @Override public Type<FoodState> type() { return TYPE; }
    }
    public record CommonConfig(String json) implements CustomPacketPayload {
        public static final Type<CommonConfig> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SOLValheim.MOD_ID, "common_config"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CommonConfig> CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(1048576), CommonConfig::json, CommonConfig::new);
        @Override public Type<CommonConfig> type() { return TYPE; }
    }
}
