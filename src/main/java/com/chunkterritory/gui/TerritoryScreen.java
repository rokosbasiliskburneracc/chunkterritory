package com.chunkterritory.gui;

import com.chunkterritory.territory.Territory;
import com.chunkterritory.territory.TerritoryManager;
import com.chunkterritory.render.TerritoryOverlayRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;

import java.util.*;

/**
 * Main territory management GUI.
 * Open with the keybind (default: Y).
 *
 * Features:
 *  - List all territories
 *  - Create new territory
 *  - Rename / recolor / delete
 *  - Claim/unclaim current chunk for selected territory
 *  - Export/import share codes
 */
public class TerritoryScreen extends Screen {

    private static final int PANEL_WIDTH = 200;
    private static final int LIST_ITEM_H = 22;

    private final Screen parent;
    private List<Territory> territoryList = new ArrayList<>();
    private int selectedIndex = -1;

    // Scroll
    private int scrollOffset = 0;
    private static final int VISIBLE_ROWS = 10;

    // Bottom buttons
    private ButtonWidget btnClaim;
    private ButtonWidget btnUnclaim;
    private ButtonWidget btnCreate;
    private ButtonWidget btnDelete;
    private ButtonWidget btnExport;
    private ButtonWidget btnImport;
    private ButtonWidget btnRename;

    // Share code text field
    private TextFieldWidget shareField;
    private String statusMessage = "";
    private int statusTimer = 0;

