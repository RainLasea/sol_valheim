package vice.sol_valheim.forge;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import vice.sol_valheim.ConfigSync;
import vice.sol_valheim.accessors.PlayerEntityMixinDataAccessor;
public final class FoodNetworking {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation("sol_valheim", "main"), () -> "1", "1"::equals, "1"::equals);
    public static void register() {
        CHANNEL.messageBuilder(FoodState.class, 0, NetworkDirection.PLAY_TO_CLIENT)
            .encoder((payload, buffer) -> buffer.writeNbt(payload.data()))
            .decoder(buffer -> new FoodState(buffer.readNbt()))
            .consumerMainThread((payload, context) -> FoodClientNetworking.food(payload.data())).add();
        CHANNEL.messageBuilder(CommonConfig.class, 1, NetworkDirection.PLAY_TO_CLIENT)
            .encoder((payload, buffer) -> buffer.writeUtf(payload.json(), 1048576))
            .decoder(buffer -> new CommonConfig(buffer.readUtf(1048576)))
            .consumerMainThread((payload, context) -> ConfigSync.apply(payload.json())).add();
    }
    public static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer)
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new FoodState(((PlayerEntityMixinDataAccessor) player).sol_valheim$getFoodData().save(new CompoundTag(), player.level().registryAccess())));
    }
    public static void syncConfig(ServerPlayer player) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new CommonConfig(ConfigSync.encode())); }
    public record FoodState(CompoundTag data) {}
    public record CommonConfig(String json) {}
}
