package com.waylays.chromeneon.client.hud.nav;

import com.waylays.chromeneon.ChromeNeon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The player's own map markers, kept on this computer per world (single player) or per server address, in
 * {@code config/chromeneon/markers/}. Up to nine, numbered; each belongs to a dimension.
 */
public final class MapMarkers {
    public static final int MAX = 9;

    public record Marker(int number, String dimension, BlockPos pos) {}

    private final List<Marker> markers = new ArrayList<>();
    private Path file;

    public void load() {
        markers.clear();
        file = fileFor(Minecraft.getInstance());
        if (file == null || !Files.exists(file)) return;
        try {
            for (String line : Files.readAllLines(file)) {
                String[] p = line.trim().split("\\s+");
                if (p.length != 5) continue;
                markers.add(new Marker(Integer.parseInt(p[0]), p[1],
                        new BlockPos(Integer.parseInt(p[2]), Integer.parseInt(p[3]), Integer.parseInt(p[4]))));
            }
        } catch (IOException | NumberFormatException e) {
            ChromeNeon.LOGGER.warn("Could not read map markers from {}: {}", file, e.toString());
        }
    }

    private void save() {
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            List<String> lines = markers.stream().map(m -> m.number() + " " + m.dimension() + " "
                    + m.pos().getX() + " " + m.pos().getY() + " " + m.pos().getZ()).toList();
            Files.write(file, lines);
        } catch (IOException e) {
            ChromeNeon.LOGGER.warn("Could not save map markers to {}: {}", file, e.toString());
        }
    }

    public void unload() {
        markers.clear();
        file = null;
    }

    /** Places a marker; returns its number, or -1 when all nine are used. */
    public int add(String dimension, BlockPos pos) {
        for (int n = 1; n <= MAX; n++) {
            int number = n;
            if (markers.stream().noneMatch(m -> m.number() == number)) {
                markers.add(new Marker(n, dimension, pos.immutable()));
                save();
                return n;
            }
        }
        return -1;
    }

    /** Removes the marker closest to {@code pos} within {@code range} blocks; returns its number or -1. */
    public int removeNearest(String dimension, BlockPos pos, double range) {
        Marker best = null;
        double bestDist = range * range;
        for (Marker m : markers) {
            double d = m.pos().distSqr(pos);
            if (m.dimension().equals(dimension) && d <= bestDist) {
                best = m;
                bestDist = d;
            }
        }
        if (best == null) return -1;
        markers.remove(best);
        save();
        return best.number();
    }

    public List<Marker> in(String dimension) {
        return markers.stream().filter(m -> m.dimension().equals(dimension)).toList();
    }

    private static Path fileFor(Minecraft mc) {
        String key;
        if (mc.getSingleplayerServer() != null) {
            key = "world_" + mc.getSingleplayerServer().getWorldData().getLevelName();
        } else {
            ServerData server = mc.getCurrentServer();
            if (server == null) return null;
            key = "server_" + server.ip;
        }
        return FMLPaths.CONFIGDIR.get().resolve("chromeneon/markers/" + key.replaceAll("[^A-Za-z0-9._-]", "_") + ".txt");
    }
}
