package vice.sol_valheim;

import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Jankson;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.serializer.ConfigSerializer;
import me.shedaniel.autoconfig.util.Utils;

import java.nio.file.Files;
import java.nio.file.Path;

/** JSON5 input with compact float values and constructor defaults for missing settings. */
public final class ReadableConfigSerializer<T extends ConfigData> implements ConfigSerializer<T> {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Path path;
    private final Class<T> configClass;

    public ReadableConfigSerializer(Config definition, Class<T> configClass) {
        this(Utils.getConfigFolder().resolve(definition.name() + ".json5"), configClass);
    }

    public ReadableConfigSerializer(Path path, Class<T> configClass) {
        this.path = path;
        this.configClass = configClass;
    }

    @Override public T deserialize() throws SerializationException {
        if (!Files.exists(path)) return createDefault();
        try {
            var json = Jankson.builder().build().load(path.toFile());
            var config = GSON.fromJson(json.toJson(false, false), configClass);
            if (config == null) throw new IllegalArgumentException("Expected a configuration object: " + path);
            return config;
        } catch (Exception exception) {
            throw new SerializationException(exception);
        }
    }

    @Override public void serialize(T config) throws SerializationException {
        try {
            Files.createDirectories(path.toAbsolutePath().getParent());
            Files.writeString(path, GSON.toJson(config) + System.lineSeparator());
        } catch (Exception exception) {
            throw new SerializationException(exception);
        }
    }

    @Override public T createDefault() {
        return Utils.constructUnsafely(configClass);
    }
}
