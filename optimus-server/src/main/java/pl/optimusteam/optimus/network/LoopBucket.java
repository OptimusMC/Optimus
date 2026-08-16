package pl.optimusteam.optimus.network;

import io.netty.channel.EventLoop;
import java.util.ArrayList;

import net.minecraft.network.Connection;

public final class LoopBucket {
    public final EventLoop eventLoop;
    public final EventLoopBatchState batchState;
    public final ArrayList<Connection> connections = new ArrayList<>();
    public final int index;

    public LoopBucket(EventLoop eventLoop) {
        this(eventLoop, -1);
    }

    public LoopBucket(EventLoop eventLoop, int index) {
        this.eventLoop = eventLoop;
        this.index = index;
        this.batchState = Connection.BATCH_STATES.computeIfAbsent(eventLoop, EventLoopBatchState::new);
    }

    @SuppressWarnings("unchecked")
    public ArrayList<Connection> getConnectionsCloned() {
        return (ArrayList<Connection>) connections.clone();
    }
}
