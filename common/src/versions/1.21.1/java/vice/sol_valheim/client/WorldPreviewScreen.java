package vice.sol_valheim.client;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public abstract class WorldPreviewScreen extends Screen {
    protected WorldPreviewScreen(Component title) { super(title); }
    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (minecraft.level == null) super.renderBackground(graphics, mouseX, mouseY, partialTicks);
    }
}
