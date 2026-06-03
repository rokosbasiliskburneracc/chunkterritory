package com.chunkterritory.mixin;

import com.chunkterritory.territory.Territory;
import com.chunkterritory.territory.TerritoryManager;
import com.chunkterritory.render.TerritoryOverlayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pseudo-mixin: this class is loaded ONLY when Xaero's World Map is present.
 * It hooks into Xaero's GuiMap render cycle to draw our territory overlays.
 *
 * NOTE: Because Xaero's obfuscated mappings differ per version, we use
 * TerritoryOverlayRenderer which accesses Xaero via its public API where possible,
 * and falls back to a standalone chunk highlight renderer when the map is closed.
 */
@Mixin(targets = "xaero.map.gui.GuiMap", remap = false)
public class XaeroWorldMapMixin {

    @Inject(method = "render", at = @At("RETURN"), require = 0)
    private void onRender(CallbackInfo ci) {
        // Overlay is drawn by TerritoryOverlayRenderer via the WorldMapSession API
        // This hook just triggers a refresh if needed
        TerritoryOverlayRenderer.markDirty();
    }
}
