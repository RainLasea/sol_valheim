package vice.sol_valheim.platform;
import net.minecraft.client.Minecraft;
public final class ClientPlatform {
    public static int rightHeight() { return Minecraft.getInstance().gui.rightHeight; }
    public static void reserveHeight(int height) { Minecraft.getInstance().gui.rightHeight += height; }
}
