package pl.optimusteam.optimus.ecs;

import ca.spottedleaf.moonrise.common.PlatformHooks;
import com.google.common.collect.ImmutableList;
import io.papermc.paper.configuration.WorldConfiguration;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.Arrays;
import java.util.List;

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
    public static int[] baseRange = new int[capacity];
    public static int[] rangeY = new int[capacity];
    public static byte[] activationType = new byte[capacity];
    public static boolean[] defaultActivationState = new boolean[capacity];
    public static long[] activatedTick = new long[capacity];
    public static boolean[] visibleByDefault = new boolean[capacity];
    public static boolean[] isPlayer = new boolean[capacity];
    public static boolean[] spectatorEntity = new boolean[capacity];
    public static boolean[] cameraIsSelf = new boolean[capacity];
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
        trackingRange[slot] = effectiveRange(e, range);
        baseRange[slot] = range;
        rangeY[slot] = rangeYFor(e);
        activationType[slot] = (byte) e.activationType.ordinal();
        defaultActivationState[slot] = e.defaultActivationState;
        activatedTick[slot] = e.activatedTick;
        visibleByDefault[slot] = e.visibleByDefault;
        boolean p = e instanceof ServerPlayer;
        isPlayer[slot] = p;
        spectatorEntity[slot] = e.isSpectator();
        cameraIsSelf[slot] = !p || ((ServerPlayer) e).getCamera() == e;
        level[slot] = e.level();
        entity[slot] = e;
        e.ecsEntitySlot = slot;
    }

    public static void remove(Entity e) {
        int slot = e.ecsEntitySlot;
        if (slot == -1) return;
        e.ecsEntitySlot = -1;
        int last = --size;
        PlayerComponents.fixupEntityBits(slot, last);
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
            baseRange[slot] = baseRange[last];
            rangeY[slot] = rangeY[last];
            activationType[slot] = activationType[last];
            defaultActivationState[slot] = defaultActivationState[last];
            activatedTick[slot] = activatedTick[last];
            visibleByDefault[slot] = visibleByDefault[last];
            isPlayer[slot] = isPlayer[last];
            spectatorEntity[slot] = spectatorEntity[last];
            cameraIsSelf[slot] = cameraIsSelf[last];
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

    public static void setSpectatorEntity(int slot, boolean v) {
        spectatorEntity[slot] = v;
    }

    public static void setCameraIsSelf(int slot, boolean v) {
        cameraIsSelf[slot] = v;
    }

    public static void syncEffectiveRange(Entity vehicle) {
        for (Entity v = vehicle; v != null; v = v.getVehicle()) {
            int slot = v.ecsEntitySlot;
            if (slot != -1) trackingRange[slot] = effectiveRange(v, baseRange[slot]);
        }
    }

    public static void recaptureConfig() {
        for (int i = 0; i < size; ++i) {
            Entity e = entity[i];
            trackingRange[i] = effectiveRange(e, baseRange[i]);
            rangeY[i] = rangeYFor(e);
        }
    }

    public static int effectiveRange(Entity e, int base) {
        int range = base;
        if (e.getPassengers() != ImmutableList.<Entity>of()) {
            List<Entity> passengers = (List<Entity>) e.getIndirectPassengers();
            for (int i = 0, len = passengers.size(); i < len; ++i) {
                Entity passenger = passengers.get(i);
                range = Math.max(range, PlatformHooks.get().modifyEntityTrackingRange(passenger, passenger.getType().clientTrackingRange() << 4));
            }
        }
        return e.level().getServer().getScaledTrackingDistance(range);
    }

    public static int rangeYFor(Entity e) {
        WorldConfiguration.Entities.TrackingRangeY cfg = e.level().paperConfig().entities.trackingRangeY;
        return cfg.enabled ? cfg.get(e, -1) : -1;
    }

    public static int capacityWords() {
        return capacity >> 6;
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
        baseRange = Arrays.copyOf(baseRange, capacity);
        rangeY = Arrays.copyOf(rangeY, capacity);
        activationType = Arrays.copyOf(activationType, capacity);
        defaultActivationState = Arrays.copyOf(defaultActivationState, capacity);
        activatedTick = Arrays.copyOf(activatedTick, capacity);
        visibleByDefault = Arrays.copyOf(visibleByDefault, capacity);
        isPlayer = Arrays.copyOf(isPlayer, capacity);
        spectatorEntity = Arrays.copyOf(spectatorEntity, capacity);
        cameraIsSelf = Arrays.copyOf(cameraIsSelf, capacity);
        level = Arrays.copyOf(level, capacity);
        entity = Arrays.copyOf(entity, capacity);
        PlayerComponents.growEntityBits(capacity);
    }
}
