package com.chunkterritory;

import com.chunkterritory.gui.TerritoryScreen;
import com.chunkterritory.render.ChunkBorderRenderer;
import com.chunkterritory.render.TerritoryOverlayRenderer;
import com.chunkterritory.territory.TerritoryManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChunkTerritoryClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("ChunkTerritory");
    public static final String MOD_ID = "chunkterritory";

    // Keybind: Y to open GUI
    public static KeyBinding openGuiKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[ChunkTerritory] Initializing...");

        // Register keybind
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.chunkterritory.open_gui",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_Y,
            "category.chunkterritory"
        ));

        // World join/leave events
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            String worldId = TerritoryManager.deriveWorldId(client);
            TerritoryManager.INSTANCE.onWorldJoin(worldId);
            TerritoryOverlayRenderer.init();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            TerritoryManager.INSTANCE.onWorldLeave();
        });

        // Client tick: handle keybind press + overlay sync
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                client.setScreen(new TerritoryScreen(null));
            }
            TerritoryOverlayRenderer.tick();
        });

        // World render: draw chunk borders in 3D
        WorldRenderEvents.AFTER_ENTITIES.register(ChunkBorderRenderer::render);

        LOGGER.info("[ChunkTerritory] Ready! Press Y to manage territories.");
    }
}
