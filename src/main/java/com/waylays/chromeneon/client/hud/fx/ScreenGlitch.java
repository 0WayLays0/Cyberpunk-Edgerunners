package com.waylays.chromeneon.client.hud.fx;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.waylays.chromeneon.ChromeNeon;
import com.waylays.chromeneon.client.hud.HudFrame;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.nio.file.Path;
import java.util.List;

/**
 * The real screen distortion of a glitch: the rendered world is copied into a texture (straight from the frame
 * being drawn), then torn slices of it are drawn back shifted sideways with a red and a cyan ghost. Runs only
 * during a glitch, before the HUD is drawn.
 */
public final class ScreenGlitch {
    private int texture = -1, texW, texH;
    private boolean broken;

    public void render(GuiGraphics g, HudFrame f, List<GlitchController.Band> bands, float strength) {
        if (bands.isEmpty() || broken) return;
        try {
            capture(g);
            draw(g, f, bands, strength);
        } catch (RuntimeException e) {
            // Keep the HUD's own glitch (jitter) and drop the tearing on a driver that refuses the copy
            broken = true;
            ChromeNeon.LOGGER.warn("Screen glitch disabled: {}", e.toString());
        }
    }

    /** Copies the colour of the frame drawn so far (the world) into our texture. */
    private void capture(GuiGraphics g) {
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        int w = main.width, h = main.height;
        if (texture < 0 || w != texW || h != texH) {
            if (texture >= 0) TextureUtil.releaseTextureId(texture);
            texture = TextureUtil.generateTextureId();
            TextureUtil.prepareImage(NativeImage.InternalGlFormat.RGBA, texture, w, h);
            RenderSystem.bindTexture(texture);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            texW = w;
            texH = h;
        }
        g.flush();
        main.bindWrite(false);
        RenderSystem.bindTexture(texture);
        GlStateManager._glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);
    }

    private void draw(GuiGraphics g, HudFrame f, List<GlitchController.Band> bands, float strength) {
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Matrix4f m = g.pose().last().pose();
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (GlitchController.Band band : bands) {
            float y0 = band.y() * f.height, y1 = Math.min(f.height, y0 + Math.max(1F, band.height() * f.height));
            float shift = band.shift() * 14F * strength;
            slice(bb, m, f, y0, y1, shift, 0xFFFFFFFF);
            slice(bb, m, f, y0, y1, shift + 2.5F * strength, 0x66FF2B4E);
            slice(bb, m, f, y0, y1, shift - 2.5F * strength, 0x661EF2FF);
        }
        BufferUploader.drawWithShader(bb.buildOrThrow());
    }

    /** One slice: the band of the copied image (rows y0..y1, flipped: textures start at the bottom), shifted. */
    private static void slice(BufferBuilder bb, Matrix4f m, HudFrame f, float y0, float y1, float shift, int argb) {
        float v0 = 1F - y0 / f.height, v1 = 1F - y1 / f.height;
        bb.addVertex(m, shift, y0, 0).setUv(0, v0).setColor(argb);
        bb.addVertex(m, shift, y1, 0).setUv(0, v1).setColor(argb);
        bb.addVertex(m, f.width + shift, y1, 0).setUv(1, v1).setColor(argb);
        bb.addVertex(m, f.width + shift, y0, 0).setUv(1, v0).setColor(argb);
    }

    /** Saves the last captured image (for the automated test): proof of what the tearing draws from. */
    public void dump(Path file) throws java.io.IOException {
        if (texture < 0) return;
        try (NativeImage image = new NativeImage(texW, texH, false)) {
            RenderSystem.bindTexture(texture);
            image.downloadTexture(0, false);
            image.flipY();
            image.writeToFile(file);
        }
    }
}
