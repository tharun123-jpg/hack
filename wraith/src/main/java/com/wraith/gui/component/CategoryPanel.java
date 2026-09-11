package com.wraith.gui.component;

import com.wraith.gui.ClickGui;
import com.wraith.gui.Render2D;
import com.wraith.gui.Theme;
import com.wraith.module.Category;
import com.wraith.module.Module;
import com.wraith.util.ColorUtil;
import com.wraith.util.InputUtil;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/**
 * One category = one draggable header with a stack of {@link ModuleCard}s.
 * Collapsed it is only the header, which keeps the default layout as compact as
 * the reference client while still allowing everything to be dragged apart.
 */
public final class CategoryPanel {

    private final Category category;
    private final ClickGui gui;
    private final List<ModuleCard> cards = new ArrayList<>();

    private float x;
    private float y;
    private float height;
    private boolean collapsed = true;
    private boolean moved;
    private float grabX;
    private float grabY;
    private float hover;

    public CategoryPanel(Category category, List<Module> modules, ClickGui gui) {
        this.category = category;
        this.gui = gui;
        for (Module module : modules) cards.add(new ModuleCard(module, gui));
    }

    public Category category() { return category; }
    public boolean collapsed() { return collapsed; }
    public void collapsed(boolean collapsed) { this.collapsed = collapsed; }
    public void x(float x) { this.x = x; }
    public void y(float y) { this.y = y; }
    public float x() { return x; }
    public float y() { return y; }

    /** Width reserved in the auto row: header only when collapsed. */
    public float occupiedWidth(Theme theme) {
        return collapsed ? Math.max(48f, Render2D.textWidth(category.label()) + 22f) : theme.panelWidth();
    }

    public boolean movable() { return !moved; }

    public void render(DrawContext context, Theme theme, float tickDelta, float openness, int screenW, int screenH) {
        float w = theme.panelWidth();
        float headerH = theme.rowHeight();
        float mx = (float) InputUtil.mouseX();
        float my = (float) InputUtil.mouseY();
        boolean overHeader = Render2D.inBox(mx, my, x, y, collapsed ? occupiedWidth(theme) : w, headerH);

        hover = Render2D.animate(hover, overHeader ? 1f : 0f, theme.animationSpeed());

        float bodyH = 0f;
        for (ModuleCard card : cards) bodyH += card.height(theme);
        height = headerH + (collapsed ? 0 : bodyH + 3f);

        float alphaScale = openness;
        int panelColor = ColorUtil.withAlpha(theme.panel(), (int) (ColorUtil.alpha(theme.panel()) * alphaScale));
        int headerColor = ColorUtil.blend(ColorUtil.withAlpha(theme.accentDeep(255), (int) (255 * alphaScale)),
                panelColor, 1f - hover);

        // Header: the grab handle and the collapse switch.
        Render2D.rounded(context, x, y, collapsed ? occupiedWidth(theme) : w, headerH,
                collapsed ? theme.radius() : theme.radius(), headerColor);
        if (!collapsed) {
            // Square off the header's bottom corners so it reads as one panel.
            Render2D.rect(context, x, y + headerH - theme.radius(), collapsed ? occupiedWidth(theme) : w,
                    theme.radius(), headerColor);
            Render2D.rounded(context, x, y, w, height, theme.radius(), ColorUtil.withAlpha(panelColor, (int) (255 * alphaScale)));
        }
        String label = category.label();
        int textW = Render2D.textWidth(label);
        Render2D.text(context, label, x + (collapsed ? occupiedWidth(theme) : w) / 2f - textW / 2f,
                y + (headerH - Render2D.FONT_HEIGHT) / 2f + 1, ColorUtil.withAlpha(theme.text(), (int) (255 * openness)), false);

        // Active-state tick: one accent pixel under the header says "on screen".
        if (!collapsed) {
            Render2D.hline(context, x + 3, y + headerH, w - 6, theme.accent((int) (200 * openness)));
        }

        if (overHeader && InputUtil.mouseDown(0)) {
            if (!moved && InputUtil.draggedEnough(3f)) {
                moved = true;
                grabX = mx - x;
                grabY = my - y;
            }
            if (moved) {
                x = clamp(mx - grabX, 2, screenW - (collapsed ? occupiedWidth(theme) : w) - 2);
                y = clamp(my - grabY, 2, screenH - headerH - 2);
                gui.consumeClick();
            }
        }

        if (overHeader && InputUtil.mouseClicked(0) && !moved) {
            collapsed = !collapsed;
            gui.consumeClick();
        }
        // Release anywhere ends the drag: requiring the cursor to still be over the
        // header would leave the panel pinned out of the auto row forever.
        if (moved && InputUtil.mouseReleased(0)) {
            moved = false;
        }

        if (collapsed) return;

        float cursor = y + headerH + 2f;
        Render2D.clipped(context, x, cursor - 1, w, Math.max(0, height - headerH - 1), () -> {
            float inner = cursor;
            for (ModuleCard card : cards) {
                card.render(context, theme, x + 2, inner, w - 4, tickDelta, openness);
                inner += card.height(theme);
            }
        });
    }

    /** Auto-layout puts the panel back in the row; dragging opts it out. */
    public void snapToRow(float rowX) {
        if (!moved) {
            x = rowX;
            y = 4;
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.min(max, Math.max(min, value));
    }

}
