package gg.pufferfish.pufferfish;


import de.bsommerfeld.jshepherd.annotation.Comment;
import de.bsommerfeld.jshepherd.annotation.Key;
import de.bsommerfeld.jshepherd.annotation.PostInject;
import de.bsommerfeld.jshepherd.annotation.Section;
import de.bsommerfeld.jshepherd.core.ConfigurablePojo;
import de.bsommerfeld.jshepherd.core.ConfigurationLoader;
import gg.pufferfish.pufferfish.flare.FlareSetup;
import gg.pufferfish.pufferfish.sentry.SentryManager;
import gg.pufferfish.pufferfish.simd.SIMDDetection;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.NullMarked;
import java.net.URI;
import java.nio.file.Paths;
import java.util.List;

@NullMarked
@Comment({
    "Pufferfish Configuration",
    "Check out Pufferfish Host for maximum performance server hosting: https://pufferfish.host",
    "Join our Discord for support: https://discord.gg/reZw4vQV9H",
    "Download new builds at https://ci.pufferfish.host/job/Pufferfish"
})
@SuppressWarnings({"unused", "FieldMayBeFinal", "FieldCanBeLocal"})
public class PufferfishConfig extends ConfigurablePojo<PufferfishConfig> {
    private PufferfishConfig() {
    }

    @SuppressWarnings("NullAway.Init")
    private static PufferfishConfig INSTANCE;
    public static PufferfishConfig getInstance() {
        return INSTANCE;
    }

    private static boolean INITIALIZED = false;
    public static void init() {
        if (INITIALIZED) {
            return;
        }

        INSTANCE = ConfigurationLoader.from(Paths.get("pufferfish.yml"))
            .withComments()
            .load(PufferfishConfig::new);
        INITIALIZED = true;

        // Attempt to detect vectorization
        try {
            SIMDDetection.isEnabled = SIMDDetection.canEnable(PufferfishLogger.LOGGER);
            SIMDDetection.versionLimited = SIMDDetection.getJavaVersion() < SIMDDetection.MIN_JAVA_VERSION || SIMDDetection.getJavaVersion() > SIMDDetection.MAX_JAVA_VERSION;
        } catch (NoClassDefFoundError | Exception e) {
            MinecraftServer.LOGGER.error(e.getMessage());
        }

        if (SIMDDetection.isEnabled) {
            PufferfishLogger.LOGGER.info("SIMD operations detected as functional. Will replace some operations with faster versions.");
        } else if (SIMDDetection.versionLimited) {
            PufferfishLogger.LOGGER.warning("Will not enable SIMD! These optimizations are only safely supported on Java 17-27.");
        } else {
            PufferfishLogger.LOGGER.warning("SIMD operations are available for your server, but are not configured!");
            PufferfishLogger.LOGGER.warning("To enable additional optimizations, add \"--add-modules=jdk.incubator.vector\" to your startup flags, BEFORE the \"-jar\".");
            PufferfishLogger.LOGGER.warning("If you have already added this flag, then SIMD operations are not supported on your JVM or CPU.");
            PufferfishLogger.LOGGER.warning("Debug: Java: " + System.getProperty("java.version") + ", test run: " + SIMDDetection.testRun);
        }
    }

    @Section("info")
    public Info info = new Info();
    public static class Info {
        @Key("version")
        public String version = "1.0";
    }

    @Comment({
        "Whether or not books should be writeable.",
        "Servers that anticipate being a target for duping may want to consider",
        "disabling this option.",
        "This can be overridden per-player with the permission pufferfish.usebooks"
    })
    @Key("enable-books")
    public boolean enableBooks = true;

    @Comment({
        "Optimizes the suffocation check by selectively skipping",
        "the check in a way that still appears vanilla. This should",
        "be left enabled on most servers, but is provided as a",
        "configuration option if the vanilla deviation is undesirable."
    })
    @Key("enable-suffocation-optimization")
    public boolean enableSuffocationOptimization = true;

    @Comment({
        "Whether or not asynchronous mob spawning should be enabled.",
        "On servers with many entities, this can improve performance by up to 15%. You must have",
        "paper's per-player-mob-spawns setting set to true for this to work.",
        "One quick note - this does not actually spawn mobs async (that would be very unsafe).",
        "This just offloads some expensive calculations that are required for mob spawning."
    })
    @Key("enable-async-mob-spawning")
    private boolean enableAsyncMobSpawning = true;
    public transient boolean asyncMobSpawning = true;
    private transient boolean asyncMobSpawningInitialized = false;