    public TerritoryScreen(Screen parent) {
        super(Text.literal("Chunk Territory"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        refreshList();
        int panelX = width / 2 - PANEL_WIDTH / 2;
        int listBottom = height - 130;

        // --- Bottom action bar ---
        int bx = panelX;
        int by = listBottom + 8;

        btnClaim = addDrawableChild(ButtonWidget.builder(Text.literal("Claim Chunk"), b -> claimCurrent())
            .dimensions(bx, by, 95, 20).build());
        btnUnclaim = addDrawableChild(ButtonWidget.builder(Text.literal("Unclaim Chunk"), b -> unclaimCurrent())
            .dimensions(bx + 100, by, 100, 20).build());

        by += 24;
        btnCreate = addDrawableChild(ButtonWidget.builder(Text.literal("+ New Territory"), b -> openCreate())
            .dimensions(bx, by, 95, 20).build());
        btnRename = addDrawableChild(ButtonWidget.builder(Text.literal("Rename"), b -> openRename())
            .dimensions(bx + 100, by, 95, 20).build());

        by += 24;
        btnDelete = addDrawableChild(ButtonWidget.builder(Text.literal("Delete"), b -> deleteSelected())
            .dimensions(bx, by, 95, 20).build());

        by += 28;
        // Share code
        shareField = addDrawableChild(new TextFieldWidget(textRenderer, bx, by, PANEL_WIDTH - 55, 20,
            Text.literal("Share code")));
        shareField.setMaxLength(2048);
        shareField.setPlaceholder(Text.literal("Paste share code here…"));

        btnImport = addDrawableChild(ButtonWidget.builder(Text.literal("Import"), b -> doImport())
            .dimensions(bx + PANEL_WIDTH - 52, by, 52, 20).build());

        by += 24;
        btnExport = addDrawableChild(ButtonWidget.builder(Text.literal("Export All"), b -> doExport())
            .dimensions(bx, by, 95, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> close())
            .dimensions(bx + 100, by, 100, 20).build());

        updateButtonStates();
    }

    private void refreshList() {
        territoryList = new ArrayList<>(TerritoryManager.INSTANCE.getAll());
        if (selectedIndex >= territoryList.size()) selectedIndex = territoryList.size() - 1;
    }

    private void updateButtonStates() {
        boolean sel = selectedIndex >= 0 && selectedIndex < territoryList.size();
        if (btnClaim != null) btnClaim.active = sel;
        if (btnUnclaim != null) btnUnclaim.active = true;
        if (btnDelete != null) btnDelete.active = sel;
        if (btnRename != null) btnRename.active = sel;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);

        int panelX = width / 2 - PANEL_WIDTH / 2;
        int listTop = 40;
        int listBottom = height - 130;
        int listH = listBottom - listTop;

        // Panel background
        ctx.fill(panelX - 4, listTop - 4, panelX + PANEL_WIDTH + 4, listBottom + 4, 0xAA000000);
        ctx.drawBorder(panelX - 4, listTop - 4, PANEL_WIDTH + 8, listH + 8, 0xFF555555);

        // Title
        ctx.drawCenteredTextWithShadow(textRenderer, "Chunk Territory", width / 2, listTop - 16, 0xFFFFFF);

        // Territory list
        int visibleCount = Math.min(VISIBLE_ROWS, territoryList.size());
        for (int i = 0; i < visibleCount; i++) {
            int realIndex = i + scrollOffset;
            if (realIndex >= territoryList.size()) break;
            Territory t = territoryList.get(realIndex);

            int rowY = listTop + i * LIST_ITEM_H;
            boolean hovered = mouseX >= panelX && mouseX <= panelX + PANEL_WIDTH
                && mouseY >= rowY && mouseY < rowY + LIST_ITEM_H;
            boolean selected = realIndex == selectedIndex;

            // Row bg
            int rowBg = selected ? 0xFF335577 : (hovered ? 0xFF2A2A2A : 0xFF1A1A1A);
            ctx.fill(panelX, rowY, panelX + PANEL_WIDTH, rowY + LIST_ITEM_H - 1, rowBg);

            // Color swatch
            int swatchColor = t.color | 0xFF000000;
            ctx.fill(panelX + 4, rowY + 5, panelX + 14, rowY + LIST_ITEM_H - 5, swatchColor);

            // Name + chunk count
            String label = t.name + " (" + t.chunks.size() + ")";
            if (textRenderer.getWidth(label) > PANEL_WIDTH - 24) {
                label = textRenderer.trimToWidth(label, PANEL_WIDTH - 30) + "…";
            }
            ctx.drawTextWithShadow(textRenderer, label, panelX + 18, rowY + 7, 0xFFFFFF);
        }

        if (territoryList.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, "No territories yet. Create one!",
                width / 2, listTop + 20, 0xAAAAAA);
        }

        // Status message
        if (statusTimer > 0) {
            statusTimer--;
            ctx.drawCenteredTextWithShadow(textRenderer, statusMessage, width / 2, height - 14, 0xFFFF55);
        }

        // Hover tooltip: show territory name + owner when hovering list
        for (int i = 0; i < visibleCount; i++) {
            int realIndex = i + scrollOffset;
            if (realIndex >= territoryList.size()) break;
            Territory t = territoryList.get(realIndex);
            int rowY = listTop + i * LIST_ITEM_H;
            if (mouseX >= panelX && mouseX <= panelX + PANEL_WIDTH && mouseY >= rowY && mouseY < rowY + LIST_ITEM_H) {
                List<Text> tooltip = List.of(
                    Text.literal(t.name).styled(s -> s.withBold(true)),
                    Text.literal("Owner: " + t.owner),
                    Text.literal("Chunks: " + t.chunks.size()),
                    Text.literal("ID: " + t.id)
                );
                ctx.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int panelX = width / 2 - PANEL_WIDTH / 2;
        int listTop = 40;
        int visibleCount = Math.min(VISIBLE_ROWS, territoryList.size());
        for (int i = 0; i < visibleCount; i++) {
            int rowY = listTop + i * LIST_ITEM_H;
            if (mx >= panelX && mx <= panelX + PANEL_WIDTH && my >= rowY && my < rowY + LIST_ITEM_H) {
                int realIndex = i + scrollOffset;
                if (realIndex < territoryList.size()) {
                    selectedIndex = realIndex;
                    updateButtonStates();
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hScroll, double vScroll) {
        scrollOffset = Math.max(0, Math.min(scrollOffset - (int) Math.signum(vScroll),
            Math.max(0, territoryList.size() - VISIBLE_ROWS)));
        return true;
    }

    // ---- Actions ----

    private void claimCurrent() {
        if (selectedIndex < 0 || selectedIndex >= territoryList.size()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        Territory t = territoryList.get(selectedIndex);
        ChunkPos pos = client.player.getChunkPos();
        boolean claimed = TerritoryManager.INSTANCE.claimChunk(t, pos.x, pos.z);
        setStatus(claimed ? "Claimed chunk " + pos.x + "," + pos.z + " for '" + t.name + "'"
            : "Already owned by this territory");
        refreshList();
        TerritoryOverlayRenderer.markDirty();
    }

    private void unclaimCurrent() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        ChunkPos pos = client.player.getChunkPos();
        boolean removed = TerritoryManager.INSTANCE.unclaimChunk(pos.x, pos.z);
        setStatus(removed ? "Unclaimed chunk " + pos.x + "," + pos.z : "Chunk wasn't claimed");
        refreshList();
        TerritoryOverlayRenderer.markDirty();
    }

    private void openCreate() {
        MinecraftClient.getInstance().setScreen(new CreateTerritoryScreen(this));
    }

    private void openRename() {
        if (selectedIndex < 0 || selectedIndex >= territoryList.size()) return;
        Territory t = territoryList.get(selectedIndex);
        MinecraftClient.getInstance().setScreen(new RenameTerritoryScreen(this, t));
    }

    private void deleteSelected() {
        if (selectedIndex < 0 || selectedIndex >= territoryList.size()) return;
        Territory t = territoryList.get(selectedIndex);
        TerritoryManager.INSTANCE.deleteTerritory(t.id);
        refreshList();
        selectedIndex = Math.max(0, selectedIndex - 1);
        updateButtonStates();
        setStatus("Deleted '" + t.name + "'");
        TerritoryOverlayRenderer.markDirty();
    }

    private void doExport() {
        String code = TerritoryManager.INSTANCE.exportShareCode();
        if (code != null) {
            MinecraftClient.getInstance().keyboard.setClipboard(code);
            shareField.setText(code);
            setStatus("Share code copied to clipboard!");
        } else {
            setStatus("Export failed.");
        }
    }

    private void doImport() {
        String code = shareField.getText().trim();
        if (code.isEmpty()) {
            setStatus("Paste a share code first!");
            return;
        }
        int count = TerritoryManager.INSTANCE.importShareCode(code);
        if (count >= 0) {
            refreshList();
            setStatus("Imported " + count + " territories!");
            TerritoryOverlayRenderer.markDirty();
        } else {
            setStatus("Invalid share code.");
        }
    }

    public void onTerritoryCreated() {
        refreshList();
        selectedIndex = territoryList.size() - 1;
        updateButtonStates();
        TerritoryOverlayRenderer.markDirty();
    }

    private void setStatus(String msg) {
        statusMessage = msg;
        statusTimer = 120;
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }
}
