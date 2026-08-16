package gg.pufferfish.pufferfish;

import com.destroystokyo.paper.PaperVersionFetcher;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import io.papermc.paper.ServerBuildInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.TimeUnit;

import static io.papermc.paper.ServerBuildInfo.StringRepresentation.VERSION_SIMPLE;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.TextColor.color;
import static net.minecraft.server.MinecraftServer.COMPONENT_LOGGER;

public class PufferfishVersionFetcher extends PaperVersionFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(PufferfishVersionFetcher.class.getSimpleName());
    private static final long CACHE_TIME = TimeUnit.MINUTES.toMillis(30);

    private static final String REPOSITORY = "SerlithNetwork/Puffernot";
    private static final String DOWNLOAD_PAGE = "https://github.com/SerlithNetwork/Puffernot/releases";

    private static final int DISTANCE_ERROR = -1;
    private static final int DISTANCE_UNKNOWN = -2;
    private static final ServerBuildInfo BUILD_INFO = ServerBuildInfo.buildInfo();
    private static final String USER_AGENT = BUILD_INFO.brandName() + "/" + BUILD_INFO.asString(VERSION_SIMPLE) + " (https://serlith.net)";

    private static final Gson GSON = new Gson();

    @Override
    public long getCacheTime() {
        return CACHE_TIME;
    }

    @Override
    public @NonNull Component getVersionMessage() {
        final Component updateMessage;
        final ServerBuildInfo build = ServerBuildInfo.buildInfo();
        if (build.buildNumber().isEmpty() && build.gitCommit().isEmpty()) {
            updateMessage = text("You are running a development version without access to version information", color(0xFF5300));
        } else {
            updateMessage = PufferfishVersionFetcher.getUpdateStatusMessage(); // Pufferfish - Rebrand
        }
        final Component history = this.getHistory();

        return history != null ? Component.textOfChildren(updateMessage, Component.newline(), history) : updateMessage;
    }

    private static Component getUpdateStatusMessage() {
        int distance = DISTANCE_ERROR;

        final OptionalInt buildNumber = PufferfishVersionFetcher.BUILD_INFO.buildNumber();
        if (buildNumber.isPresent()) {
            distance = PufferfishVersionFetcher.fetchDistanceFromSiteApi(buildNumber.getAsInt());
        } else {
            final Optional<String> gitBranch = PufferfishVersionFetcher.BUILD_INFO.gitBranch();
            final Optional<String> gitCommit = PufferfishVersionFetcher.BUILD_INFO.gitCommit();
            if (gitBranch.isPresent() && gitCommit.isPresent()) {
                distance = PufferfishVersionFetcher.fetchDistanceFromGitHub(gitBranch.get(), gitCommit.get());
            }
        }

        return switch (distance) {
            case DISTANCE_ERROR -> text("Error obtaining version information", NamedTextColor.YELLOW);
            case 0 -> text("You are running the latest version", NamedTextColor.GREEN);
            case DISTANCE_UNKNOWN -> text("Unknown version", NamedTextColor.YELLOW);
            default -> text("You are " + distance + " version(s) behind", NamedTextColor.YELLOW)
                .append(Component.newline())
                .append(text("Download the new version at: ")
                    .append(text(DOWNLOAD_PAGE, NamedTextColor.GOLD)
                        .hoverEvent(text("Click to open", NamedTextColor.WHITE))
                        .clickEvent(ClickEvent.openUrl(DOWNLOAD_PAGE))));
        };
    }

    private static int fetchDistanceFromSiteApi(final int actionsBuild) {
        try {
            final URL buildsUrl = URI.create("https://version.serlith.net/api/v1/project/fetch/pufferfish/" + PufferfishVersionFetcher.BUILD_INFO.minecraftVersionId() + "/latest/build").toURL();
            final HttpURLConnection connection = (HttpURLConnection) buildsUrl.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", PufferfishVersionFetcher.USER_AGENT);
            connection.setRequestProperty("Accept", "application/json");
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                final JsonPrimitive json = GSON.fromJson(reader, JsonPrimitive.class);
                final int latest = json.getAsInt();
                return Math.max(latest - actionsBuild, 0);
            } catch (final JsonSyntaxException ex) {
                LOGGER.error("Error parsing json from Pufferfish's downloads API", ex);
                return DISTANCE_ERROR;
            } catch (final SocketTimeoutException ex) {
                return 0;
            }
        } catch (final IOException e) {
            LOGGER.error("Error while parsing version", e);
            return DISTANCE_ERROR;
        }
    }

    // Contributed by Techcable <Techcable@outlook.com> in GH-65
    private static int fetchDistanceFromGitHub(final String branch, final String hash) {
        try {
            final HttpURLConnection connection = (HttpURLConnection) URI.create("https://api.github.com/repos/%s/compare/%s...%s".formatted(PufferfishVersionFetcher.REPOSITORY, branch, hash)).toURL().openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", PufferfishVersionFetcher.USER_AGENT);
            connection.connect();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) return DISTANCE_UNKNOWN; // Unknown commit
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                final JsonObject obj = GSON.fromJson(reader, JsonObject.class);
                final String status = obj.get("status").getAsString();
                return switch (status) {
                    case "identical" -> 0;
                    case "behind" -> obj.get("behind_by").getAsInt();
                    default -> DISTANCE_ERROR;
                };
            } catch (final JsonSyntaxException | NumberFormatException e) {
                LOGGER.error("Error parsing json from GitHub's API", e);
                return DISTANCE_ERROR;
            }
        } catch (final IOException ignored) {
            return DISTANCE_ERROR;
        }
    }

    public static void getUpdateStatusStartupMessage() {
        int distance = DISTANCE_ERROR;

        final OptionalInt buildNumber = BUILD_INFO.buildNumber();
        if (buildNumber.isEmpty() && BUILD_INFO.gitCommit().isEmpty()) {
            COMPONENT_LOGGER.warn(text("*** You are running a development version without access to version information ***"));
        } else {
            final Optional<PaperVersionFetcher.MinecraftVersionFetcher> apiResult = PaperVersionFetcher.fetchMinecraftVersionList();
            if (buildNumber.isPresent()) {
                distance = PufferfishVersionFetcher.fetchDistanceFromSiteApi(buildNumber.getAsInt());
            } else {
                final Optional<String> gitBranch = BUILD_INFO.gitBranch();
                final Optional<String> gitCommit = BUILD_INFO.gitCommit();
                if (gitBranch.isPresent() && gitCommit.isPresent()) {
                    distance = PufferfishVersionFetcher.fetchDistanceFromGitHub(gitBranch.get(), gitCommit.get());
                }
            }

            switch (distance) {
                case DISTANCE_ERROR -> COMPONENT_LOGGER.error(text("*** Error obtaining version information! Cannot fetch version info ***"));
                case 0 -> apiResult.ifPresent(result -> {
                    COMPONENT_LOGGER.warn(text("*************************************************************************************"));
                    COMPONENT_LOGGER.warn(text("You are running the latest build for your Minecraft version (" + BUILD_INFO.minecraftVersionId() + ")"));
                    COMPONENT_LOGGER.warn(text("However, you are " + result.distance() + " release(s) behind the latest stable release (" + result.latestVersion() + ")!"));
                    COMPONENT_LOGGER.warn(text("It is recommended that you update as soon as possible"));
                    COMPONENT_LOGGER.warn(text(DOWNLOAD_PAGE));
                    COMPONENT_LOGGER.warn(text("*************************************************************************************"));
                });
                case DISTANCE_UNKNOWN -> COMPONENT_LOGGER.warn(text("*** You are running an unknown version! Cannot fetch version info ***"));
                default -> {
                    if (apiResult.isPresent()) {
                        COMPONENT_LOGGER.warn(text("*** You are running an outdated version of Minecraft, which is " + apiResult.get().distance() + " release(s) and " + distance + " build(s) behind!"));
                        COMPONENT_LOGGER.warn(text("*** Please update to the latest stable version on " + DOWNLOAD_PAGE + " ***"));
                    } else {
                        COMPONENT_LOGGER.info(text("*** Currently you are " + distance + " build(s) behind ***"));
                        COMPONENT_LOGGER.info(text("*** It is highly recommended to download the latest build from " + DOWNLOAD_PAGE + " ***"));
                    }
                }
            }
        }
    }

}