    @Comment("Optimizes projectile settings")
    @Section("projectile")
    public Projectile projectile = new Projectile();
    public static class Projectile {

        @Comment({
            "Controls how many chunks are allowed",
            "to be sync loaded by projectiles in a tick."
        })
        @Key("max-loads-per-tick")
        public int maxLoadsPerTick = 10;

        @Comment({
            "Controls how many chunks a projectile",
            "can load in its lifetime before it gets", "automatically removed."
        })
        @Key("max-loads-per-projectile")
        public int maxLoadsPerProjectile = 10;

    }

    @Section("dab")
    @Comment({
        "Optimizes entity brains when",
        "they're far away from the player"
    })
    public DAB dab = new DAB();
    public static class DAB {

        @Key("enabled")
        public boolean enabled = true;

        @Comment({
            "This value determines how far away an entity has to be",
            "from the player to start being effected by DEAR."
        })
        @Key("start-distance")
        private int startDistance = 12;
        public transient int startDistanceSquared = 144;

        @Comment({
            "This value defines how often in ticks, the furthest entity",
            "will get their pathfinders and behaviors ticked. 20 = 1s"
        })
        @Key("max-tick-freq")
        public int maxTickFreq = 20;

        @Comment({
            "This value defines how much distance modifies an entity's",
            "tick frequency. freq = (distanceToPlayer^2) / (2^value)",
            "If you want further away entities to tick less often, use 7.",
            "If you want further away entities to tick more often, try 9."
        })
        @Key("activation-dist-mod")
        public int activationDistMod = 8;

        @Comment("A list of entities to ignore for activation")
        @Key("blacklisted-entities")
        public List<String> blacklistedEntities = List.of();

    }

    @Comment("Configures Flare, the built-in profiler")
    @Section("flare")
    public Flare flare = new Flare();
    public static class Flare {

        @Comment("Sets the server to use for profiles.")
        @Key("url")
        private String urlString = "https://flare.airplane.gg";
        public transient URI url = URI.create(this.urlString);

    }

    @Comment("Options for connecting to Pufferfish/Airplane's online utilities")
    @Section("web-services")
    public WebServices webServices = new WebServices();
    public static class WebServices {

        @Key("token")
        public String token = "";

    }

    @Comment("Settings for things that don't belong elsewhere")
    @Section("misc")
    public Misc misc = new Misc();
    public static class Misc {

        @Key("disable-method-profiler")
        public boolean disableMethodProfiler = true;

    }

    @Comment({
        "Throttles the AI goal selector in entity inactive ticks.",
        "This can improve performance by a few percent, but has minor gameplay implications."
    })
    @Key("inactive-goal-selector-throttle")
    public boolean inactiveGoalSelectorThrottle = true;

    @Comment({
        "Allows end crystals to respawn the ender dragon.",
        "On servers that expect end crystal fights in the end dimension, disabling this",
        "will prevent the server from performing an expensive search to attempt respawning",
        "the ender dragon whenever a player places an end crystal."
    })
    @Key("allow-end-crystal-respawn")
    public boolean allowEndCrystalRespawn = true;

    @Comment({
        "Sentry DSN for improved error logging, leave blank to disable",
        "Obtain from https://sentry.io/"
    })
    @Key("sentry-dsn")
    public String sentryDns = "";

    @PostInject
    private void validate() {

        // Sentry
        String sentryEnvironment = System.getenv("SENTRY_DSN");
        if (!this.sentryDns.isBlank() || (sentryEnvironment != null && !sentryEnvironment.isBlank())) {
            SentryManager.init();
        }

        // Mob spawning
        if (!this.asyncMobSpawningInitialized) {
            this.asyncMobSpawningInitialized = true;
            this.asyncMobSpawning = this.enableAsyncMobSpawning;
        }

        // Dynamic activation of brain
        this.dab.startDistanceSquared = this.dab.startDistance * this.dab.startDistance;
        BuiltInRegistries.ENTITY_TYPE.forEach(e -> e.pufferfish$dabEnabled = true);
        this.dab.blacklistedEntities.forEach(name -> BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.tryParse(name)).ifPresentOrElse(
            e -> e.pufferfish$dabEnabled = false,
            () -> MinecraftServer.LOGGER.warn("Unknown entity \"{}\"", name)
        ));

        // Flare
        this.flare.url = URI.create(this.flare.urlString);
        if (!this.webServices.token.isBlank()) {
            FlareSetup.init();
        }

    }


}
