package vice.sol_valheim;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Items;
import vice.sol_valheim.accessors.PlayerEntityMixinDataAccessor;
import vice.sol_valheim.client.HudLayout;
import vice.sol_valheim.client.HudPositionScreen;

public final class FoodHUD {
    private static int automaticRightHeight = 39;

    public static int automaticRightHeight() { return automaticRightHeight; }

    public static void render(GuiGraphics graphics) {
        var client = Minecraft.getInstance();
        automaticRightHeight = vice.sol_valheim.platform.ClientPlatform.rightHeight();
        if (client.screen instanceof HudPositionScreen) return;
        var player = client.player;
        if (player == null || client.options.hideGui || player.isCreative() || player.isSpectator()) return;
        var data = ((PlayerEntityMixinDataAccessor) player).sol_valheim$getFoodData();
        if (!data.hasFood()) return;
        var settings = SOLValheim.Config.client;
        var layout = HudLayout.create(graphics.guiWidth(), graphics.guiHeight(), automaticRightHeight,
                SOLValheim.Config.common.maxSlots, settings.useLargeIcons, settings.useProgressBar,
                settings.customHudPosition, settings.hudPositionX, settings.hudPositionY);
        if (!settings.customHudPosition) vice.sol_valheim.platform.ClientPlatform.reserveHeight(layout.height() + 1);
        int offset = 0;
        for (var food : data.ItemEntries) renderSlot(graphics, food, layout.slotX(offset++), layout.y(), layout.iconSize(), false);
        if (data.DrinkSlot != null) renderSlot(graphics, data.DrinkSlot, layout.slotX(offset), layout.y(), layout.iconSize(), true);
    }

    public static void renderPreview(GuiGraphics graphics, HudLayout layout) {
        var samples = new net.minecraft.world.item.Item[]{Items.COOKED_BEEF, Items.BREAD, Items.APPLE, Items.BAKED_POTATO, Items.CARROT};
        for (int i = 0; i < SOLValheim.Config.common.maxSlots; i++) {
            boolean drink = i == SOLValheim.Config.common.maxSlots - 1;
            var item = drink ? Items.POTION : samples[i];
            var config = ModConfig.getFoodConfig(item);
            if (config == null) continue;
            int ticks = i == 2 ? 500 : (int) (config.getTime() * (drink ? 0.8f : 0.75f / (i + 1)));
            renderSlot(graphics, new ValheimFoodData.EatenFoodItem(item, ticks), layout.slotX(i), layout.y(), layout.iconSize(), drink);
        }
    }
    private static void renderSlot(GuiGraphics graphics, ValheimFoodData.EatenFoodItem food, int x, int y, int size, boolean drink) {
        var config = food.getConfig();
        if (config == null) return;
        var client = Minecraft.getInstance();
        float fraction = Math.max(0f, Math.min(1f, (float) food.ticksLeft / config.getTime()));
        graphics.fill(x, y, x + size, y + size, drink ? 0x603468A3 : 0x60000000);
        if (!SOLValheim.Config.client.useProgressBar) {
            graphics.fill(x, y + size - Math.max(1, (int) (size * fraction)), x + size, y + size,
                    food.canEatEarly() ? 0x80FF0A0A : 0x60000000);
        }
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(x + 1, y + 1, 0);
        float scale = (size - 2) / 16f;
        pose.scale(scale, scale, 1);
        graphics.renderItem(food.stack, 0, 0);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0, 200);
        if (SOLValheim.Config.client.useProgressBar) {
            int barWidth = size - 2;
            int remainingWidth = (int) Math.ceil(barWidth * fraction);
            int barY = y + size + 1;
            int color = food.canEatEarly() ? 0xFFED3939 : (drink ? 0xFF6DB9ED : 0xFFDEB665);
            graphics.fill(x + 1, barY, x + size - 1, barY + 2, 0x90000000);
            if (remainingWidth > 0) {
                graphics.fill(x + 1, barY, x + 1 + remainingWidth, barY + 2, color);
            }
        } else {
            boolean seconds = food.ticksLeft < 1200;
            String text = Integer.toString((int) Math.ceil(food.ticksLeft / (seconds ? 20.0 : 1200.0)))
                    + (seconds ? "s" : "m");
            graphics.drawString(client.font, text, x + size - client.font.width(text), y + size - 8,
                    seconds ? 0xFFED3939 : 0xFFFFFFFF);
        }
        pose.popPose();
    }
}
