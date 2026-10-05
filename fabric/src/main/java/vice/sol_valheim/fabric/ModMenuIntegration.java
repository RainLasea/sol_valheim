package vice.sol_valheim.fabric;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import vice.sol_valheim.client.ValheimConfigScreen;
public final class ModMenuIntegration implements ModMenuApi {
    @Override public ConfigScreenFactory<?> getModConfigScreenFactory() { return ValheimConfigScreen::new; }
}
