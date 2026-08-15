package pl.optimusteam.optimus.tracker;

import ca.spottedleaf.moonrise.patches.entity_tracker.EntityTrackerEntity;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import pl.optimusteam.optimus.ecs.EntityComponents;
import pl.optimusteam.optimus.ecs.PlayerComponents;

import java.util.Arrays;

public final class EntityTracker {
    private static Object[] candidates = new Object[2048];
    private static int candidateCount;

    public static void tick(ServerLevel level) {
        scan(level);
        apply();
    }

    private static void scan(ServerLevel level) {
        final int entityCount = EntityComponents.size;
        final double[] ex = EntityComponents.x;
        final double[] ey = EntityComponents.y;
        final double[] ez = EntityComponents.z;
        final int[] range = EntityComponents.trackingRange;
        final int[] rangeY = EntityComponents.rangeY;
        final boolean[] vbd = EntityComponents.visibleByDefault;
        final boolean[] isPlayer = EntityComponents.isPlayer;
        final boolean[] spectatorEntity = EntityComponents.spectatorEntity;
        final boolean[] cameraIsSelf = EntityComponents.cameraIsSelf;
        final Object[] elevel = EntityComponents.level;
        final Entity[] entities = EntityComponents.entity;
        final Object dim = level.dimension();
        final int words = (entityCount + 63) >> 6;
        for (int p = 0, playerCount = PlayerComponents.size; p < playerCount; ++p) {
            if (PlayerComponents.dimension[p] != dim) continue;
            final ServerPlayer viewer = PlayerComponents.player[p];
            final double px = PlayerComponents.x[p];
            final double py = PlayerComponents.y[p];
            final double pz = PlayerComponents.z[p];
            final int vd16 = PlayerComponents.viewDistance[p] * 16;
            final boolean viewerSpec = PlayerComponents.spectator[p];
            final boolean hasInv = PlayerComponents.hasInvertedVisibility[p];
            final long[] sent = PlayerComponents.sentBits[p];
            final long[] seen = PlayerComponents.seenByBits[p];
            final int selfSlot = viewer.ecsEntitySlot;
            for (int w = 0; w < words; ++w) {
                final int base = w << 6;
                final int end = Math.min(64, entityCount - base);
                long newWord = 0L;
                for (int b = 0; b < end; ++b) {
                    final int i = base + b;
                    if (elevel[i] != level) continue;
                    final double dx = px - ex[i];
                    final double dz = pz - ez[i];
                    final double visibleRange = Math.min(range[i], vd16);
                    boolean vis = dx * dx + dz * dz <= visibleRange * visibleRange;
                    final int ry = rangeY[i];
                    if (vis && ry != -1) {
                        final double dy = py - ey[i];
                        final double ryd = ry;
                        vis = dy * dy <= ryd * ryd;
                    }
                    if (vis) {
                        final int idx = (((Mth.floor(ex[i]) >> 4) & 127) << 7) | ((Mth.floor(ez[i]) >> 4) & 127);
                        vis = (sent[idx >> 6] & (1L << idx)) != 0L
                            && (!isPlayer[i] || (viewerSpec ? cameraIsSelf[i] : !spectatorEntity[i]))
                            && (hasInv || vbd[i]);
                    }
                    if (vis) newWord |= 1L << b;
                }
                if ((selfSlot >> 6) == w) newWord &= ~(1L << selfSlot);
                long xor = getXor(newWord, seen[w]);
                while (xor != 0L) {
                    final int b = Long.numberOfTrailingZeros(xor);
                    xor &= xor - 1L;
                    addCandidate(entities[base + b], viewer);
                }
            }
        }
    }

    private static long getXor(long newWord, long seen) {
        return newWord ^ seen;
    }

    private static void apply() {
        for (int k = 0; k < candidateCount; k += 2) {
            final Entity e = (Entity) candidates[k];
            final ServerPlayer p = (ServerPlayer) candidates[k + 1];
            candidates[k] = null;
            candidates[k + 1] = null;
            final ChunkMap.TrackedEntity tracker = ((EntityTrackerEntity) e).moonrise$getTrackedEntity();
            if (tracker != null) tracker.updatePlayer(p);
        }
        candidateCount = 0;
    }

    private static void addCandidate(Entity e, ServerPlayer p) {
        if (candidateCount + 2 > candidates.length) candidates = Arrays.copyOf(candidates, candidates.length * 2);
        candidates[candidateCount++] = e;
        candidates[candidateCount++] = p;
    }

}
