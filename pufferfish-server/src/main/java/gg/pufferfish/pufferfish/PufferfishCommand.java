package gg.pufferfish.pufferfish;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.PaperCommands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import java.util.List;
import java.util.Set;

public class PufferfishCommand {

    private static final Component PREFIX = MiniMessage.miniMessage().deserialize("<#12fff6><b>Pufferfish »</b></#12fff6> ");
    private static final Component FEEDBACK_RELOAD_SUCCESS = PREFIX.append(Component.text("Pufferfish configuration has been reloaded.", NamedTextColor.WHITE));
    private static final Component FEEDBACK_RELOAD_FAILED = PREFIX.append(Component.text("Failed to reload.", NamedTextColor.RED));
    private static Component FEEDBACK_CURRENT_VERSION = null;

    public static void init() {

        LiteralCommandNode<CommandSourceStack> command = Commands.literal("pufferfish")
            .requires(s -> s.getSender().hasPermission("bukkit.command.pufferfish"))
            .then(Commands.literal("reload")
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    MinecraftServer console = MinecraftServer.getServer();
                    try {
                        PufferfishConfig.INSTANCE.load();
                    } catch (Exception e) {
                        sender.sendMessage(FEEDBACK_RELOAD_FAILED);
                        console.server.getLogger().severe(e.getMessage());
                        return Command.SINGLE_SUCCESS;
                    }
                    console.server.reloadCount++;
                    sender.sendMessage(FEEDBACK_RELOAD_SUCCESS);
                    return Command.SINGLE_SUCCESS;
                })
            )
            .then(Commands.literal("version")
                .executes(ctx -> {
                    if (FEEDBACK_CURRENT_VERSION == null) {
                        FEEDBACK_CURRENT_VERSION = PREFIX.append(Component.text("This server is running " + Bukkit.getName() + " (Puffernot) version " + Bukkit.getVersion() + " (Implementing API version " + Bukkit.getBukkitVersion() + ")", NamedTextColor.WHITE));
                    }
                    ctx.getSource().getSender().sendMessage(FEEDBACK_CURRENT_VERSION);
                    return Command.SINGLE_SUCCESS;
                })
            )
            .build();

        PaperCommands.INSTANCE.registerWithFlagsInternal(null, "pufferfish", "pufferfish", command, "Pufferfish related commands", List.of(), Set.of());

    }

}
