package pl.optimusteam.optimus.network;

import io.netty.channel.EventLoop;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import pl.optimusteam.optimus.ecs.PlayerComponents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class CopyOnWriteArrayListWithBatchStates extends CopyOnWriteArrayList<ServerPlayer> {
    private final HashMap<EventLoop, LoopBucket> eventLoopBuckets = new HashMap<>();
    private final ArrayList<LoopBucket> bucketsByIndex = new ArrayList<>();

    public HashMap<EventLoop, LoopBucket> getEventLoopBuckets() {
        return eventLoopBuckets;
    }

    public ArrayList<LoopBucket> getBucketsByIndex() {
        return bucketsByIndex;
    }

    private LoopBucket getOrCreateBucket(EventLoop eventLoop) {
        LoopBucket bucket = eventLoopBuckets.get(eventLoop);
        if (bucket == null) {
            bucket = new LoopBucket(eventLoop, bucketsByIndex.size());
            eventLoopBuckets.put(eventLoop, bucket);
            bucketsByIndex.add(bucket);
        }
        return bucket;
    }

    @Override
    public boolean add(ServerPlayer e) {
        Connection connection = e.connection.connection;
        LoopBucket bucket = getOrCreateBucket(connection.channel.eventLoop());
        bucket.connections.add(connection);

        PlayerComponents.add(e, bucket.index); // Mini ECS

        return super.add(e);
    }

    @Override
    public boolean remove(Object o) {
        ServerPlayer e = (ServerPlayer) o;
        Connection connection = e.connection.connection;
        getOrCreateBucket(connection.channel.eventLoop()).connections.remove(connection);

        PlayerComponents.remove(e); // Mini ECS

        return super.remove(e);
    }
}
