package gg.pufferfish.pufferfish.flare;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.PluginClassLoader;
import org.jspecify.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class PluginLookup {

    private static final Cache<@NonNull String, @NonNull String> PLUGIN_NAME_CACHE = CacheBuilder.newBuilder()
        .expireAfterAccess(1, TimeUnit.MINUTES)
        .maximumSize(1024)
        .build();
    private static final Cache<@NonNull Boolean, @NonNull Collection<ClassLoader>> CLASS_LOADER_CACHE = CacheBuilder.newBuilder() // Yes, boolean key, trust
        .expireAfterAccess(1, TimeUnit.MINUTES)
        .build();

    public static Optional<String> getPluginForClass(String name) {
        if (name.endsWith(".so") ||
            name.startsWith("net.minecraft.") || name.startsWith("java.") || name.startsWith("com.mojang.") ||
            name.startsWith("com.google.") || name.startsWith("it.unimi.") || name.startsWith("sun.") || name.startsWith("javax.") ||
            name.startsWith("jdk.") || name.startsWith("io.papermc.") || name.startsWith("gg.pufferfish.") || name.startsWith("co.technove.") ||
            name.startsWith("ca.spottedleaf.") || name.startsWith("com.sun.") || name.startsWith("org.jline.") || name.startsWith("org.bukkit.") ||
            name.startsWith("org.spigotmc.") || name.startsWith("com.destroystokyo.paper") || name.startsWith("co.aikar.") || name.startsWith("com.velocitypowered.")
        ) {
            return Optional.empty();
        }

        String existing = PLUGIN_NAME_CACHE.getIfPresent(name);
        if (existing != null) {
            return Optional.ofNullable(existing.isEmpty() ? null : existing);
        }

        Collection<ClassLoader> classLoaders;
        try {
            classLoaders = CLASS_LOADER_CACHE.get(true, PluginLookup::loadClassLoaders);
        } catch (ExecutionException ignore) {
            return Optional.empty();
        }

        ClassLoader classLoader = PluginLookup.matchClassLoader(classLoaders, name);
        if (classLoader == null) {
            return Optional.empty();
        }

        String pluginName = "";
        if (classLoader instanceof PluginClassLoader bukkitLoader) {
            pluginName = bukkitLoader.airplane$getPlugin().getName();
        } else if (classLoader instanceof PaperPluginClassLoader paperLoader) {
            JavaPlugin plugin = paperLoader.airplane$getLoadedJavaPlugin();
            if (plugin != null) {
                pluginName = plugin.getName();
            }
        }
        PLUGIN_NAME_CACHE.put(name, pluginName);
        return Optional.ofNullable(pluginName.isEmpty() ? null : pluginName);
    }

    private static @Nullable ClassLoader matchClassLoader(Collection<ClassLoader> classLoaders, String className) {
        for (ClassLoader classLoader : classLoaders) {
            try {
                classLoader.loadClass(className);
                return classLoader;
            } catch (ClassNotFoundException ignore) {}
        }
        return null;
    }

    private static Collection<ClassLoader> loadClassLoaders() {
        return Arrays.stream(Bukkit.getPluginManager().getPlugins()).map(i -> i.getClass().getClassLoader()).toList();
    }

}
