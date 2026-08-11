package pl.optimusteam.optimus.network;

import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class QueueEntry {
    final Collection<Connection> connections;
    final Packet<?> packet;
    final @Nullable ChannelFutureListener listener;
    final boolean flush;

    public QueueEntry(Collection<Connection> connections, Packet<?> packet, @Nullable ChannelFutureListener listener, boolean flush) {
        this.connections = connections;
        this.packet = packet;
        this.listener = listener;
        this.flush = flush;
    }

    public QueueEntry(Connection connection, Packet<?> packet, @Nullable ChannelFutureListener listener, boolean flush) {
        this(Collections.singletonList(connection), packet, listener, flush);
    }
}
