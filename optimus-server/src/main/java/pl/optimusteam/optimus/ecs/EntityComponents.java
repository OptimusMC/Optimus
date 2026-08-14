package pl.optimusteam.optimus.ecs;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.Arrays;

public final class EntityComponents {
    private static int capacity = 512;
    public static int size;
    public static double[] x = new double[capacity];
    public static double[] y = new double[capacity];
    public static double[] z = new double[capacity];
    public static double[] bbMinX = new double[capacity];
    public static double[] bbMinY = new double[capacity];
    public static double[] bbMinZ = new double[capacity];
    public static double[] bbMaxX = new double[capacity];
    public static double[] bbMaxY = new double[capacity];
    public static double[] bbMaxZ = new double[capacity];
    public static int[] trackingRange = new int[capacity];
    public static byte[] activationType = new byte[capacity];
    public static boolean[] defaultActivationState = new boolean[capacity];
    public static long[] activatedTick = new long[capacity];
    public static boolean[] visibleByDefault = new boolean[capacity];
    public static Object[] level = new Object[capacity];
    public static Entity[] entity = new Entity[capacity];

    public static void add(Entity e, int range) {
        if (e.ecsEntitySlot != -1) throw new IllegalStateException("entity already registered: " + e);
        if (size == capacity) grow();
        int slot = size++;
        x[slot] = e.getX();
        y[slot] = e.getY();
        z[slot] = e.getZ();
        AABB bb = e.getBoundingBox();
        bbMinX[slot] = bb.minX;
        bbMinY[slot] = bb.minY;
        bbMinZ[slot] = bb.minZ;
        bbMaxX[slot] = bb.maxX;
        bbMaxY[slot] = bb.maxY;
        bbMaxZ[slot] = bb.maxZ;
        trackingRange[slot] = range;
        activationType[slot] = (byte) e.activationType.ordinal();
        defaultActivationState[slot] = e.defaultActivationState;
        activatedTick[slot] = e.activatedTick;
        visibleByDefault[slot] = e.visibleByDefault;
        level[slot] = e.level();
        entity[slot] = e;
        e.ecsEntitySlot = slot;
    }

    public static void remove(Entity e) {
        int slot = e.ecsEntitySlot;
        if (slot == -1) return;
        e.ecsEntitySlot = -1;
        int last = --size;
        if (slot != last) {
            x[slot] = x[last];
            y[slot] = y[last];
            z[slot] = z[last];
            bbMinX[slot] = bbMinX[last];
            bbMinY[slot] = bbMinY[last];
            bbMinZ[slot] = bbMinZ[last];
            bbMaxX[slot] = bbMaxX[last];
            bbMaxY[slot] = bbMaxY[last];
            bbMaxZ[slot] = bbMaxZ[last];
            trackingRange[slot] = trackingRange[last];
            activationType[slot] = activationType[last];
            defaultActivationState[slot] = defaultActivationState[last];
            activatedTick[slot] = activatedTick[last];
            visibleByDefault[slot] = visibleByDefault[last];
            level[slot] = level[last];
            entity[slot] = entity[last];
            entity[slot].ecsEntitySlot = slot;
        }
        level[last] = null;
        entity[last] = null;
    }

    public static void setPos(int slot, double ex, double ey, double ez) {
        x[slot] = ex;
        y[slot] = ey;
        z[slot] = ez;
    }

    public static void setBb(int slot, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        bbMinX[slot] = minX;
        bbMinY[slot] = minY;
        bbMinZ[slot] = minZ;
        bbMaxX[slot] = maxX;
        bbMaxY[slot] = maxY;
        bbMaxZ[slot] = maxZ;
    }

    public static void setActivatedTick(int slot, long tick) {
        activatedTick[slot] = tick;
    }

    public static void setVisibleByDefault(int slot, boolean v) {
        visibleByDefault[slot] = v;
    }

    private static void grow() {
        capacity *= 2;
        x = Arrays.copyOf(x, capacity);
        y = Arrays.copyOf(y, capacity);
        z = Arrays.copyOf(z, capacity);
        bbMinX = Arrays.copyOf(bbMinX, capacity);
        bbMinY = Arrays.copyOf(bbMinY, capacity);
        bbMinZ = Arrays.copyOf(bbMinZ, capacity);
        bbMaxX = Arrays.copyOf(bbMaxX, capacity);
        bbMaxY = Arrays.copyOf(bbMaxY, capacity);
        bbMaxZ = Arrays.copyOf(bbMaxZ, capacity);
        trackingRange = Arrays.copyOf(trackingRange, capacity);
        activationType = Arrays.copyOf(activationType, capacity);
        defaultActivationState = Arrays.copyOf(defaultActivationState, capacity);
        activatedTick = Arrays.copyOf(activatedTick, capacity);
        visibleByDefault = Arrays.copyOf(visibleByDefault, capacity);
        level = Arrays.copyOf(level, capacity);
        entity = Arrays.copyOf(entity, capacity);
    }
}
