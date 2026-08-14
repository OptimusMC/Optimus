package pl.optimusteam.optimus.network;

import io.netty.channel.EventLoop;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import pl.optimusteam.optimus.ecs.PlayerComponents;

import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class CopyOnWriteArrayListWithBatchStates extends CopyOnWriteArrayList<ServerPlayer> {
    private final HashMap<EventLoop, LoopBucket> eventLoopBuckets = new HashMap<>();

    public HashMap<EventLoop, LoopBucket> getEventLoopBuckets() {
        return eventLoopBuckets;
    }

    @Override
    public boolean add(ServerPlayer e) {
        Connection connection = e.connection.connection;
        EventLoop eventLoop = connection.channel.eventLoop();
        eventLoopBuckets.computeIfAbsent(eventLoop, _ -> new LoopBucket(eventLoop)).connections.add(connection);

        PlayerComponents.add(e); // Mini ECS

        return super.add(e);
    }

    @Override
    public boolean remove(Object o) {
        ServerPlayer e = (ServerPlayer) o;
        Connection connection = e.connection.connection;
        EventLoop eventLoop = connection.channel.eventLoop();
        eventLoopBuckets.computeIfAbsent(eventLoop, _ -> new LoopBucket(eventLoop)).connections.remove(connection);

        PlayerComponents.remove(e); // Mini ECS

        return super.remove(e);
    }
}
