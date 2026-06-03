package com.chunkterritory.render;

import com.chunkterritory.territory.Territory;
import com.chunkterritory.territory.TerritoryManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.*;
import org.joml.Matrix4f;

import java.util.Collection;

/**
 * Renders colored chunk outlines in the 3D world view.
 * Active when the player is within render distance of a territory chunk.
 * This works regardless of whether Xaero is installed.
 */
public class ChunkBorderRenderer {

    private static final int OUTLINE_Y_MIN = -5;
    private static final int OUTLINE_Y_MAX = 5;
    private static final float LINE_WIDTH = 2.0f;

    public static void render(WorldRenderContext ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        Collection<Territory> territories = TerritoryManager.INSTANCE.getAll();
        if (territories.isEmpty()) return;

        MatrixStack matrices = ctx.matrixStack();
        Camera camera = ctx.camera();
        Vec3d camPos = camera.getPos();

        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.lineWidth(LINE_WIDTH);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buf = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        double playerY = client.player.getY();
        int renderY = (int) Math.round(playerY);

        for (Territory t : territories) {
            int color = t.color;
            float r = ((color >> 16) & 0xFF) / 255f;
            float g = ((color >> 8) & 0xFF) / 255f;
            float b = (color & 0xFF) / 255f;
            float a = 0.85f;

            for (Territory.ChunkKey key : t.chunks) {
                drawChunkOutline(buf, matrix, key.x(), key.z(), renderY, r, g, b, a);
            }
        }

        BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        matrices.pop();
    }

    private static void drawChunkOutline(BufferBuilder buf, Matrix4f matrix,
                                          int chunkX, int chunkZ, int y,
                                          float r, float g, float b, float a) {
        int x1 = chunkX * 16;
        int z1 = chunkZ * 16;
        int x2 = x1 + 16;
        int z2 = z1 + 16;

        // Bottom square
        line(buf, matrix, x1, y, z1, x2, y, z1, r, g, b, a);
        line(buf, matrix, x2, y, z1, x2, y, z2, r, g, b, a);
        line(buf, matrix, x2, y, z2, x1, y, z2, r, g, b, a);
        line(buf, matrix, x1, y, z2, x1, y, z1, r, g, b, a);
    }

    private static void line(BufferBuilder buf, Matrix4f m,
                              float x1, float y1, float z1,
                              float x2, float y2, float z2,
                              float r, float g, float b, float a) {
        buf.vertex(m, x1, y1, z1).color(r, g, b, a);
        buf.vertex(m, x2, y2, z2).color(r, g, b, a);
    }
}
