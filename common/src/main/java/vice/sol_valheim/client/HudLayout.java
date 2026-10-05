package vice.sol_valheim.client;

/** Screen-relative bounds shared by the HUD and its position editor. */
public record HudLayout(int x, int y, int width, int height, int iconSize) {
    public static HudLayout create(int screenWidth, int screenHeight, int rightHeight, int slots,
                                   boolean largeIcons, boolean progressBar, boolean custom, float posX, float posY) {
        int size = largeIcons ? 18 : 12;
        int width = slots * (size + 2) - 2;
        int height = size + (progressBar ? 4 : 0);
        int availableX = Math.max(0, screenWidth - width);
        int availableY = Math.max(0, screenHeight - height);
        int x = custom ? Math.round(availableX * posX) : screenWidth / 2 + 91 - slots * (size + 2);
        int y = custom ? Math.round(availableY * posY) : screenHeight - rightHeight + 9 - height;
        return new HudLayout(net.minecraft.util.Mth.clamp(x, 0, availableX), net.minecraft.util.Mth.clamp(y, 0, availableY), width, height, size);
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public int slotX(int index) {
        return x + width - iconSize - index * (iconSize + 2);
    }
}
