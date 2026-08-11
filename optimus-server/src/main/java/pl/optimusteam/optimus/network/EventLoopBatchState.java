package pl.optimusteam.optimus.network;

import com.mojang.logging.LogUtils;
import io.netty.channel.Channel;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.network.Connection;
import org.slf4j.Logger;

public class EventLoopBatchState {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final long PACKET_DRAINING_INTERVAL = Long.getLong("optimus.packetDrainingIntervalNanos", 1_000_000L);
    private static final int MAIN_THREAD_PACKET_DRAIN_CAPACITY = Integer.getInteger("optimus.mainThreadPacketDrainCapacity", (int) Math.pow(2, 14));
    private static final int OFF_MAIN_THREAD_PACKET_DRAIN_CAPACITY = Integer.getInteger("optimus.offMainThreadPacketDrainCapacity", (int) Math.pow(2, 14));

    public final io.netty.channel.EventLoop eventLoop;
    private final ReferenceOpenHashSet<Channel> channelsToFlush = new ReferenceOpenHashSet<>(64);

    public EventLoopBatchState(io.netty.channel.EventLoop eventLoop) {
        this.eventLoop = eventLoop;
        this.eventLoop.scheduleWithFixedDelay(this::doDrain, PACKET_DRAINING_INTERVAL, PACKET_DRAINING_INTERVAL, java.util.concurrent.TimeUnit.NANOSECONDS);
    }

    private void doDrain() {
        do {
            this.channelsToFlush.clear();
            QueueEntry entry;
            while ((entry = this.mainThreadQueue.poll()) != null || (entry = this.offMainThreadQueue.poll()) != null) {
                try {
                    for (final Connection connection : entry.connections) {
                        connection.optimus$doSendPacket(entry.packet, entry.listener, entry.listener != null);
                        if (entry.flush) {
                            this.channelsToFlush.add(connection.channel);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Exception in batched packet send", e);
                }
            }
            for (Channel ch : this.channelsToFlush) {
                if (ch.isOpen()) ch.flush();
            }
        } while (!this.mainThreadQueue.isEmpty() || !this.offMainThreadQueue.isEmpty());
    }

    private final org.jctools.queues.SpscArrayQueue<QueueEntry> mainThreadQueue = new org.jctools.queues.SpscArrayQueue<>(MAIN_THREAD_PACKET_DRAIN_CAPACITY);
    private static long lastFullMainThreadQueueWarning = System.currentTimeMillis();

    public void offerToMainQueue(QueueEntry queueEntry) {
        boolean failedToOffer = false;
        while (!mainThreadQueue.offer(queueEntry)) {
            failedToOffer = true;
            Thread.yield();
        }

        if (failedToOffer && System.currentTimeMillis() - lastFullMainThreadQueueWarning > 30_000) {
            LOGGER.warn("Optimus packet drain queue is full. This will impact main thread performance.");
            LOGGER.warn("Please refer to the Optimus documentation for mitigation steps.");
            lastFullMainThreadQueueWarning = System.currentTimeMillis();
        }
    }

    private final org.jctools.queues.MpscArrayQueue<QueueEntry> offMainThreadQueue = new org.jctools.queues.MpscArrayQueue<>(OFF_MAIN_THREAD_PACKET_DRAIN_CAPACITY);
    private static volatile long lastFullOffMainThreadQueueWarning = System.currentTimeMillis();

    public void offerToOffMainQueue(QueueEntry queueEntry) {
        boolean failedToOffer = false;
        while (!offMainThreadQueue.offer(queueEntry)) {
            failedToOffer = true;
            Thread.yield();
        }

        if (failedToOffer && System.currentTimeMillis() - lastFullOffMainThreadQueueWarning > 30_000) {
            LOGGER.warn("Optimus off-main packet drain queue is full. This will impact main thread performance.");
            LOGGER.warn("Please refer to the Optimus documentation for mitigation steps.");
            lastFullOffMainThreadQueueWarning = System.currentTimeMillis();
        }
    }
}
