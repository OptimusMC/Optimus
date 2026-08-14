package pl.optimusteam.optimus.ecs;

import ca.spottedleaf.moonrise.patches.chunk_system.player.RegionizedPlayerChunkLoader;
import net.minecraft.network.Connection;
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
    public static ServerPlayer[] player = new ServerPlayer[capacity];
    public static Connection[] connection = new Connection[capacity];

    public static void add(ServerPlayer p) {
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
        player[slot] = p;
        connection[slot] = p.connection.connection;
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
            player[slot] = player[last];
            connection[slot] = connection[last];
            player[slot].ecsSlot = slot;
        }
        dimension[last] = null;
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

    private static void grow() {
        capacity *= 2;
        x = Arrays.copyOf(x, capacity);
        y = Arrays.copyOf(y, capacity);
        z = Arrays.copyOf(z, capacity);
        dimension = Arrays.copyOf(dimension, capacity);
        viewDistance = Arrays.copyOf(viewDistance, capacity);
        spectator = Arrays.copyOf(spectator, capacity);
        hasInvertedVisibility = Arrays.copyOf(hasInvertedVisibility, capacity);
        player = Arrays.copyOf(player, capacity);
        connection = Arrays.copyOf(connection, capacity);
    }
}
