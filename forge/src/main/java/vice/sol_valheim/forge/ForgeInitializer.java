package vice.sol_valheim.forge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.common.MinecraftForge;
import vice.sol_valheim.SOLValheim;
@Mod(SOLValheim.MOD_ID)
public final class ForgeInitializer {
    public ForgeInitializer() {
        SOLValheim.init(); FoodNetworking.register();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(new PlayerEvents());
    }
    private void setup(FMLCommonSetupEvent event) { event.enqueueWork(SOLValheim::generateFoodConfigs); }
}
