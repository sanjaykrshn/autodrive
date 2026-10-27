package com.sanjay.autodrive.track;

import com.sanjay.autodrive.AutoDrive;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import java.util.*;

/** Incremental, repeatable construction confined to the mod's own superflat dimension. */
public final class TrackBuilder {
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, AutoDrive.id("test_tracks"));
    private static final Map<String, List<TrackLayout.Sample>> CHECKPOINTS = new HashMap<>();
    private static final Map<String, List<Cell>> CELLS = new HashMap<>();
    private static final ArrayDeque<Job> JOBS = new ArrayDeque<>();
    private static final Set<String> QUEUED = new HashSet<>();
    public record Cell(int x, int z, double distance, double along) {}
    private record Job(String id, Iterator<Cell> cells, UUID requestedBy) {}
    private TrackBuilder() {}
    public static List<TrackLayout.Sample> checkpoints(TrackLayout track) {
        return CHECKPOINTS.computeIfAbsent(track.id(), ignored -> track.checkpoints());
    }
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(TrackBuilder::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { JOBS.clear(); QUEUED.clear(); });
    }
    public static boolean busy() { return !JOBS.isEmpty(); }
    public static boolean ready(ServerLevel level, TrackLayout track) {
        return level.getBlockState(new BlockPos(track.originX() - 14, TrackLayout.SURFACE_Y - 1, track.originZ() - 14)).is(Blocks.LODESTONE);
    }
    public static boolean queue(TrackLayout track, UUID requestedBy) {
        if (!QUEUED.add(track.id())) return false;
        JOBS.add(new Job(track.id(), raster(track).iterator(), requestedBy));
        return true;
    }
    public static List<Cell> raster(TrackLayout track) {
        return CELLS.computeIfAbsent(track.id(), ignored -> {
            Map<Long, Cell> nearest = new HashMap<>();
            for (var p : track.samples()) {
                for (int x = (int) Math.floor(p.x() - 12); x <= Math.ceil(p.x() + 12); x++) {
                    for (int z = (int) Math.floor(p.z() - 12); z <= Math.ceil(p.z() + 12); z++) {
                        double distance = Math.hypot(x + 0.5 - p.x(), z + 0.5 - p.z());
                        if (distance > 11) continue;
                        long key = ((long) x << 32) | (z & 0xffffffffL);
                        Cell old = nearest.get(key);
                        if (old == null || distance < old.distance) nearest.put(key, new Cell(x, z, distance, p.distance()));
                    }
                }
            }
            // Chunk order prevents repeatedly swapping between distant chunks.
            return nearest.values().stream().sorted(Comparator.comparingInt((Cell c) -> c.x >> 4)
                    .thenComparingInt(c -> c.z >> 4).thenComparingInt(Cell::x).thenComparingInt(Cell::z)).toList();
        });
    }
    private static void tick(MinecraftServer server) {
        if (JOBS.isEmpty()) return;
        ServerLevel level = server.getLevel(DIMENSION);
        if (level == null) { JOBS.clear(); QUEUED.clear(); return; }
        Job job = JOBS.peek();
        int remaining = 512;
        while (remaining-- > 0 && job.cells.hasNext()) paint(level, job.cells.next());
        if (!job.cells.hasNext()) {
            TrackLayout track = Tracks.get(job.id);
            finishGate(level, track);
            JOBS.remove(); QUEUED.remove(job.id);
            var player = server.getPlayerList().getPlayer(job.requestedBy);
            if (player != null) player.sendSystemMessage(Component.literal(track.title() + " is ready. /autodrive track go " + track.id()));
            AutoDrive.LOGGER.info("Built {} in AutoDrive Test Grounds", job.id);
        }
    }
    private static void paint(ServerLevel level, Cell cell) {
        int y = TrackLayout.SURFACE_Y - 1;
        Block road;
        if (cell.distance < 5.25) road = Blocks.CONCRETE.gray();
        else if (cell.distance < 6) road = Blocks.CONCRETE.white();
        else if (cell.distance < 7.5) road = ((int) (cell.along / 4) % 2 == 0) ? Blocks.CONCRETE.red() : Blocks.CONCRETE.white();
        else if (cell.distance < 10) road = Blocks.CONCRETE.lightBlue();
        else road = Blocks.SMOOTH_STONE;
        // A two-block checkerboard strip perpendicular to the start/finish tangent.
        if (cell.distance < 6 && cell.along < 1.5) {
            road = ((cell.x + cell.z) & 1) == 0 ? Blocks.CONCRETE.white() : Blocks.CONCRETE.black();
        }
        set(level, cell.x, y, cell.z, road);
        for (int dy = 1; dy <= 4; dy++) set(level, cell.x, y + dy, cell.z, dy == 1 && cell.distance >= 10 ? Blocks.SMOOTH_STONE : Blocks.AIR);
    }
    private static void finishGate(ServerLevel level, TrackLayout track) {
        var start = track.samples().getFirst();
        int x = (int) Math.round(start.x()), z = (int) Math.round(start.z()), y = TrackLayout.SURFACE_Y;
        for (int h = 0; h < 6; h++) {
            set(level, x, y + h, z - 9, Blocks.IRON_BLOCK); set(level, x, y + h, z + 9, Blocks.IRON_BLOCK);
        }
        for (int dz = -9; dz <= 9; dz++) {
            set(level, x, y + 6, z + dz, (dz & 1) == 0 ? Blocks.CONCRETE.white() : Blocks.CONCRETE.black());
            set(level, x, y + 5, z + dz, Blocks.SEA_LANTERN);
        }
        set(level, track.originX() - 14, y - 1, track.originZ() - 14, Blocks.LODESTONE);
        for (var checkpoint : checkpoints(track)) {
            int cx = (int) Math.round(checkpoint.x() + Math.cos(checkpoint.yaw()) * 8.5);
            int cz = (int) Math.round(checkpoint.z() + Math.sin(checkpoint.yaw()) * 8.5);
            set(level, cx, y - 1, cz, Blocks.SEA_LANTERN);
        }
    }
    private static void set(ServerLevel level, int x, int y, int z, Block block) {
        BlockPos pos = new BlockPos(x,y,z);
        if (!level.getBlockState(pos).is(block)) level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_CLIENTS);
    }
}
