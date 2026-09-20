package gg.pufferfish.pufferfish.flare;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.papermc.paper.plugin.provider.classloader.ConfiguredPluginClassLoader;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@NullMarked
public class PluginLookup {

    private static final Cache<String, String> PLUGIN_NAME_CACHE = CacheBuilder.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(2))
        .maximumSize(1024)
        .build();
    private static final Cache<Boolean, Map<Plugin, ClassLoader>> CLASS_LOADER_CACHE = CacheBuilder.newBuilder() // Yes, boolean key, trust
        .expireAfterAccess(Duration.ofMinutes(2))
        .build();

    public static Optional<String> getPluginForClass(String name) {
        if (name.endsWith(".so")
            || name.startsWith("net.minecraft.") || name.startsWith("java.") || name.startsWith("com.mojang.")
            || name.startsWith("com.google.") || name.startsWith("it.unimi.") || name.startsWith("sun.") || name.startsWith("javax.")
            || name.startsWith("jdk.") || name.startsWith("io.papermc.") || name.startsWith("gg.pufferfish.") || name.startsWith("co.technove.")
            || name.startsWith("ca.spottedleaf.") || name.startsWith("com.sun.") || name.startsWith("org.jline.") || name.startsWith("org.bukkit.")
            || name.startsWith("org.spigotmc.") || name.startsWith("com.destroystokyo.paper") || name.startsWith("co.aikar.")
            || name.startsWith("io.netty.") || name.startsWith("com.velocitypowered.")
        ) {
            return Optional.empty();
        }

        String existing = PLUGIN_NAME_CACHE.getIfPresent(name);
        if (existing != null) {
            return Optional.ofNullable(existing.isEmpty() ? null : existing);
        }

        Map<Plugin, ClassLoader> classLoaders;
        try {
            classLoaders = CLASS_LOADER_CACHE.get(true, PluginLookup::loadClassLoaders);
        } catch (ExecutionException ignore) {
            return Optional.empty();
        }

        Plugin plugin = PluginLookup.matchPlugin(classLoaders, name);
        if (plugin == null) {
            return Optional.empty();
        }

        String pluginName = plugin.getName();
        PLUGIN_NAME_CACHE.put(name, pluginName);
        return Optional.ofNullable(pluginName.isEmpty() ? null : pluginName);
    }

    private static @Nullable Plugin matchPlugin(Map<Plugin, ClassLoader> classLoaders, String className) {
        for (Map.Entry<Plugin, ClassLoader> entry : classLoaders.entrySet()) {
            boolean matchesPluginClassLoader = false;
            boolean matchesServerClassLoader = true;
            if (entry.getValue() instanceof ConfiguredPluginClassLoader pluginClassLoader) {
                try {
                    pluginClassLoader.loadClass(className, true, false, true);
                    matchesPluginClassLoader = true;
                }  catch (ClassNotFoundException ignore) {}
            }
            try {
                Class.forName(className);
            } catch (ClassNotFoundException ignore) {
                matchesServerClassLoader = false;
            }
            if (matchesPluginClassLoader && !matchesServerClassLoader) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static Map<Plugin, ClassLoader> loadClassLoaders() {
        return Arrays.stream(Bukkit.getPluginManager().getPlugins()).collect(Collectors.toMap(i -> i, i -> i.getClass().getClassLoader()));
    }

}
