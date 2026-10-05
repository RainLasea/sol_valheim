package vice.sol_valheim.fabric;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import vice.sol_valheim.ConfigSync;
import vice.sol_valheim.accessors.PlayerEntityMixinDataAccessor;
public final class FoodNetworking {
    public static final ResourceLocation FOOD = new ResourceLocation("sol_valheim", "food_state");
    public static final ResourceLocation CONFIG = new ResourceLocation("sol_valheim", "common_config");
    public static void register() {}
    public static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            var buffer = PacketByteBufs.create();
            buffer.writeNbt(((PlayerEntityMixinDataAccessor) player).sol_valheim$getFoodData().save(new CompoundTag(), player.level().registryAccess()));
            ServerPlayNetworking.send(serverPlayer, FOOD, buffer);
        }
    }
    public static void syncConfig(ServerPlayer player) {
        var buffer = PacketByteBufs.create(); buffer.writeUtf(ConfigSync.encode(), 1048576);
        ServerPlayNetworking.send(player, CONFIG, buffer);
    }
}
