package gg.pufferfish.pufferfish.flare.collectors;

import ca.spottedleaf.common.time.TickData;
import co.technove.flare.live.CollectorData;
import co.technove.flare.live.LiveCollector;
import co.technove.flare.live.formatter.SuffixFormatter;
import gg.pufferfish.pufferfish.flare.CustomCategories;
import net.minecraft.server.MinecraftServer;

import java.time.Duration;

public class TPSCollector extends LiveCollector {
    private static final CollectorData TPS = new CollectorData("airplane:tps", "TPS", "Ticks per second, or how fast the server updates. For a smooth server this should be a constant 20TPS.", SuffixFormatter.of("TPS"), CustomCategories.MC_PERF);
    private static final CollectorData MSPT = new CollectorData("airplane:mspt", "MSPT", "Milliseconds per tick, which can show how well your server is performing. This value should always be under 50mspt.", SuffixFormatter.of("mspt"), CustomCategories.MC_PERF);

    public TPSCollector() {
        super(TPS, MSPT);

        this.interval = Duration.ofSeconds(5);
    }

    @Override
    public void run() {
        TickData.TickReportData data = MinecraftServer.getServer().tickTimes5s.generateTickReport(null, System.nanoTime(), MinecraftServer.getServer().tickRateManager().nanosecondsPerTick());
        double tps = data == null ? 2.0 : data.tpsData().segmentAll().average();
        double mspt = data == null ? 0.0 : (data.timePerTickData().segmentAll().average() / 1.0E6);

        this.report(TPS, Math.min(20D, Math.round(tps * 100d) / 100d));
        this.report(MSPT, (double) Math.round(mspt * 100d) / 100d);
    }
}
