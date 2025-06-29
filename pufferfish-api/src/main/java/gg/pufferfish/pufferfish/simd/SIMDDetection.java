package gg.pufferfish.pufferfish.simd;

import org.jetbrains.annotations.ApiStatus;
import java.util.logging.Logger;

@ApiStatus.Internal
public class SIMDDetection {

    public static final int MIN_JAVA_VERSION = 17;
    public static final int MAX_JAVA_VERSION = 25;

    public static boolean isEnabled = false;
    public static boolean versionLimited = false;
    public static boolean testRun = false;

    private static int version = 0; // Aurora - Cache Java version

    @ApiStatus.Internal
    public static boolean canEnable(Logger logger) {
        try {
            return SIMDChecker.canEnable(logger);
        } catch (NoClassDefFoundError | Exception ignored) {
            return false;
        }
    }

    @ApiStatus.Internal
    public static int getJavaVersion() {
        // https://stackoverflow.com/a/2591122
        if (SIMDDetection.version != 0) return SIMDDetection.version; // Aurora - Cache Java version
        String version = System.getProperty("java.version");
        if(version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if(dot != -1) { version = version.substring(0, dot); }
        }
        version = version.split("-")[0]; // Pufferfish - Azul is stupid // Aurora - Yes it is
        return (SIMDDetection.version = Integer.parseInt(version)); // Aurora - Cache Java version
    }

}
