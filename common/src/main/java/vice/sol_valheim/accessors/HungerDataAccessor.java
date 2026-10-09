package vice.sol_valheim.accessors;

import net.minecraft.world.entity.player.Player;

/** Associates the vanilla hunger object with its player without a global player map. */
public interface HungerDataAccessor {
    void sol_valheim$bindPlayer(Player player);
}
