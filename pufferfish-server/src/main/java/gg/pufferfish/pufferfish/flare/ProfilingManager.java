package gg.pufferfish.pufferfish.flare;

import co.technove.flare.Flare;
import co.technove.flare.FlareAuth;
import co.technove.flare.FlareBuilder;
import co.technove.flare.exceptions.UserReportableException;
import co.technove.flare.internal.profiling.ProfileType;
import gg.pufferfish.pufferfish.PufferfishConfig;
import gg.pufferfish.pufferfish.PufferfishLogger;
import gg.pufferfish.pufferfish.compat.ServerConfigurations;
import gg.pufferfish.pufferfish.flare.collectors.GCEventCollector;
import gg.pufferfish.pufferfish.flare.collectors.StatCollector;
import gg.pufferfish.pufferfish.flare.collectors.TPSCollector;
import gg.pufferfish.pufferfish.flare.collectors.ThreadCollector;
import gg.pufferfish.pufferfish.flare.collectors.WorldCountCollector;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.VirtualMemory;
import oshi.software.os.OperatingSystem;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Stream;

public class ProfilingManager {

    private static Flare currentFlare;
    private static ScheduledFuture<?> currentTask = null;
    private static final ScheduledExecutorService ses = new ScheduledThreadPoolExecutor(1, r -> {
        Thread t = new Thread(r);
        t.setName("Flare Profiling Manager Thread");
        return t;
    });
    private static final ConcurrentLinkedQueue<Runnable> mainThreadTaskQueue = new ConcurrentLinkedQueue<>();
    private static final TextColor EXCEPTION_COLOR = TextColor.color(218, 144, 147);
    private static final TextColor MAIN_COLOR = TextColor.color(106, 126, 218);
    private static final TextColor HEX = TextColor.color(227, 234, 234);
    private static final Component PREFIX = Component.text()
        .color(NamedTextColor.GRAY)
        .append(Component.text("[", NamedTextColor.DARK_GRAY))
        .append(Component.text("✈", MAIN_COLOR, TextDecoration.BOLD))
        .append(Component.text("]", NamedTextColor.DARK_GRAY))
        .append(Component.text(" "))
        .build();

    public static synchronized boolean isProfiling() {
        return currentFlare != null && currentFlare.isRunning();
    }

    public static synchronized String getProfilingUri() {
        return Objects.requireNonNull(currentFlare).getURI()
            .map(URI::toString)
            .orElse("Flare is not running");
    }

    public static Duration getTimeRan() {
        Flare flare = currentFlare; // copy reference so no need to sync
        if (flare == null) {
            return Duration.ofMillis(0);
        }
        return flare.getCurrentDuration();
    }

