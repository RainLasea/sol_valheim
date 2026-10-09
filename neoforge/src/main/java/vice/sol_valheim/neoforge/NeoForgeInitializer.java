package vice.sol_valheim.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import vice.sol_valheim.SOLValheim;

@Mod(SOLValheim.MOD_ID)
public final class NeoForgeInitializer {
    public NeoForgeInitializer(IEventBus bus) {
        SOLValheim.init();
        bus.addListener(FoodNetworking::register);
        NeoForge.EVENT_BUS.register(new PlayerEvents());
    }
}
