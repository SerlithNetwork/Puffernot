package gg.pufferfish.pufferfish;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;
import me.biquaternions.spark.common.SparkStyle;
import me.lucko.spark.paper.common.platform.serverconfig.ConfigParser;
import me.lucko.spark.paper.common.platform.serverconfig.ExcludedConfigFilter;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@NullMarked
public class PufferfishSparkStyle implements SparkStyle {

    private final TextColor titleColor = TextColor.color(255, 145, 0);
    private final TextColor dataColor = TextColor.color(155, 255, 255);

    private final TextColor prefixColor = TextColor.color(255, 200, 0);

    private final TextColor healthGoodColor = TextColor.color(155, 255, 255);
    private final TextColor healthMediumColor = TextColor.color(255, 190, 0);
    private final TextColor healthBadColor = TextColor.color(255, 20, 20);

    @Override
    public TextColor getTitleColor() {
        return this.titleColor;
    }

    @Override
    public TextColor getDataColor() {
        return this.dataColor;
    }

    @Override
    public TextColor getPrefixColor() {
        return this.prefixColor;
    }

    @Override
    public String getPrefix() {
        return "\uD83D\uDC21";
    }

    @Override
    public TextColor getHealthGoodColor() {
        return this.healthGoodColor;
    }

    @Override
    public TextColor getHealthMediumColor() {
        return this.healthMediumColor;
    }

    @Override
    public TextColor getHealthBadColor() {
        return this.healthBadColor;
    }

    @Override
    public Map<String, ConfigParser> getPlatformConfigs() {
        return Map.of();
    }

    @Override
    public List<String> getPlatformHiddenPaths() {
        return List.of(
            "web-services.token",
            "sentry-dsn"
        );
    }

    // Taken from spark
    protected static class YamlConfigParser implements ConfigParser {
        public static final YamlConfigParser INSTANCE = new YamlConfigParser();
        protected static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(MemorySection.class, (JsonSerializer<MemorySection>) (obj, type, ctx) -> ctx.serialize(obj.getValues(false)))
            .create();

        @Override
        public @Nullable JsonElement load(String file, ExcludedConfigFilter filter) throws IOException {
            Map<String, Object> values = this.parse(Paths.get(file));
            if (values == null) {
                return null;
            }

            return filter.apply(GSON.toJsonTree(values));
        }

        @Override
        public Map<String, Object> parse(BufferedReader reader) throws IOException {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(reader);
            return config.getValues(false);
        }
    }

    // Paper 1.19+ split config layout
    protected static class SplitYamlConfigParser extends YamlConfigParser {
        public static final SplitYamlConfigParser INSTANCE = new SplitYamlConfigParser();

        @Override
        public @Nullable JsonElement load(String group, ExcludedConfigFilter filter) throws IOException {
            String prefix = group.replace("/", "");

            Path configDir = Paths.get("config");
            if (!Files.exists(configDir)) {
                return null;
            }

            JsonObject root = new JsonObject();

            for (Map.Entry<String, Path> entry : getNestedFiles(configDir, prefix).entrySet()) {
                String fileName = entry.getKey();
                Path path = entry.getValue();

                Map<String, Object> values = this.parse(path);
                if (values == null) {
                    continue;
                }

                // apply the filter individually to each nested file
                root.add(fileName, filter.apply(GSON.toJsonTree(values)));
            }

            return root;
        }

        private static Map<String, Path> getNestedFiles(Path configDir, String prefix) {
            Map<String, Path> files = new LinkedHashMap<>();
            files.put("global.yml", configDir.resolve(prefix + "-global.yml"));
            files.put("world-defaults.yml", configDir.resolve(prefix + "-world-defaults.yml"));
            for (World world : Bukkit.getWorlds()) {
                files.put(world.getName() + ".yml", world.getWorldFolder().toPath().resolve(prefix + "-world.yml"));
            }
            return files;
        }
    }

}
