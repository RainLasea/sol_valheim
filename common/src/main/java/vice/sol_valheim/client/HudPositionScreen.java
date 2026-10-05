package vice.sol_valheim.client;

import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import vice.sol_valheim.FoodHUD;
import vice.sol_valheim.ModConfig;
import vice.sol_valheim.SOLValheim;

public final class HudPositionScreen extends WorldPreviewScreen {
    private final Screen parent;
    private boolean custom;
    private float posX;
    private float posY;
    private boolean draggingHud;
    private double dragX;
    private double dragY;

    public HudPositionScreen(Screen parent) {
        super(Component.translatable("gui.sol_valheim.position"));
        this.parent = parent;
        var config = AutoConfig.getConfigHolder(ModConfig.class).getConfig().client;
        custom = config.customHudPosition;
        posX = config.hudPositionX;
        posY = config.hudPositionY;
    }

    @Override protected void init() {
        int buttonWidth = Math.min(90, (width - 24) / 3);
        int x = (width - buttonWidth * 3 - 12) / 2;
        int y = height - 28;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> save())
                .bounds(x, y, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(x + buttonWidth + 6, y, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.sol_valheim.reset_position"), button -> {
            custom = false;
            posX = 0.5f;
            posY = 1f;
            draggingHud = false;
        }).bounds(x + (buttonWidth + 6) * 2, y, buttonWidth, 20).build());
    }

    private HudLayout layout() {
        var config = SOLValheim.Config.client;
        return HudLayout.create(width, height, FoodHUD.automaticRightHeight(), SOLValheim.Config.common.maxSlots,
                config.useLargeIcons, config.useProgressBar, custom, posX, posY);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        graphics.fill(0, 0, width, 54, 0xB0000000);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFFFF);
        graphics.drawWordWrap(font, Component.translatable("gui.sol_valheim.position_help"), 12, 26, width - 24, 0xFFE0E0E0);
        var layout = layout();
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(0, 0, 300);
        FoodHUD.renderPreview(graphics, layout);
        graphics.renderOutline(layout.x() - 2, layout.y() - 2, layout.width() + 4, layout.height() + 4,
                draggingHud || layout.contains(mouseX, mouseY) ? 0xFFFFD080 : 0xA0FFFFFF);
        if (custom && Math.abs(posX - 0.5f) < 0.001f)
            graphics.fill(width / 2, 54, width / 2 + 1, height - 32, 0x6080C0FF);
        if (custom && Math.abs(posY - 0.5f) < 0.001f)
            graphics.fill(0, height / 2, width, height / 2 + 1, 0x6080C0FF);
        pose.popPose();
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        var layout = layout();
        if (button != 0 || !layout.contains(mouseX, mouseY)) return false;
        dragX = mouseX - layout.x();
        dragY = mouseY - layout.y();
        draggingHud = true;
        setDragging(true);
        return true;
    }

    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!draggingHud || button != 0) return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        moveTo(mouseX - dragX, mouseY - dragY, !hasControlDown());
        return true;
    }

    private void moveTo(double x, double y, boolean snap) {
        var layout = layout();
        int availableX = Math.max(0, width - layout.width());
        int availableY = Math.max(0, height - layout.height());
        custom = true;
        posX = availableX == 0 ? 0 : (float) net.minecraft.util.Mth.clamp(x / availableX, 0, 1);
        posY = availableY == 0 ? 0 : (float) net.minecraft.util.Mth.clamp(y / availableY, 0, 1);
        if (snap && Math.abs(posX - 0.5f) * availableX <= 5) posX = 0.5f;
        if (snap && Math.abs(posY - 0.5f) * availableY <= 5) posY = 0.5f;
    }

    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingHud && button == 0) {
            draggingHud = false;
            setDragging(false);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        int step = hasShiftDown() ? 10 : 1;
        int dx = key == GLFW.GLFW_KEY_LEFT ? -step : key == GLFW.GLFW_KEY_RIGHT ? step : 0;
        int dy = key == GLFW.GLFW_KEY_UP ? -step : key == GLFW.GLFW_KEY_DOWN ? step : 0;
        if (dx != 0 || dy != 0) {
            var layout = layout();
            moveTo(layout.x() + dx, layout.y() + dy, false);
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    private void save() {
        var holder = AutoConfig.getConfigHolder(ModConfig.class);
        holder.getConfig().client.customHudPosition = custom;
        holder.getConfig().client.hudPositionX = posX;
        holder.getConfig().client.hudPositionY = posY;
        holder.save();
        onClose();
    }

    @Override public void onClose() {
        minecraft.setScreen(parent);
    }
}
