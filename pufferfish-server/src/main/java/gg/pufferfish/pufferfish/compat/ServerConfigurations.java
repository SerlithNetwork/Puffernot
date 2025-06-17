package gg.pufferfish.pufferfish.compat;

import com.google.common.io.Files;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
        "pufferfish.yml",
        "jellyfish.yml",
        "aurora.yml"
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
            File f = new File(file);
            if (f.exists()) files.put(file, getCleanCopy(file));
        }
        MinecraftServer server = MinecraftServer.getServer();
        for (ServerLevel serverLevel : server.getAllLevels()) {
            File worldDir = serverLevel.getWorld().getWorldFolder();
            File paperWorldConfig = new File(worldDir, "paper-world.yml");
            String cleanConfig = getCleanCopy(paperWorldConfig.getPath());
            if (!cleanConfig.isEmpty()) {
                files.put(paperWorldConfig.getPath(), cleanConfig);
            }
        }
        return files;
    }

    @SuppressWarnings("deprecation")
    public static String getCleanCopy(String configName) throws IOException {
        File file = new File(configName);

        switch (Files.getFileExtension(configName)) {
            case "properties": {
                Properties properties = new Properties();
                try (FileInputStream inputStream = new FileInputStream(file)) {
                    properties.load(inputStream);
                }
                for (String hiddenConfig : properties.stringPropertyNames()) {
                    if (matchesRegex(hiddenConfig, hiddenConfigEntries)) properties.remove(hiddenConfig);
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
                try {
                    configuration.load(file);
                } catch (InvalidConfigurationException e) {
                    throw new IOException(e);
                }
                configuration.options().header(null);
                for (String key : configuration.getKeys(true)) {
                    if (matchesRegex(key, hiddenConfigEntries)) {
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
                throw new IllegalArgumentException("Bad file type " + configName);
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
