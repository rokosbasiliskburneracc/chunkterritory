package com.chunkterritory.gui;

import com.chunkterritory.territory.Territory;
import com.chunkterritory.territory.TerritoryManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;

/**
 * Small dialog for creating a new territory with a name and color picker.
 */
public class CreateTerritoryScreen extends Screen {

    private final TerritoryScreen parent;
    private TextFieldWidget nameField;
    private int selectedColorIndex = 0;

    private static final int SWATCH_SIZE = 20;
    private static final int SWATCH_GAP = 4;

    public CreateTerritoryScreen(TerritoryScreen parent) {
        super(Text.literal("New Territory"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2 - 40;

        nameField = addDrawableChild(new TextFieldWidget(textRenderer, cx - 80, cy, 160, 20,
            Text.literal("Territory name")));
        nameField.setMaxLength(32);
        nameField.setFocused(true);
        nameField.setPlaceholder(Text.literal("e.g. My Base, Farm, Jungle"));

        addDrawableChild(ButtonWidget.builder(Text.literal("Create"), b -> doCreate())
            .dimensions(cx - 42, cy + 80, 85, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> close())
            .dimensions(cx - 42, cy + 104, 85, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);

        int cx = width / 2;
        int cy = height / 2 - 40;

        ctx.drawCenteredTextWithShadow(textRenderer, "New Territory", cx, cy - 20, 0xFFFFFF);
        ctx.drawTextWithShadow(textRenderer, "Name:", cx - 80, cy - 5, 0xAAAAAA);

        // Color swatches
        ctx.drawTextWithShadow(textRenderer, "Color:", cx - 80, cy + 28, 0xAAAAAA);
        int colors = Territory.PALETTE.length;
        int totalW = colors * (SWATCH_SIZE + SWATCH_GAP) - SWATCH_GAP;
        int startX = cx - totalW / 2;
        for (int i = 0; i < colors; i++) {
            int sx = startX + i * (SWATCH_SIZE + SWATCH_GAP);
            int sy = cy + 40;
            int col = Territory.PALETTE[i] | 0xFF000000;
            ctx.fill(sx, sy, sx + SWATCH_SIZE, sy + SWATCH_SIZE, col);
            if (i == selectedColorIndex) {
                ctx.drawBorder(sx - 2, sy - 2, SWATCH_SIZE + 4, SWATCH_SIZE + 4, 0xFFFFFFFF);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int cx = width / 2;
        int cy = height / 2 - 40;
        int colors = Territory.PALETTE.length;
        int totalW = colors * (SWATCH_SIZE + SWATCH_GAP) - SWATCH_GAP;
        int startX = cx - totalW / 2;
        int swatchY = cy + 40;

        for (int i = 0; i < colors; i++) {
            int sx = startX + i * (SWATCH_SIZE + SWATCH_GAP);
            if (mx >= sx && mx <= sx + SWATCH_SIZE && my >= swatchY && my <= swatchY + SWATCH_SIZE) {
                selectedColorIndex = i;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) { // Enter / numpad Enter
            doCreate();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void doCreate() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) name = "Territory";
        MinecraftClient client = MinecraftClient.getInstance();
        String owner = client.player != null ? client.player.getName().getString() : "Unknown";

        Territory t = TerritoryManager.INSTANCE.createTerritory(name, owner);
        // Override with chosen color
        t.color = Territory.PALETTE[selectedColorIndex];
        TerritoryManager.INSTANCE.save();

        parent.onTerritoryCreated();
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }
}
