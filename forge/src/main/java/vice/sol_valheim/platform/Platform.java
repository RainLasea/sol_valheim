package vice.sol_valheim.platform;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.food.FoodProperties;
import vice.sol_valheim.forge.FoodNetworking;
public final class Platform {
    public static boolean mayFly(Player player) { return player.getAbilities().mayfly; }
    public static boolean serverRunning() { return net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer() != null; }
    public static FoodProperties foodProperties(ItemStack stack, LivingEntity entity) { return stack.getFoodProperties(entity); }
    public static void sync(Player player) { FoodNetworking.sync(player); }
    public static void syncConfig(ServerPlayer player) { FoodNetworking.syncConfig(player); }
}
