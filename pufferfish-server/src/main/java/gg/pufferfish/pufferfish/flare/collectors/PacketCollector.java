package gg.pufferfish.pufferfish.flare.collectors;

import co.technove.flare.Flare;
import co.technove.flare.live.CollectorData;
import co.technove.flare.live.LiveCollector;
import co.technove.flare.live.formatter.SuffixFormatter;
import gg.pufferfish.pufferfish.flare.CustomCategories;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

public class PacketCollector extends LiveCollector {

    private static final CollectorData OUTBOUND_PACKETS = new CollectorData("flare:network:outpacketcount", "Packets Sent", "Number of packets being sent by the server", new SuffixFormatter("Packet", "Packets"), CustomCategories.MC_PERF);

    public static final PacketCollector INSTANCE = new PacketCollector();
    private static final PacketCounterHandler PACKET_COUNTER = new PacketCounterHandler() ;

    public PacketCollector() {
        super(OUTBOUND_PACKETS);
        this.interval = Duration.ofSeconds(5);
    }

    @Override
    public void start(Flare flare) {
        for (Connection connection : MinecraftServer.getServer().getConnection().getConnections()) {
            connection.channel.pipeline().addLast("flare:packet_counter", PACKET_COUNTER);
        }
        super.start(flare);
    }

    @Override
    public void stop(Flare flare) {
        for (Connection connection : MinecraftServer.getServer().getConnection().getConnections()) {
            if (connection.channel.pipeline().get("flare:packet_counter") != null) {
                connection.channel.pipeline().remove("flare:packet_counter");
            }
        }
        super.stop(flare);
    }

    @Override
    public void run() {
        long outboundPackets = PACKET_COUNTER.outboundPacketCounter.getAndSet(0);
        this.report(OUTBOUND_PACKETS, outboundPackets);
    }

    @ChannelHandler.Sharable
    private static class PacketCounterHandler extends ChannelDuplexHandler {
        private final AtomicLong outboundPacketCounter = new AtomicLong();

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
            outboundPacketCounter.incrementAndGet();
            ctx.write(msg, promise);
        }

    }

    public void injectProfilingHandler(ServerPlayer player) {
        player.connection.connection.channel.pipeline().addLast("flare:packet_counter", PACKET_COUNTER);
    }

    public void uninjectProfilingHandler(ServerPlayer player) {
        if (player.connection.connection.channel.pipeline().get("flare:packet_counter") != null) {
            player.connection.connection.channel.pipeline().remove("flare:packet_counter");
        }
    }

}
