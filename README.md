# Chunk Territory Mod

A **client-side only** Fabric mod for Minecraft 1.21.1 that lets you and your friends mark, name, and color chunk territories — with optional Xaero's World Map integration showing colored chunk overlays with hover tooltips.

**No server mod required.**

---

## Features

- **Claim chunks** for named territories (press `Y` while standing in a chunk)
- **Color territories** with a 10-color palette
- **See colored chunk outlines** in the 3D world (glowing borders at your feet)
- **Xaero's World Map integration** — territories show as semi-transparent colored regions on the map with name tooltips on hover
- **Share codes** — export all your territories to a compact code, send it to friends, they import and see your layout instantly
- Saves per server/world automatically

---

## How to Use

| Action | How |
|--------|-----|
| Open Territory Manager | Press `Y` (rebindable in Controls) |
| Claim a chunk | Select territory → stand in chunk → click "Claim Chunk" |
| Unclaim a chunk | Stand in chunk → click "Unclaim Chunk" |
| Create territory | Click "+ New Territory", type a name, pick a color |
| Rename / recolor | Select territory → click "Rename" |
| Delete | Select territory → click "Delete" |
| Export all to share code | Click "Export All" — code is copied to clipboard |
| Import friend's territories | Paste their code → click "Import" |

---

## Building

### Requirements
- Java 21 (must be on your PATH)
- Internet connection (Gradle downloads dependencies on first build)

### Steps

**Windows:**
```
gradlew.bat build
```

**Mac/Linux:**
```
./gradlew build
```

The compiled `.jar` will be at:
```
build/libs/chunkterritory-1.0.0.jar
```

Drop it in your `.minecraft/mods/` folder alongside Fabric API.

---

## Dependencies

**Required:**
- Fabric Loader 0.16+
- Fabric API

**Optional (but recommended):**
- [Xaero's World Map](https://modrinth.com/mod/xaeros-world-map) — enables colored chunk overlays on the map

---

## Xaero's World Map Integration

When Xaero's World Map is installed, territories appear as colored semi-transparent overlays on the map. Hovering a colored chunk shows the territory name and owner.

The mod detects Xaero automatically — if Xaero isn't installed, chunk border outlines still render in the 3D world.

> **Note:** Xaero's public highlight API (`xaero.pub.api`) was introduced in WorldMap v1.30. If you're on an older version, 3D borders still work but map overlays won't appear.

---

## Share Code Format

Share codes are prefixed with `CT1:` followed by Base64-encoded gzipped JSON. They encode all territories including chunk coordinates, names, colors, and owner names. Safe to paste in chat or Discord.

Example:
```
CT1:H4sIAAAAAAAA_6tWKkktLlGyUlIqS04tLk4tBgBnKfxTGAAAAA==
```

---

## File Locations

Territory data is saved to:
```
.minecraft/config/chunkterritory/<world_id>.json
```

One file per server address / singleplayer world.