    public static synchronized boolean start(ProfileType profileType) throws UserReportableException {
        if (currentFlare != null && !currentFlare.isRunning()) {
            currentFlare = null; // errored out
        }
        if (ProfilingManager.isProfiling()) {
            return false;
        }
        if (Bukkit.isPrimaryThread()) {
            throw new UserReportableException("Profiles should be started off-thread");
        }

        try {
            OperatingSystem os = new SystemInfo().getOperatingSystem();

            SystemInfo systemInfo = new SystemInfo();
            HardwareAbstractionLayer hardware = systemInfo.getHardware();

            CentralProcessor processor = hardware.getProcessor();
            CentralProcessor.ProcessorIdentifier processorIdentifier = processor.getProcessorIdentifier();

            GlobalMemory memory = hardware.getMemory();
            VirtualMemory virtualMemory = memory.getVirtualMemory();

            FlareBuilder builder = new FlareBuilder()
                .withProfileType(profileType)
                .withMemoryProfiling(true)
                .withAuth(FlareAuth.fromTokenAndUrl(PufferfishConfig.getInstance().webServices.token, PufferfishConfig.getInstance().flare.url))

                .withFiles(ServerConfigurations.getCleanCopies())
                .withVersion("Primary Version", Bukkit.getName() + " | " + Bukkit.getVersion())
                .withVersion("Bukkit Version", Bukkit.getBukkitVersion())
                .withVersion("Minecraft Version", Bukkit.getMinecraftVersion())

                .withGraphCategories(CustomCategories.ENTITIES_AND_CHUNKS, CustomCategories.MC_PERF)
                .withCollectors(new TPSCollector(), new WorldCountCollector(ProfilingManager::submitToMainThread), new GCEventCollector(), new StatCollector(), new ThreadCollector())
                .withClassIdentifier(PluginLookup::getPluginForClass)

                .withHardware(new FlareBuilder.HardwareBuilder()
                    .setCoreCount(processor.getPhysicalProcessorCount())
                    .setThreadCount(processor.getLogicalProcessorCount())
                    .setCpuModel(processorIdentifier.getName())
                    .setCpuFrequency(processor.getMaxFreq())

                    .setTotalMemory(memory.getTotal())
                    .setTotalSwap(virtualMemory.getSwapTotal())
                    .setTotalVirtual(virtualMemory.getVirtualMax())
                )

                .withOperatingSystem(new FlareBuilder.OperatingSystemBuilder()
                    .setManufacturer(os.getManufacturer())
                    .setFamily(os.getFamily())
                    .setVersion(os.getVersionInfo().toString())
                    .setBitness(os.getBitness())
                )

                .withExceptionRunnable(() -> {
                    try {
                        currentTask.cancel(true);
                    } catch (Throwable t) {
                        PufferfishLogger.LOGGER.log(Level.WARNING, "Error occurred stopping Flare", t);
                    } finally {
                        currentTask = null;
                    }

                    String profilingUri = FlareCommand.PROFILING_URI;
                    ProfilingManager.broadcastPrefixed(
                        Component.text("An exception happened and profiling has stopped", EXCEPTION_COLOR),
                        Component.text(profilingUri, HEX).clickEvent(ClickEvent.openUrl(profilingUri))
                    );
                });

            currentFlare = builder.build();
        } catch (IOException e) {
            PufferfishLogger.LOGGER.log(Level.WARNING, "Failed to read configuration files:", e);
            throw new UserReportableException("Failed to load configuration files, check logs for further details.");
        }

        try {
            currentFlare.start();
        } catch (IllegalStateException e) {
            PufferfishLogger.LOGGER.log(Level.WARNING, "Error starting Flare:", e);
            throw new UserReportableException("Failed to start Flare, check logs for further details.");
        }

        currentTask = ses.schedule(ProfilingManager::stop, 15, TimeUnit.MINUTES);
        // PufferfishLogger.LOGGER.log(Level.INFO, "Flare has been started: " + getProfilingUri());
        return true;
    }

    public static synchronized boolean stop() {
        if (!ProfilingManager.isProfiling()) {
            return false;
        }
        if (!currentFlare.isRunning()) {
            currentFlare = null;
            return true;
        }
        String profilingUri = ProfilingManager.getProfilingUri();
        ProfilingManager.broadcastPrefixed(
            Component.text("Profiling has been stopped.", MAIN_COLOR),
            Component.text(profilingUri, HEX).clickEvent(ClickEvent.openUrl(profilingUri))
        );
        try {
            currentFlare.stop();
        } catch (IllegalStateException e) {
            PufferfishLogger.LOGGER.log(Level.WARNING, "Error occurred stopping Flare", e);
        }
        currentFlare = null;

        try {
            currentTask.cancel(true);
        } catch (Throwable t) {
            PufferfishLogger.LOGGER.log(Level.WARNING, "Error occurred stopping Flare", t);
        }

        currentTask = null;
        return true;
    }

    private static void submitToMainThread(Runnable task) {
        ProfilingManager.mainThreadTaskQueue.offer(task);
    }

    public static void executeMainThreadTasks() {
        Runnable task;
        while ((task = ProfilingManager.mainThreadTaskQueue.poll()) != null) {
            task.run();
        }
    }

    private static void broadcastPrefixed(Component ...lines) {
        Stream.concat(
                MinecraftServer.getServer().server.getOnlinePlayers().stream(),
                Stream.of(MinecraftServer.getServer().server.getConsoleSender())
            )
            .filter(s -> s.hasPermission("airplane.flare"))
            .forEach(s -> {
                for (Component line : lines) {
                    s.sendMessage(PREFIX.append(line));
                }
            });
    }

}
