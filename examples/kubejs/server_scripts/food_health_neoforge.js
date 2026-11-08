// NeoForge 1.21.1: adjust health points before SOL adds starting health and applies maxHealth.
// Two health points equal one heart. foodHealth already includes the drink bonus.
// This event can run several times per tick. Only change its result; leave player state alone.
NativeEvents.onEvent('vice.sol_valheim.neoforge.event.FoodHealthEvent', event => {
  if (event.entity.isCreative() || event.entity.isSpectator()) return

  // Example: reduce the food contribution by 25%, rounded down to a whole health point.
  event.setFoodHealth(Math.floor(event.foodHealth * 0.75))

  // event.originalFoodHealth: unmodified contribution from food and drink.
  // event.foodData: live SOL food entries, available for reading diet categories or timers.
  // SOL applies the result to health, fullness, and speed bonuses, then synchronizes it.
})
