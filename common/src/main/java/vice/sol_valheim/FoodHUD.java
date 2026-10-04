package vice.sol_valheim;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import vice.sol_valheim.accessors.PlayerEntityMixinDataAccessor;

public final class FoodHUD {
    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        var client = Minecraft.getInstance();
        var player = client.player;
        if (player == null || client.options.hideGui || player.isCreative() || player.isSpectator()) return;
        var data = ((PlayerEntityMixinDataAccessor) player).sol_valheim$getFoodData();
        if (!data.hasFood()) return;
        int size = SOLValheim.Config.client.useLargeIcons ? 18 : 12;
        int right = graphics.guiWidth() / 2 + 91;

        int top = graphics.guiHeight() - client.gui.rightHeight + 9 - size;
        client.gui.rightHeight += size + 1;
        int offset = 0;
        for (var food : data.ItemEntries) renderSlot(graphics, food, right - (++offset * (size + 2)), top, size, false);
        if (data.DrinkSlot != null) renderSlot(graphics, data.DrinkSlot, right - (++offset * (size + 2)), top, size, true);
    }
    private static void renderSlot(GuiGraphics graphics, ValheimFoodData.EatenFoodItem food, int x, int y, int size, boolean drink) {
        var config = ModConfig.getFoodConfig(food.item);
        if (config == null) return;
        var client = Minecraft.getInstance();
        float fraction = Math.min(1f, (float) food.ticksLeft / config.getTime());
        graphics.fill(x, y, x + size, y + size, drink ? 0x603468A3 : 0x60000000);
        graphics.fill(x, y + size - Math.max(1, (int) (size * fraction)), x + size, y + size,
                food.canEatEarly() ? 0x80FF0A0A : 0x60000000);
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(x + 1, y + 1, 0);
        float scale = (size - 2) / 16f;
        pose.scale(scale, scale, 1);
        graphics.renderItem(new ItemStack(food.item), 0, 0);
        pose.popPose();

        boolean seconds = food.ticksLeft < 1200;
        String text = Integer.toString((int) Math.ceil(food.ticksLeft / (seconds ? 20.0 : 1200.0)));
        pose.pushPose();
        pose.translate(0, 0, 200);
        graphics.drawString(client.font, text, x + size - client.font.width(text), y + size - 8,
                seconds ? 0xFFED3939 : 0xFFFFFFFF);
        pose.popPose();
    }
}
