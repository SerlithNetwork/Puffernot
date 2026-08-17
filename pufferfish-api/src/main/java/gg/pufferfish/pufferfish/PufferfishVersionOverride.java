package gg.pufferfish.pufferfish;

import io.papermc.paper.ServerBuildInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@NullMarked
@ApiStatus.Internal
public class PufferfishVersionOverride {
    private PufferfishVersionOverride() {
    }

    private static final Component HEADER = MiniMessage.miniMessage().deserialize(
        "<gradient:#0069FF:#96CCFF:#0069FF><st>    </st>[ <white><b>Pufferfish</b></white> ]<st>                                                    </st></gradient>"
    );
    private static final TextColor COLOR_PRIMARY = TextColor.color(0x00, 0xAA, 0xFF);

    public static Component getVersionMessage() {
        final ServerBuildInfo build = ServerBuildInfo.buildInfo();
        final String version = build.minecraftVersionName();
        final String buildNumber = build.buildNumber().stream().mapToObj(String::valueOf).findFirst().orElse("DEV");
        final Optional<String> commit = build.gitCommit();

        final List<Component> components = new ArrayList<>(4);
        components.add(Component.empty());
        components.add(HEADER);

        Component next = Component.textOfChildren(
            Component.text("Minecraft", COLOR_PRIMARY),
            Component.space(),
            Component.text(version, NamedTextColor.WHITE),
            Component.space(),
            Component.text("build", COLOR_PRIMARY),
            Component.space(),
            Component.text(buildNumber, NamedTextColor.WHITE)
        );
        if (commit.isPresent()) {
            next = Component.textOfChildren(
                next,
                Component.space(),
                Component.text("commit", COLOR_PRIMARY),
                Component.space(),
                Component.text(commit.get(), NamedTextColor.WHITE, TextDecoration.UNDERLINED)
            );
        }
        components.add(next);

        final String javaVersion = System.getProperty("java.specification.version", "Unknown");
        final String javaVendorVersion = System.getProperty("java.vendor.version", "Unknown");
        final String javaVendor = System.getProperty("java.vendor", "Unknown");
        components.add(Component.textOfChildren(
            Component.text("Java", COLOR_PRIMARY),
            Component.space(),
            Component.text(javaVersion, NamedTextColor.WHITE),
            Component.space(),
            Component.text("(", NamedTextColor.DARK_GRAY),
            Component.text(javaVendorVersion, NamedTextColor.WHITE),
            Component.text(")", NamedTextColor.DARK_GRAY),
            Component.space(),
            Component.text("provided by", COLOR_PRIMARY),
            Component.space(),
            Component.text(javaVendor, NamedTextColor.WHITE, TextDecoration.UNDERLINED)
        ));

        return Component.join(JoinConfiguration.newlines(), components);
    }

}
