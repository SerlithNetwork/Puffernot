package gg.pufferfish.pufferfish.flare;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.PluginClassLoader;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class PluginLookup {
    private static final Cache<String, String> pluginNameCache = CacheBuilder.newBuilder()
        .expireAfterAccess(1, TimeUnit.MINUTES)
        .maximumSize(1024)
        .build();

    public static Optional<String> getPluginForClass(String name) {
        if (name.startsWith("net.minecraft") || name.startsWith("java.") || name.startsWith("com.mojang") ||
            name.startsWith("com.google") || name.startsWith("it.unimi") || name.startsWith("sun")) {
            return Optional.empty();
        }

        String existing = pluginNameCache.getIfPresent(name);
        if (existing != null) {
            return Optional.ofNullable(existing.isEmpty() ? null : existing);
        }

        try {
            Class<?> clazz = Class.forName(name);
            ClassLoader loader = clazz.getClassLoader();

            String pluginName = "";
            if (loader instanceof PluginClassLoader bukkitLoader) {
                pluginName = bukkitLoader.airplane$getPlugin().getName();
            } else if (loader instanceof PaperPluginClassLoader paperLoader) {
                JavaPlugin plugin = paperLoader.airplane$getLoadedJavaPlugin();
                if (plugin != null) {
                    pluginName = plugin.getName();
                }
            }
            pluginNameCache.put(name, pluginName);
            return Optional.ofNullable(pluginName.isEmpty() ? null : pluginName);

        } catch (Exception ignored) {
            return Optional.empty();
        }
    }
}
