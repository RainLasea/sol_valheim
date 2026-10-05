package vice.sol_valheim.fabric;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import vice.sol_valheim.ConfigSync;
import vice.sol_valheim.accessors.PlayerEntityMixinDataAccessor;
public final class FoodNetworking {
    public static void register() {
        PayloadTypeRegistry.playS2C().register(FoodState.TYPE, FoodState.CODEC);
        PayloadTypeRegistry.playS2C().register(CommonConfig.TYPE, CommonConfig.CODEC);
    }
    public static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer)
            ServerPlayNetworking.send(serverPlayer, new FoodState(((PlayerEntityMixinDataAccessor) player).sol_valheim$getFoodData().save(new CompoundTag(), player.level().registryAccess())));
    }
    public static void syncConfig(ServerPlayer player) { ServerPlayNetworking.send(player, new CommonConfig(ConfigSync.encode())); }
    public record FoodState(CompoundTag data) implements CustomPacketPayload {
        public static final Type<FoodState> TYPE = new Type<>(ResourceLocation.tryParse("sol_valheim:food_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, FoodState> CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, FoodState::data, FoodState::new);
        @Override public Type<FoodState> type() { return TYPE; }
    }
    public record CommonConfig(String json) implements CustomPacketPayload {
        public static final Type<CommonConfig> TYPE = new Type<>(ResourceLocation.tryParse("sol_valheim:common_config"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CommonConfig> CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(1048576), CommonConfig::json, CommonConfig::new);
        @Override public Type<CommonConfig> type() { return TYPE; }
    }
}
