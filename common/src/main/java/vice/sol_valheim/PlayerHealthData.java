package vice.sol_valheim;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

/** Server-owned snapshot shared by creative and spectator, independent of food data. */
public final class PlayerHealthData {
    private float survivalHealth = Float.NaN;

    public void capture(float health) {
        if (Float.isNaN(survivalHealth) && Float.isFinite(health) && health > 0)
            survivalHealth = health;
    }

    public void restore(Player player) {
        if (Float.isNaN(survivalHealth) || player.isDeadOrDying()) return;
        float health = Math.min(survivalHealth, player.getMaxHealth());
        survivalHealth = Float.NaN;
        if (player.getHealth() != health) player.setHealth(health);
    }

    public CompoundTag save() {
        var tag = new CompoundTag();
        if (!Float.isNaN(survivalHealth)) tag.putFloat("survival_health", survivalHealth);
        return tag;
    }

    public static PlayerHealthData read(CompoundTag tag) {
        var data = new PlayerHealthData();
        if (tag.contains("survival_health", Tag.TAG_ANY_NUMERIC))
            data.capture(tag.getFloat("survival_health"));
        return data;
    }
}
