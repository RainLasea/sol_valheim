package vice.sol_valheim.neoforge.event;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import vice.sol_valheim.ValheimFoodData;

/**
 * Adjusts food health on the server before the starting health and configured cap are applied.
 * Values are health points (two per heart), including the drink effectiveness bonus.
 * Posted on the NeoForge game bus for attribute updates and food fullness queries, so handlers
 * should only calculate a value without changing player health, attributes, or food entries.
 * Clients use the resulting synchronized SOL health modifier and do not fire this event.
 */
public final class FoodHealthEvent extends PlayerEvent {
    private final ValheimFoodData foodData;
    private final float originalFoodHealth;
    private float foodHealth;

    public FoodHealthEvent(Player player, ValheimFoodData foodData, float foodHealth) {
        super(player);
        this.foodData = foodData;
        this.originalFoodHealth = foodHealth;
        this.foodHealth = foodHealth;
    }

    public ValheimFoodData getFoodData() { return foodData; }
    public float getOriginalFoodHealth() { return originalFoodHealth; }
    public float getFoodHealth() { return foodHealth; }

    /** Sets food health; negative values become zero. Starting health is added afterward. */
    public void setFoodHealth(float foodHealth) {
        if (!Float.isFinite(foodHealth)) throw new IllegalArgumentException("Food health must be finite");
        this.foodHealth = Math.max(0, foodHealth);
    }
}
