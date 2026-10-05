package vice.sol_valheim.platform;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
public final class ClientPlatform {
    public static int rightHeight() {
        var player = Minecraft.getInstance().player;
        if (player == null) return 39;
        int height = 39;
        if (player.getAirSupply() < player.getMaxAirSupply() || player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)) height += 10;
        if (player.getVehicle() instanceof LivingEntity vehicle)
            height += ((Math.min(30, (int) Math.ceil(vehicle.getMaxHealth() / 2)) + 9) / 10) * 10;
        return height;
    }
    public static void reserveHeight(int height) {}
}
