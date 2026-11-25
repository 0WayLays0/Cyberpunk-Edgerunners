package com.waylays.chromeneon.client.hud.nav;

/**
 * The transform between the world and the minimap: centred on the player, {@code zoom} blocks per pixel, turned
 * so the player faces up when {@code rotate} (north up otherwise).
 */
public record MapView(double centerX, double centerZ, float yawDegrees, boolean rotate, float zoom, float size) {
    private double angle() {
        return rotate ? Math.toRadians(yawDegrees) : Math.PI;
    }

    /** Map pixel offset from the centre (right, down) to world position. */
    public double[] toWorld(double dx, double dy) {
        double a = angle(), fx = -Math.sin(a), fz = Math.cos(a), rx = -Math.cos(a), rz = -Math.sin(a);
        return new double[] {centerX + (rx * dx - fx * dy) * zoom, centerZ + (rz * dx - fz * dy) * zoom};
    }

    /**
     * The map's axes in the world, {right x, right z, up x, up z} in blocks per pixel: one pixel right of the
     * centre is {@code centre + right}, one pixel up is {@code centre + up}. Computed once per frame for the image.
     */
    public double[] axes() {
        double a = angle();
        return new double[] {-Math.cos(a) * zoom, -Math.sin(a) * zoom, -Math.sin(a) * zoom, Math.cos(a) * zoom};
    }

    /** World position to map pixel offset from the centre (right, down). */
    public float[] toMap(double wx, double wz) {
        double a = angle(), fx = -Math.sin(a), fz = Math.cos(a), rx = -Math.cos(a), rz = -Math.sin(a);
        double dx = (wx - centerX) / zoom, dz = (wz - centerZ) / zoom;
        return new float[] {(float) (dx * rx + dz * rz), (float) -(dx * fx + dz * fz)};
    }
}
