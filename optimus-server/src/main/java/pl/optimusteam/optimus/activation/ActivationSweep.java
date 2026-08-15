package pl.optimusteam.optimus.activation;

import io.papermc.paper.entity.activation.ActivationType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import pl.optimusteam.optimus.ecs.EntityComponents;

import java.util.Arrays;
import java.util.List;

public final class ActivationSweep {
    private static final int[] effRange = new int[ActivationType.values().length];
    private static int playerCapacity = 16;
    private static double[] pMinX = new double[playerCapacity];
    private static double[] pMaxX = new double[playerCapacity];
    private static double[] pMinZ = new double[playerCapacity];
    private static double[] pMaxZ = new double[playerCapacity];
    private static double[] pMinY = new double[playerCapacity];
    private static double[] pMaxY = new double[playerCapacity];

    public static void activateEntities(Level world, int miscRange, int raiderRange, int animalRange, int monsterRange,
                                        int waterRange, int flyingRange, int villagerRange, int maxRange) {
        effRange[ActivationType.MISC.ordinal()] = Math.min(miscRange, maxRange);
        effRange[ActivationType.RAIDER.ordinal()] = Math.min(raiderRange, maxRange);
        effRange[ActivationType.ANIMAL.ordinal()] = Math.min(animalRange, maxRange);
        effRange[ActivationType.MONSTER.ordinal()] = Math.min(monsterRange, maxRange);
        effRange[ActivationType.WATER.ordinal()] = Math.min(waterRange, maxRange);
        effRange[ActivationType.FLYING_MONSTER.ordinal()] = Math.min(flyingRange, maxRange);
        effRange[ActivationType.VILLAGER.ordinal()] = Math.min(villagerRange, maxRange);

        long tick = MinecraftServer.currentTick;
        boolean ignoreSpectators = world.spigotConfig.ignoreSpectatorActivation;
        List<? extends Player> players = world.players();
        int playerCount = 0;
        double uMinX = Double.POSITIVE_INFINITY;
        double uMaxX = Double.NEGATIVE_INFINITY;
        double uMinZ = Double.POSITIVE_INFINITY;
        double uMaxZ = Double.NEGATIVE_INFINITY;
        for (int j = 0, n = players.size(); j < n; ++j) {
            Player player = players.get(j);
            player.optimus$setActivatedTick(tick);
            if (ignoreSpectators && player.isSpectator()) continue;
            if (playerCount == playerCapacity) growPlayers();
            AABB bb = player.getBoundingBox();
            pMinX[playerCount] = bb.minX;
            pMaxX[playerCount] = bb.maxX;
            pMinZ[playerCount] = bb.minZ;
            pMaxZ[playerCount] = bb.maxZ;
            pMinY[playerCount] = bb.minY;
            pMaxY[playerCount] = bb.maxY;
            if (bb.minX < uMinX) uMinX = bb.minX;
            if (bb.maxX > uMaxX) uMaxX = bb.maxX;
            if (bb.minZ < uMinZ) uMinZ = bb.minZ;
            if (bb.maxZ > uMaxZ) uMaxZ = bb.maxZ;
            ++playerCount;
        }
        if (playerCount == 0) return;
        uMinX -= maxRange;
        uMaxX += maxRange;
        uMinZ -= maxRange;
        uMaxZ += maxRange;

        double worldHeight = world.getHeight();
        boolean tickMarkers = world.paperConfig().entities.markers.tick;
        int size = EntityComponents.size;
        double[] bbMinX = EntityComponents.bbMinX;
        double[] bbMinY = EntityComponents.bbMinY;
        double[] bbMinZ = EntityComponents.bbMinZ;
        double[] bbMaxX = EntityComponents.bbMaxX;
        double[] bbMaxY = EntityComponents.bbMaxY;
        double[] bbMaxZ = EntityComponents.bbMaxZ;
        long[] activatedTick = EntityComponents.activatedTick;
        byte[] activationType = EntityComponents.activationType;
        boolean[] defaultActivationState = EntityComponents.defaultActivationState;
        boolean[] isMarker = EntityComponents.isMarker;
        Object[] elevel = EntityComponents.level;
        Entity[] entities = EntityComponents.entity;
        double[] qMinX = pMinX;
        double[] qMaxX = pMaxX;
        double[] qMinZ = pMinZ;
        double[] qMaxZ = pMaxZ;
        double[] qMinY = pMinY;
        double[] qMaxY = pMaxY;
        int[] ranges = effRange;

        for (int i = 0; i < size; ++i) {
            if (activatedTick[i] >= tick) continue;
            if (elevel[i] != world) continue;
            double eMinX = bbMinX[i];
            double eMaxX = bbMaxX[i];
            double eMinZ = bbMinZ[i];
            double eMaxZ = bbMaxZ[i];
            if (eMinX >= uMaxX || eMaxX <= uMinX || eMinZ >= uMaxZ || eMaxZ <= uMinZ) continue;
            if (isMarker[i] && !tickMarkers) continue;
            double r = defaultActivationState[i] ? maxRange : ranges[activationType[i]];
            double eMinY = bbMinY[i];
            double eMaxY = bbMaxY[i];
            for (int j = 0; j < playerCount; ++j) {
                if (eMinX < qMaxX[j] + r && eMaxX > qMinX[j] - r
                    && eMinZ < qMaxZ[j] + r && eMaxZ > qMinZ[j] - r
                    && eMinY < qMaxY[j] + worldHeight && eMaxY > qMinY[j] - worldHeight) {
                    entities[i].optimus$setActivatedTick(tick);
                    break;
                }
            }
        }
    }

    private static void growPlayers() {
        playerCapacity *= 2;
        pMinX = Arrays.copyOf(pMinX, playerCapacity);
        pMaxX = Arrays.copyOf(pMaxX, playerCapacity);
        pMinZ = Arrays.copyOf(pMinZ, playerCapacity);
        pMaxZ = Arrays.copyOf(pMaxZ, playerCapacity);
        pMinY = Arrays.copyOf(pMinY, playerCapacity);
        pMaxY = Arrays.copyOf(pMaxY, playerCapacity);
    }

    private ActivationSweep() {
    }
}
