package com.waylays.chromeneon.client.debug;

import java.util.LinkedHashMap;
import java.util.Map;

/** Time spent drawing each part of the HUD, accumulated for the performance report of the automated test. */
public final class HudPerf {
    private static final Map<String, long[]> TIMES = new LinkedHashMap<>();

    private HudPerf() {}

    public static synchronized void record(String part, long nanos) {
        long[] t = TIMES.computeIfAbsent(part, k -> new long[3]);
        t[0] += nanos;
        t[1]++;
        t[2] = Math.max(t[2], nanos);
    }

    public static synchronized void reset() {
        TIMES.clear();
    }

    /** One line per part: average and worst time per frame, in microseconds. */
    public static synchronized String report() {
        StringBuilder sb = new StringBuilder();
        long total = 0;
        long frames = 0;
        for (var e : TIMES.entrySet()) {
            long[] t = e.getValue();
            sb.append(String.format("%-14s avg %7.1f us  max %8.1f us  (%d frames)%n", e.getKey(), t[0] / 1000.0 / Math.max(1, t[1]), t[2] / 1000.0, t[1]));
            total += t[0];
            frames = Math.max(frames, t[1]);
        }
        sb.append(String.format("%-14s avg %7.1f us per frame%n", "TOTAL", total / 1000.0 / Math.max(1, frames)));
        return sb.toString();
    }
}
