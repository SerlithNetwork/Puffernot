package gg.pufferfish.pufferfish.flare;

import co.technove.flare.exceptions.UserReportableException;
import co.technove.flare.internal.profiling.ProfileType;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import gg.pufferfish.pufferfish.PufferfishConfig;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.PaperCommands;
import io.papermc.paper.util.MCUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.server.MinecraftServer;
import org.bukkit.command.CommandSender;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class FlareCommand {

    private static final String BASE_URL = "https://blog.airplane.gg/flare-tutorial/#setting-the-access-token";
    private static final TextColor HEX = TextColor.color(227, 234, 234);
    private static final TextColor MAIN_COLOR = TextColor.color(106, 126, 218);
    private static final Component PREFIX = Component.text()
        .color(NamedTextColor.GRAY)
        .append(Component.text("[", NamedTextColor.DARK_GRAY))
        .append(Component.text("✈", MAIN_COLOR, TextDecoration.BOLD))
        .append(Component.text("]", NamedTextColor.DARK_GRAY))
        .append(Component.text(" "))
        .build();

    public static void init() {

        LiteralCommandNode<CommandSourceStack> command = Commands.literal("flare")
            .requires(s -> s.getSender().hasPermission("airplane.flare"))
            .then(Commands.literal("profiler")
                .then(Commands.literal("start")
                    .then(Commands.literal("--cpu")
                        .executes(ctx -> {
                            FlareCommand.execute(ctx.getSource().getSender(), ProfileType.CPU);
                            return Command.SINGLE_SUCCESS;
                        })
                    )
                    .then(Commands.literal("--alloc")
                        .executes(ctx -> {
                            FlareCommand.execute(ctx.getSource().getSender(), ProfileType.ALLOC);
                            return Command.SINGLE_SUCCESS;
                        })
                    )
                    .then(Commands.literal("--lock")
                        .executes(ctx -> {
                            FlareCommand.execute(ctx.getSource().getSender(), ProfileType.LOCK);
                            return Command.SINGLE_SUCCESS;
                        })
                    )
                    .then(Commands.literal("--wall")
                        .executes(ctx -> {
                            FlareCommand.execute(ctx.getSource().getSender(), ProfileType.WALL);
                            return Command.SINGLE_SUCCESS;
                        })
                    )
                    .executes(ctx -> {
                        FlareCommand.execute(ctx.getSource().getSender(), ProfileType.ITIMER);
                        return Command.SINGLE_SUCCESS;
                    })
                )
                .then(Commands.literal("stop")
                    .executes(ctx -> {
                        CommandSender sender = ctx.getSource().getSender();
                        if (!FlareCommand.isFlareAvailable(sender)) {
                            return Command.SINGLE_SUCCESS;
                        }

                        String profile = ProfilingManager.getProfilingUri();
                        if (ProfilingManager.stop()) {
                            broadcastPrefixed(
                                Component.text("Profiling has been stopped.", MAIN_COLOR),
                                Component.text(profile, HEX).clickEvent(ClickEvent.openUrl(profile))
                            );
                        } else {
                            sendPrefixed(sender,
                                Component.text("Profiling has already been stopped.", HEX)
                            );
                        }
                        return Command.SINGLE_SUCCESS;
                    })
                )
            )
            .then(Commands.literal("status")
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!FlareCommand.isFlareAvailable(sender)) {
                        return Command.SINGLE_SUCCESS;
                    }
                    if (ProfilingManager.isProfiling()) {
                        sendPrefixed(sender,
                            Component.text("Current profile has been ran for " + ProfilingManager.getTimeRan().toString(), HEX)
                        );
                    } else {
                        sendPrefixed(sender,
                            Component.text("Flare is not running.", HEX)
                        );
                    }
                    return Command.SINGLE_SUCCESS;
                })
            )
            .then(Commands.literal("license")
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!FlareCommand.isFlareAvailable(sender)) {
                        return Command.SINGLE_SUCCESS;
                    }
                    MCUtil.scheduleAsyncTask(() -> {
                        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()) {
                            HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                                .uri(URI.create(PufferfishConfig.FLARE.URL + "/license"))
                                .header("Authorization", "token " + PufferfishConfig.WEB_SERVICES.TOKEN)
                                .GET()
                                .build(),
                                HttpResponse.BodyHandlers.ofString()
                            );
                            if (response.statusCode() == 200) {
                                sendPrefixed(sender,
                                    Component.text("Flare Profiler", MAIN_COLOR),
                                    Component.text("Licensed to ", HEX).append(Component.text(response.body(), NamedTextColor.GREEN)).append(Component.text(".", HEX))
                                );
                            } else {
                                sendPrefixed(sender,
                                    Component.text("License provided is invalid!", NamedTextColor.RED),
                                    Component.text("It might be down, or don't contain a license endpoint.", NamedTextColor.DARK_GRAY)
                                );
                            }
                        } catch (Exception ex) {
                            sendPrefixed(sender,
                                Component.text("Failed to connect to Flare server.", NamedTextColor.RED),
                                Component.text("It might be down, or don't contain a license endpoint.", NamedTextColor.DARK_GRAY)
                            );
                        }
                    });
                    return Command.SINGLE_SUCCESS;
                })
            )
            .build();

        PaperCommands.INSTANCE.registerWithFlagsInternal(null, "pufferfish", "pufferfish", command, "Profile your server with Flare", List.of(), Set.of());

    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isFlareAvailable(CommandSender sender) {
        if (PufferfishConfig.WEB_SERVICES.TOKEN.isEmpty()) {
            Component clickable = Component.text(BASE_URL, HEX, TextDecoration.UNDERLINED).clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, BASE_URL));
            sendPrefixed(sender,
                Component.text("Flare currently requires an access token to use.").color(NamedTextColor.GRAY),
                Component.text("To learn more, visit ", NamedTextColor.GRAY).append(clickable)
            );
            return false;
        }
        return true;
    }

    private static void execute(CommandSender sender, final ProfileType profileType) {
        if (!FlareCommand.isFlareAvailable(sender)) {
            return;
        }
        if (!FlareSetup.isSupported()) {
            sendPrefixed(sender,
                Component.text("Profiling is not supported in this environment", NamedTextColor.RED),
                Component.text("Check your startup logs for the error.", NamedTextColor.RED)
            );
            return;
        }
        if (ProfilingManager.isProfiling()) {
            sendPrefixed(sender,
                Component.text("Flare has already been started", MAIN_COLOR),
                Component.text(ProfilingManager.getProfilingUri(), HEX).clickEvent(ClickEvent.openUrl(ProfilingManager.getProfilingUri()))
            );
            return;
        }

        sendPrefixed(sender,
            Component.text("Starting a new flare, please wait...", NamedTextColor.GRAY)
        );
        MCUtil.scheduleAsyncTask(() -> {
            try {
                if (ProfilingManager.start(profileType)) {
                    broadcastPrefixed(
                        Component.text("Flare has been started!", MAIN_COLOR),
                        Component.text("It will run in the background for 15 minutes", NamedTextColor.GRAY),
                        Component.text("or until manually stopped using:", NamedTextColor.GRAY),
                        Component.text("  ").append(Component.text("/flare profiler stop", NamedTextColor.WHITE).clickEvent(ClickEvent.runCommand("flare profiler stop"))),
                        Component.text("Follow its progress here:", NamedTextColor.GRAY),
                        Component.text(ProfilingManager.getProfilingUri(), HEX).clickEvent(ClickEvent.openUrl(ProfilingManager.getProfilingUri()))
                    );
                } else {
                    sendPrefixed(sender,
                        Component.text("Flare has already been started", NamedTextColor.GRAY),
                        Component.text(ProfilingManager.getProfilingUri(), HEX).clickEvent(ClickEvent.openUrl(ProfilingManager.getProfilingUri()))
                    );
                }
            } catch (UserReportableException e) {
                sendPrefixed(sender,
                    Component.text("Flare failed to start: " + e.getUserError(), NamedTextColor.RED)
                );
                if (e.getCause() != null) {
                    MinecraftServer.LOGGER.warn("Flare failed to start", e);
                }
            }
        });
    }

    private static void sendPrefixed(CommandSender sender, Component ...lines) {
        for (Component line : lines) {
            sender.sendMessage(PREFIX.append(line));
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
