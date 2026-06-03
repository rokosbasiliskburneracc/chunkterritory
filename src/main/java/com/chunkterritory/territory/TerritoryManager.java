package com.chunkterritory.territory;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.ChunkPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import java.util.Base64;

public class TerritoryManager {

    public static final TerritoryManager INSTANCE = new TerritoryManager();
    private static final Logger LOGGER = LoggerFactory.getLogger("ChunkTerritory");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // territoryId -> Territory
    private final Map<String, Territory> territories = new LinkedHashMap<>();
    // chunkKey -> territoryId (for fast lookup)
    private final Map<Territory.ChunkKey, String> chunkIndex = new HashMap<>();

    private String currentWorldId = null;
    private int colorCounter = 0;

    private TerritoryManager() {}

    // =====================
    //   World lifecycle
    // =====================

    public void onWorldJoin(String worldId) {
        this.currentWorldId = worldId;
        this.territories.clear();
        this.chunkIndex.clear();
        this.colorCounter = 0;
        load();
        LOGGER.info("[ChunkTerritory] Loaded {} territories for world '{}'", territories.size(), worldId);
    }

    public void onWorldLeave() {
        if (currentWorldId != null) save();
        this.currentWorldId = null;
        this.territories.clear();
        this.chunkIndex.clear();
    }

    // =====================
    //   Territory CRUD
    // =====================

    public Territory createTerritory(String name, String ownerName) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        int color = Territory.nextPaletteColor(colorCounter++);
        Territory t = new Territory(id, name, color, ownerName);
        territories.put(id, t);
        save();
        return t;
    }

    public void deleteTerritory(String id) {
        Territory t = territories.remove(id);
        if (t != null) {
            for (Territory.ChunkKey key : t.chunks) {
                chunkIndex.remove(key);
            }
            save();
        }
    }

    public Collection<Territory> getAll() {
        return Collections.unmodifiableCollection(territories.values());
    }

    public Territory getById(String id) {
        return territories.get(id);
    }

    /** Returns the territory that owns this chunk, or null */
    public Territory getTerritoryAt(int chunkX, int chunkZ) {
        String id = chunkIndex.get(new Territory.ChunkKey(chunkX, chunkZ));
        return id != null ? territories.get(id) : null;
    }

    public Territory getTerritoryAt(ChunkPos pos) {
        return getTerritoryAt(pos.x, pos.z);
    }

    // =====================
    //   Chunk claiming
    // =====================

    /**
     * Claims a chunk for a territory. Removes it from any previous owner.
     * Returns false if the chunk was already owned by this territory.
     */
    public boolean claimChunk(Territory territory, int chunkX, int chunkZ) {
        Territory.ChunkKey key = new Territory.ChunkKey(chunkX, chunkZ);
        String existing = chunkIndex.get(key);
        if (territory.id.equals(existing)) return false;

        // Remove from previous owner
        if (existing != null) {
            Territory prev = territories.get(existing);
            if (prev != null) prev.removeChunk(chunkX, chunkZ);
        }

        territory.addChunk(chunkX, chunkZ);
        chunkIndex.put(key, territory.id);
        save();
        return true;
    }

    /**
     * Unclaims a chunk from whatever territory owns it.
     */
    public boolean unclaimChunk(int chunkX, int chunkZ) {
        Territory.ChunkKey key = new Territory.ChunkKey(chunkX, chunkZ);
        String id = chunkIndex.remove(key);
        if (id == null) return false;
        Territory t = territories.get(id);
        if (t != null) t.removeChunk(chunkX, chunkZ);
        save();
        return true;
    }

    public boolean isChunkClaimed(int chunkX, int chunkZ) {
        return chunkIndex.containsKey(new Territory.ChunkKey(chunkX, chunkZ));
    }

    // =====================
    //   Save / Load
    // =====================

    private Path getSaveFile() {
        String safeId = currentWorldId == null ? "unknown" : currentWorldId.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        return FabricLoader.getInstance().getConfigDir()
            .resolve("chunkterritory")
            .resolve(safeId + ".json");
    }

    public void save() {
        if (currentWorldId == null) return;
        try {
            Path file = getSaveFile();
            Files.createDirectories(file.getParent());
            JsonArray arr = new JsonArray();
            for (Territory t : territories.values()) arr.add(t.toJson());
            Files.writeString(file, GSON.toJson(arr), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("[ChunkTerritory] Failed to save territories", e);
        }
    }

    private void load() {
        Path file = getSaveFile();
        if (!Files.exists(file)) return;
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
            for (JsonElement el : arr) {
                Territory t = Territory.fromJson(el.getAsJsonObject());
                territories.put(t.id, t);
                for (Territory.ChunkKey key : t.chunks) {
                    chunkIndex.put(key, t.id);
                }
                colorCounter++;
            }
        } catch (Exception e) {
            LOGGER.error("[ChunkTerritory] Failed to load territories", e);
        }
    }

    // =====================
    //   Share codes
    // =====================

    /**
     * Exports ALL territories to a compact Base64 share code.
     */
    public String exportShareCode() {
        try {
            JsonArray arr = new JsonArray();
            for (Territory t : territories.values()) arr.add(t.toJson());
            byte[] json = GSON.toJson(arr).getBytes(StandardCharsets.UTF_8);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
                gzip.write(json);
            }
            return "CT1:" + Base64.getUrlEncoder().withoutPadding().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            LOGGER.error("[ChunkTerritory] Export failed", e);
            return null;
        }
    }

    /**
     * Imports territories from a share code. Existing territories with the same ID
     * are overwritten; others are kept.
     */
    public int importShareCode(String code) {
        try {
            if (!code.startsWith("CT1:")) throw new IllegalArgumentException("Invalid share code prefix");
            byte[] compressed = Base64.getUrlDecoder().decode(code.substring(4));

            ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
            String json;
            try (GZIPInputStream gzip = new GZIPInputStream(bais)) {
                json = new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
            }

            JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
            int imported = 0;
            for (JsonElement el : arr) {
                Territory t = Territory.fromJson(el.getAsJsonObject());
                // Remove old index entries for this territory
                Territory old = territories.get(t.id);
                if (old != null) {
                    for (Territory.ChunkKey key : old.chunks) chunkIndex.remove(key);
                }
                territories.put(t.id, t);
                for (Territory.ChunkKey key : t.chunks) chunkIndex.put(key, t.id);
                imported++;
            }
            save();
            return imported;
        } catch (Exception e) {
            LOGGER.error("[ChunkTerritory] Import failed", e);
            return -1;
        }
    }

    // =====================
    //   Utility
    // =====================

    public String getCurrentWorldId() { return currentWorldId; }

    /** Derive a stable world ID from server address or singleplayer level name */
    public static String deriveWorldId(MinecraftClient client) {
        if (client.getServer() != null) {
            // Singleplayer
            return "sp_" + client.getServer().getSavePath(net.minecraft.world.level.storage.LevelStorage.SESSION_ID_PATH).toString();
        } else if (client.getCurrentServerEntry() != null) {
            return "mp_" + client.getCurrentServerEntry().address;
        }
        return "unknown";
    }
}
