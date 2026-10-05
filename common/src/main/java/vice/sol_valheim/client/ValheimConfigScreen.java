package vice.sol_valheim.client;

import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import vice.sol_valheim.ModConfig;

public final class ValheimConfigScreen extends Screen {
    private final Screen parent;

    public ValheimConfigScreen(Screen parent) {
        super(Component.translatable("text.autoconfig.sol_valheim.title"));
        this.parent = parent;
    }

    @Override protected void init() {
        int x = width / 2 - 100;
        int y = height / 2 - 32;
        addRenderableWidget(Button.builder(Component.translatable("gui.sol_valheim.settings"),
                button -> minecraft.setScreen(AutoConfig.getConfigScreen(ModConfig.class, this).get()))
                .bounds(x, y, 200, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.sol_valheim.position"),
                button -> minecraft.setScreen(new HudPositionScreen(this)))
                .bounds(x, y + 26, 200, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(x, y + 58, 200, 20).build());
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 62, 0xFFFFFFFF);
    }

    @Override public void onClose() {
        minecraft.setScreen(parent);
    }
}
