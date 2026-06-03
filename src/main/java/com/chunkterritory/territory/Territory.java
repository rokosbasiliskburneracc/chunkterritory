package com.chunkterritory.territory;

import com.google.gson.*;
import net.minecraft.util.math.ChunkPos;

import java.awt.Color;
import java.util.*;

/**
 * Represents a named, colored territory made up of one or more chunks.
 */
public class Territory {

    public final String id;
    public String name;
    public int color; // ARGB packed int
    public String owner; // player name, cosmetic only
    public final Set<ChunkKey> chunks = new HashSet<>();

    public Territory(String id, String name, int color, String owner) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.owner = owner;
    }

    public void addChunk(int chunkX, int chunkZ) {
        chunks.add(new ChunkKey(chunkX, chunkZ));
    }

    public void removeChunk(int chunkX, int chunkZ) {
        chunks.remove(new ChunkKey(chunkX, chunkZ));
    }

    public boolean containsChunk(int chunkX, int chunkZ) {
        return chunks.contains(new ChunkKey(chunkX, chunkZ));
    }

    public boolean isEmpty() {
        return chunks.isEmpty();
    }

    /** Returns a human-readable ARGB color with 50% alpha for map overlay */
    public int getMapColor() {
        // Keep RGB, set alpha to ~120 for semi-transparent overlay
        return (0x78 << 24) | (color & 0x00FFFFFF);
    }

    // ---- Serialization ----

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        obj.addProperty("name", name);
        obj.addProperty("color", color);
        obj.addProperty("owner", owner);
        JsonArray arr = new JsonArray();
        for (ChunkKey key : chunks) {
            JsonObject c = new JsonObject();
            c.addProperty("x", key.x);
            c.addProperty("z", key.z);
            arr.add(c);
        }
        obj.add("chunks", arr);
        return obj;
    }

    public static Territory fromJson(JsonObject obj) {
        String id = obj.get("id").getAsString();
        String name = obj.get("name").getAsString();
        int color = obj.get("color").getAsInt();
        String owner = obj.has("owner") ? obj.get("owner").getAsString() : "Unknown";
        Territory t = new Territory(id, name, color, owner);
        JsonArray arr = obj.getAsJsonArray("chunks");
        for (JsonElement el : arr) {
            JsonObject c = el.getAsJsonObject();
            t.addChunk(c.get("x").getAsInt(), c.get("z").getAsInt());
        }
        return t;
    }

    // ---- ChunkKey ----

    public record ChunkKey(int x, int z) {
        public static ChunkKey of(ChunkPos pos) {
            return new ChunkKey(pos.x, pos.z);
        }
    }

    // ---- Default territory colors ----
    public static final int[] PALETTE = {
        0xFF4FC3F7, // light blue
        0xFF81C784, // green
        0xFFE57373, // red
        0xFFFFB74D, // orange
        0xFFBA68C8, // purple
        0xFFFF8A65, // deep orange
        0xFF4DB6AC, // teal
        0xFFF06292, // pink
        0xFFFFD54F, // amber
        0xFF90A4AE, // blue grey
    };

    public static int nextPaletteColor(int index) {
        return PALETTE[index % PALETTE.length];
    }
}
