package vice.sol_valheim;
import com.google.gson.Gson;
import me.shedaniel.autoconfig.AutoConfig;
public final class ConfigSync {
    private static final Gson GSON = new Gson();
    public static String encode() { return GSON.toJson(SOLValheim.Config.common); }
    public static void apply(String json) {
        var effective = new ModConfig();
        effective.common = GSON.fromJson(json, ModConfig.Common.class);
        effective.client = AutoConfig.getConfigHolder(ModConfig.class).getConfig().client;
        SOLValheim.Config = effective; effective.validate(); SOLValheim.remoteCommon = effective.common;
    }
    public static void disconnect() {
        var holder = AutoConfig.getConfigHolder(ModConfig.class);
        SOLValheim.remoteCommon = null; holder.load(); SOLValheim.Config = holder.getConfig(); SOLValheim.Config.validate();
    }
}
