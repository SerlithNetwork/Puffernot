package gg.pufferfish.pufferfish.compat;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class ServerConfigurations {

    public static final String[] configurationFiles = new String[]{
        "server.properties",
        "bukkit.yml",
        "spigot.yml",
        "config/paper-global.yml",
        "config/paper-world-defaults.yml",
        "pufferfish.yml"
    };

    private static final String[] hiddenConfigEntries = new String[]{
        "database",
        "proxies.velocity.secret",
        "web-services.token",
        "sentry-dsn",
        "server-ip",
        "motd",
        "resource-pack",
        "level-seed",
        "rcon.password",
        "rcon.ip",
        "feature-seeds",
        "world-settings.*.feature-seeds",
        "world-settings.*.seed-*",
        "seed-*"
    };

    public static Map<String, String> getCleanCopies() throws IOException {
        Map<String, String> files = new HashMap<>(configurationFiles.length);
        for (String file : configurationFiles) {
            Path path = Path.of(file);
            if (Files.exists(path)) {
                files.put(file, ServerConfigurations.getCleanCopy(path));
            }
        }
        MinecraftServer server = MinecraftServer.getServer();
        for (ServerLevel serverLevel : server.getAllLevels()) {
            Path worldPath = serverLevel.getWorld().getWorldPath();
            Path paperWorldConfig = worldPath.resolve("paper-world.yml");
            String cleanConfig = ServerConfigurations.getCleanCopy(paperWorldConfig);
            if (!cleanConfig.isEmpty()) {
                files.put(paperWorldConfig.toString(), cleanConfig);
            }
        }
        return files;
    }

    @SuppressWarnings("deprecation")
    public static String getCleanCopy(Path configPath) throws IOException {
        switch (com.google.common.io.Files.getFileExtension(configPath.getFileName().toString())) {
            case "properties": {
                Properties properties = new Properties();
                try (InputStream inputStream = Files.newInputStream(configPath)) {
                    properties.load(inputStream);
                }
                for (String hiddenConfig : properties.stringPropertyNames()) {
                    if (ServerConfigurations.matchesRegex(hiddenConfig, hiddenConfigEntries)) properties.remove(hiddenConfig);
                }
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                properties.store(outputStream, "");
                return Arrays.stream(outputStream.toString()
                        .split("\n"))
                    .filter(line -> !line.startsWith("#"))
                    .collect(Collectors.joining("\n"));
            }
            case "yml": {
                YamlConfiguration configuration = new YamlConfiguration();
                try (BufferedReader reader = Files.newBufferedReader(configPath)) {
                    configuration.load(reader);
                } catch (InvalidConfigurationException e) {
                    throw new IOException(e);
                }
                configuration.options().header(null);
                for (String key : configuration.getKeys(true)) {
                    if (ServerConfigurations.matchesRegex(key, hiddenConfigEntries)) {
                        configuration.set(key, null);
                    }
                }
                if (configuration.getKeys(false).size() == 1) {
                    return "";
                } else {
                    return configuration.saveToString();
                }
            }
            default:
                throw new IllegalArgumentException("Bad file type " + configPath);
        }
    }

    public static boolean matchesRegex(String key, String[] patterns) {
        for (String configKey : patterns) {
            String regex = configKey.replace(".", "\\.").replace("*", ".*");
            if (key.matches(regex)) {
                return true;
            }
        }
        return false;
    }

}
