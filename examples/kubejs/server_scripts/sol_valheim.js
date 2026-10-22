// Copy into kubejs/server_scripts/; no SOL Valheim KubeJS addon is required.
const SOLFood = Java.loadClass('vice.sol_valheim.FoodApi')

PlayerEvents.loggedIn(event => {
  event.player.persistentData.putBoolean('sol_valheim_example_full', false)
})

PlayerEvents.tick(event => {
  const player = event.player
  const full = SOLFood.isFull(player)
  const previous = player.persistentData.getBoolean('sol_valheim_example_full')
  if (full && !previous) {
    player.tell(Text.translate('message.sol_valheim.full_enter'))
    // Add your modpack's effects, quests or rewards here.
  } else if (!full && previous) {
    player.tell(Text.translate('message.sol_valheim.full_exit'))
  }
  player.persistentData.putBoolean('sol_valheim_example_full', full)
})

// In your own quest / advancement callback:
// SOLFood.setMaxSlots(event.player, 4)
// SOLFood.resetMaxSlots(event.player)
