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
import org.bukkit.command.ConsoleCommandSender;

import java.util.List;
import java.util.Set;

public class FlareCommand {

    private static final String BASE_URL = "https://blog.airplane.gg/flare-tutorial/#setting-the-access-token";
    private static final TextColor HEX = TextColor.fromHexString("#e3eaea");
    private static final Component PREFIX = Component.text()
        .append(Component.text("Flare ✈")
            .color(TextColor.fromHexString("#6a7eda"))
            .decoration(TextDecoration.BOLD, true)
            .append(Component.text(" ", HEX)
                .decoration(TextDecoration.BOLD, false)))
        .asComponent();

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
                        if (ProfilingManager.stop()) {
                            if (!(sender instanceof ConsoleCommandSender)) {
                                sender.sendMessage(PREFIX.append(Component.text("Profiling has been stopped.", HEX)));
                            }
                        } else {
                            sender.sendMessage(PREFIX.append(Component.text("Profiling has already been stopped.", HEX)));
                        }
                        return Command.SINGLE_SUCCESS;
                    })
                )
                .then(Commands.literal("status")
                    .executes(ctx -> {
                        CommandSender sender = ctx.getSource().getSender();
                        if (!FlareCommand.isFlareAvailable(sender)) {
                            return Command.SINGLE_SUCCESS;
                        }
                        if (ProfilingManager.isProfiling()) {
                            sender.sendMessage(PREFIX.append(Component.text("Current profile has been ran for " + ProfilingManager.getTimeRan().toString(), HEX)));
                        } else {
                            sender.sendMessage(PREFIX.append(Component.text("Flare is not running.", HEX)));
                        }
                        return Command.SINGLE_SUCCESS;
                    })
                )
            )
            .build();

        PaperCommands.INSTANCE.registerWithFlagsInternal(null, "pufferfish", "pufferfish", command, "Profile your server with Flare", List.of(), Set.of());

    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isFlareAvailable(CommandSender sender) {
        if (PufferfishConfig.WEB_SERVICES.TOKEN.isEmpty()) {
            Component clickable = Component.text(BASE_URL, HEX, TextDecoration.UNDERLINED).clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, BASE_URL));
            sender.sendMessage(PREFIX.append(Component.text("Flare currently requires an access token to use. To learn more, visit ").color(HEX).append(clickable)));
            return false;
        }
        return true;
    }

    private static void execute(CommandSender sender, final ProfileType profileType) {
        if (!FlareCommand.isFlareAvailable(sender)) {
            return;
        }
        if (!FlareSetup.isSupported()) {
            sender.sendMessage(PREFIX.append(
                Component.text("Profiling is not supported in this environment, check your startup logs for the error.", NamedTextColor.RED)));
            return;
        }
        if (ProfilingManager.isProfiling()) {
            sender.sendMessage(PREFIX.append(Component
                .text("Flare has already been started: " + ProfilingManager.getProfilingUri(), HEX)
                .clickEvent(ClickEvent.openUrl(ProfilingManager.getProfilingUri()))
            ));
            return;
        }

        MCUtil.scheduleAsyncTask(() -> {
            try {
                if (ProfilingManager.start(profileType)) {
                    if (!(sender instanceof ConsoleCommandSender)) {
                        sender.sendMessage(PREFIX.append(Component
                            .text("Flare has been started: " + ProfilingManager.getProfilingUri(), HEX)
                            .clickEvent(ClickEvent.openUrl(ProfilingManager.getProfilingUri()))
                        ));
                        sender.sendMessage(PREFIX.append(Component.text("  Run '/flare stop' to stop the Flare.", HEX)));
                    }
                } else {
                    sender.sendMessage(PREFIX.append(Component
                        .text("Flare has already been started: " + ProfilingManager.getProfilingUri(), HEX)
                        .clickEvent(ClickEvent.openUrl(ProfilingManager.getProfilingUri()))
                    ));
                }
            } catch (UserReportableException e) {
                sender.sendMessage(Component.text("Flare failed to start: " + e.getUserError(), NamedTextColor.RED));
                if (e.getCause() != null) {
                    MinecraftServer.LOGGER.warn("Flare failed to start", e);
                }
            }
        });
    }

}
