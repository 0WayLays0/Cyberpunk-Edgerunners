package com.waylays.chromeneon.client.hud.scan;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Projects world positions onto the screen with the exact matrices the world was rendered with this frame (view
 * bobbing included), so brackets sit on their target.
 */
public final class WorldProjection {
    private final Matrix4f projection = new Matrix4f(), modelView = new Matrix4f();
    private Vec3 camera = Vec3.ZERO;
    private boolean valid;

    public void capture(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        projection.set(event.getProjectionMatrix());
        modelView.set(event.getModelViewMatrix());
        camera = event.getCamera().getPosition();
        valid = true;
    }

    /** Screen position in GUI pixels, or null behind the camera. */
    public float[] project(double x, double y, double z, int guiWidth, int guiHeight) {
        if (!valid) return null;
        Vector4f v = new Vector4f((float) (x - camera.x), (float) (y - camera.y), (float) (z - camera.z), 1F);
        modelView.transform(v);
        projection.transform(v);
        if (v.w <= 0.05F) return null;
        return new float[] {(v.x / v.w + 1F) / 2F * guiWidth, (1F - v.y / v.w) / 2F * guiHeight};
    }

    /** Screen rectangle {x0, y0, x1, y1} enclosing a box, or null if any corner is behind the camera. */
    public float[] box(AABB box, int guiWidth, int guiHeight) {
        float x0 = Float.MAX_VALUE, y0 = Float.MAX_VALUE, x1 = -Float.MAX_VALUE, y1 = -Float.MAX_VALUE;
        for (int i = 0; i < 8; i++) {
            float[] p = project((i & 1) == 0 ? box.minX : box.maxX, (i & 2) == 0 ? box.minY : box.maxY,
                    (i & 4) == 0 ? box.minZ : box.maxZ, guiWidth, guiHeight);
            if (p == null) return null;
            x0 = Math.min(x0, p[0]);
            y0 = Math.min(y0, p[1]);
            x1 = Math.max(x1, p[0]);
            y1 = Math.max(y1, p[1]);
        }
        return new float[] {x0, y0, x1, y1};
    }
}
