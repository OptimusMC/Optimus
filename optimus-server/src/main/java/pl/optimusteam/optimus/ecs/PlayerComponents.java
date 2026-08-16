package pl.optimusteam.optimus.ecs;

import ca.spottedleaf.moonrise.common.util.CoordinateUtils;
import ca.spottedleaf.moonrise.patches.chunk_system.player.ChunkSystemServerPlayer;
import ca.spottedleaf.moonrise.patches.chunk_system.player.RegionizedPlayerChunkLoader;
import ca.spottedleaf.moonrise.patches.entity_tracker.EntityTrackerEntity;
import it.unimi.dsi.fastutil.longs.LongIterator;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;

public final class PlayerComponents {
    private static int capacity = 512;
    public static int size;
    public static double[] x = new double[capacity];
    public static double[] y = new double[capacity];
    public static double[] z = new double[capacity];
    public static Object[] dimension = new Object[capacity];
    public static int[] viewDistance = new int[capacity];
    public static boolean[] spectator = new boolean[capacity];
    public static boolean[] hasInvertedVisibility = new boolean[capacity];
    public static long[][] seenByBits = new long[capacity][];
    public static long[][] sentBits = new long[capacity][];
    public static ServerPlayer[] player = new ServerPlayer[capacity];
    public static Connection[] connection = new Connection[capacity];
    public static int[] bucketId = new int[capacity];

    public static void add(ServerPlayer p, int bucket) {
        if (p.ecsSlot != -1) throw new IllegalStateException("player already registered: " + p);
        if (size == capacity) grow();
        int slot = size++;
        x[slot] = p.getX();
        y[slot] = p.getY();
        z[slot] = p.getZ();
        dimension[slot] = p.level().dimension();
        viewDistance[slot] = RegionizedPlayerChunkLoader.getAPISendViewDistance(p);
        spectator[slot] = p.isSpectator();
        hasInvertedVisibility[slot] = p.getBukkitEntity().optimus$hasInvertedVisibility();
        long[] seen = new long[EntityComponents.capacityWords()];
        for (int i = 0, n = EntityComponents.size; i < n; ++i) {
            ChunkMap.TrackedEntity tracker = ((EntityTrackerEntity) EntityComponents.entity[i]).moonrise$getTrackedEntity();
            if (tracker != null && tracker.seenBy.contains(p.connection)) seen[i >> 6] |= 1L << i;
        }
        seenByBits[slot] = seen;
        long[] sent = new long[256];
        RegionizedPlayerChunkLoader.PlayerChunkLoaderData loader = ((ChunkSystemServerPlayer) p).moonrise$getChunkLoader();
        if (loader != null) {
            LongIterator it = loader.getSentChunksRaw().iterator();
            while (it.hasNext()) {
                long key = it.nextLong();
                int idx = ((CoordinateUtils.getChunkX(key) & 127) << 7) | (CoordinateUtils.getChunkZ(key) & 127);
                sent[idx >> 6] |= 1L << idx;
            }
        }
        sentBits[slot] = sent;
        player[slot] = p;
        connection[slot] = p.connection.connection;
        bucketId[slot] = bucket;
        p.ecsSlot = slot;
    }

    public static void remove(ServerPlayer p) {
        int slot = p.ecsSlot;
        if (slot == -1) return;
        p.ecsSlot = -1;
        int last = --size;
        if (slot != last) {
            x[slot] = x[last];
            y[slot] = y[last];
            z[slot] = z[last];
            dimension[slot] = dimension[last];
            viewDistance[slot] = viewDistance[last];
            spectator[slot] = spectator[last];
            hasInvertedVisibility[slot] = hasInvertedVisibility[last];
            seenByBits[slot] = seenByBits[last];
            sentBits[slot] = sentBits[last];
            player[slot] = player[last];
            connection[slot] = connection[last];
            bucketId[slot] = bucketId[last];
            player[slot].ecsSlot = slot;
        }
        dimension[last] = null;
        seenByBits[last] = null;
        sentBits[last] = null;
        player[last] = null;
        connection[last] = null;
    }

    public static void setPos(int slot, double px, double py, double pz) {
        x[slot] = px;
        y[slot] = py;
        z[slot] = pz;
    }

    public static void setDimension(int slot, Object dim) {
        dimension[slot] = dim;
    }

    public static void setViewDistance(int slot, int vd) {
        viewDistance[slot] = vd;
    }

    public static void setSpectator(int slot, boolean v) {
        spectator[slot] = v;
    }

    public static void setHasInvertedVisibility(int slot, boolean v) {
        hasInvertedVisibility[slot] = v;
    }

    public static void setSeenBit(int playerSlot, int entitySlot) {
        if (playerSlot == -1 || entitySlot == -1) return;
        seenByBits[playerSlot][entitySlot >> 6] |= 1L << entitySlot;
    }

    public static void clearSeenBit(int playerSlot, int entitySlot) {
        if (playerSlot == -1 || entitySlot == -1) return;
        seenByBits[playerSlot][entitySlot >> 6] &= ~(1L << entitySlot);
    }

    public static void sentChunkAdd(ServerPlayer p, int cx, int cz) {
        int slot = p.ecsSlot;
        if (slot == -1) return;
        int idx = ((cx & 127) << 7) | (cz & 127);
        sentBits[slot][idx >> 6] |= 1L << idx;
    }

    public static void sentChunkRemove(ServerPlayer p, int cx, int cz) {
        int slot = p.ecsSlot;
        if (slot == -1) return;
        int idx = ((cx & 127) << 7) | (cz & 127);
        sentBits[slot][idx >> 6] &= ~(1L << idx);
    }

    public static void fixupEntityBits(int slot, int last) {
        for (int p = 0; p < size; ++p) {
            long[] bits = seenByBits[p];
            boolean moved = (bits[last >> 6] & (1L << last)) != 0L;
            bits[last >> 6] &= ~(1L << last);
            if (slot != last) {
                if (moved) bits[slot >> 6] |= 1L << slot;
                else bits[slot >> 6] &= ~(1L << slot);
            }
        }
    }

    public static void growEntityBits(int entityCapacity) {
        int words = entityCapacity >> 6;
        for (int p = 0; p < size; ++p) {
            seenByBits[p] = Arrays.copyOf(seenByBits[p], words);
        }
    }

    private static void grow() {
        capacity *= 2;
        x = Arrays.copyOf(x, capacity);
        y = Arrays.copyOf(y, capacity);
        z = Arrays.copyOf(z, capacity);
        dimension = Arrays.copyOf(dimension, capacity);
        viewDistance = Arrays.copyOf(viewDistance, capacity);
        spectator = Arrays.copyOf(spectator, capacity);
        hasInvertedVisibility = Arrays.copyOf(hasInvertedVisibility, capacity);
        seenByBits = Arrays.copyOf(seenByBits, capacity);
        sentBits = Arrays.copyOf(sentBits, capacity);
        player = Arrays.copyOf(player, capacity);
        connection = Arrays.copyOf(connection, capacity);
        bucketId = Arrays.copyOf(bucketId, capacity);
    }
}
