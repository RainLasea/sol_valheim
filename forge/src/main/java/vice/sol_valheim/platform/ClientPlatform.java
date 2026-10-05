package vice.sol_valheim.platform;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.gui.overlay.ForgeGui;
public final class ClientPlatform {
    public static int rightHeight() { return ((ForgeGui) Minecraft.getInstance().gui).rightHeight; }
    public static void reserveHeight(int height) { ((ForgeGui) Minecraft.getInstance().gui).rightHeight += height; }
}
