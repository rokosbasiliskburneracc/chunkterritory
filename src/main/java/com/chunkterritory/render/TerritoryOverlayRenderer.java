package com.chunkterritory.render;

import com.chunkterritory.territory.Territory;
import com.chunkterritory.territory.TerritoryManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.ChunkPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Collection;

/**
 * Handles rendering territory overlays on Xaero's World Map.
 *
 * Strategy:
 *  1. Try to use Xaero's public WorldMap highlight/waypoint API (preferred).
 *  2. If Xaero is absent, we skip silently — the mod still works for claiming.
 *
 * Xaero's WorldMap exposes xaero.pub.api.WorldMapSession and related classes
 * since v1.30+. We access them via reflection so the mod compiles/runs fine
 * without Xaero present.
 */
public class TerritoryOverlayRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger("ChunkTerritory/Render");
    private static boolean xaeroPresent = false;
    private static boolean initialized = false;
    private static boolean dirty = true;

    // Reflected Xaero API handles
    private static Object worldMapSession = null;
    private static Method addHighlight = null;
    private static Method clearHighlights = null;
    private static Class<?> highlightClass = null;

    public static void markDirty() {
        dirty = true;
    }

    public static void init() {
        try {
            Class.forName("xaero.map.WorldMap");
            xaeroPresent = true;
            LOGGER.info("[ChunkTerritory] Xaero's World Map detected — territory overlays enabled");
            initXaeroApi();
        } catch (ClassNotFoundException e) {
            LOGGER.info("[ChunkTerritory] Xaero's World Map not found — map overlays disabled (claiming still works)");
        }
        initialized = true;
    }

    private static void initXaeroApi() {
        // Xaero's highlight API via xaero.pub.api package
        try {
            Class<?> sessionClass = Class.forName("xaero.pub.api.WorldMapSession");
            Method getInstance = sessionClass.getMethod("getInstance");
            worldMapSession = getInstance.invoke(null);

            highlightClass = Class.forName("xaero.pub.api.ChunkHighlight");
            addHighlight = sessionClass.getMethod("addChunkHighlight", highlightClass);
            clearHighlights = sessionClass.getMethod("clearChunkHighlights");
            LOGGER.info("[ChunkTerritory] Xaero highlight API ready");
        } catch (Exception e) {
            LOGGER.warn("[ChunkTerritory] Xaero highlight API not available ({}), using fallback", e.getMessage());
            worldMapSession = null;
        }
    }

    /**
     * Called every tick to sync territory data into Xaero's highlight layer.
     */
    public static void tick() {
        if (!initialized) init();
        if (!xaeroPresent || !dirty) return;
        dirty = false;
        syncHighlights();
    }

    private static void syncHighlights() {
        if (worldMapSession == null || addHighlight == null || clearHighlights == null) {
            return;
        }
        try {
            // Clear existing territory highlights
            clearHighlights.invoke(worldMapSession);

            Collection<Territory> all = TerritoryManager.INSTANCE.getAll();
            for (Territory t : all) {
                int mapColor = t.getMapColor(); // semi-transparent ARGB
                for (Territory.ChunkKey key : t.chunks) {
                    // xaero.pub.api.ChunkHighlight(int chunkX, int chunkZ, int color, String tooltip)
                    Object highlight = tryCreateHighlight(key.x(), key.z(), mapColor, t.name + " [" + t.owner + "]");
                    if (highlight != null) {
                        addHighlight.invoke(worldMapSession, highlight);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[ChunkTerritory] Failed to sync highlights: {}", e.getMessage());
        }
    }

    private static Object tryCreateHighlight(int cx, int cz, int color, String tooltip) {
        if (highlightClass == null) return null;
        try {
            // Try constructor: (int chunkX, int chunkZ, int argbColor, String tooltip)
            try {
                return highlightClass.getConstructor(int.class, int.class, int.class, String.class)
                    .newInstance(cx, cz, color, tooltip);
            } catch (NoSuchMethodException ignored) {}

            // Fallback: (int chunkX, int chunkZ, int argbColor)
            try {
                return highlightClass.getConstructor(int.class, int.class, int.class)
                    .newInstance(cx, cz, color);
            } catch (NoSuchMethodException ignored) {}

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isXaeroPresent() {
        return xaeroPresent;
    }
}
