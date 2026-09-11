package com.wraith.gui.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wraith.WraithClient;
import com.wraith.module.Category;
import com.wraith.module.Module;
import com.wraith.modules.misc.HudEditor;
import com.wraith.util.InputUtil;
import com.wraith.util.MCUtil;
import com.wraith.util.ColorUtil;
import com.wraith.gui.Render2D;
import net.minecraft.client.gui.DrawContext;

/**
 * Drives HUD editing: hover highlight, drag with snap guides, right-click to
 * toggle. Rendering of each element itself stays in its own module, so the
 * editor is pure geometry and never touches game state.
 */
public final class HudManager {

    private static final float SNAP_GUIDE = 0.5f;
    private static final float[] PADDINGS = {2, 4, 8};

    private HudModule dragging;
    private float grabX;
    private float grabY;
    private HudModule hovered;

    public java.util.List<HudModule> elements() {
        return WraithClient.modules().all().stream()
                .filter(m -> m instanceof HudModule)
                .filter(m -> m.category() == Category.HUD || m.category() == Category.RENDER)
                .map(m -> (HudModule) m)
                .toList();
    }

    public boolean isDragging() { return dragging != null; }

    /** Draw pass called by the client after vanilla HUD. */
    public void frame(DrawContext context, float tickDelta) {
        HudEditor editor = WraithClient.modules().get(HudEditor.class);
        boolean editing = editor != null && editor.isEnabled();
        if (!editing) {
            hovered = null;
            dragging = null;
            return;
        }
        // Never let a HUD box steal a click that belongs to the menu on top of it.
        if (WraithClient.gui() != null && WraithClient.gui().isOpen() && !editor.ownsClicks()) return;

        // Disabled elements are drawn here (dimmed) so they still have a hit box.
        for (HudModule element : elements()) {
            if (!element.isEnabled() && editor.showHidden()) {
                element.renderBox(context, 0f);
            }
        }

        float mx = (float) InputUtil.mouseX();
        float my = (float) InputUtil.mouseY();

        if (dragging == null) {
            hovered = null;
            for (HudModule element : elements()) {
                if (element.contains(mx, my, 2)) { hovered = element; break; }
            }
            if (hovered != null && InputUtil.mouseClicked(0)) {
                dragging = hovered;
                grabX = mx - hovered.boxX();
                grabY = my - hovered.boxY();
            } else if (hovered != null && InputUtil.mouseClicked(1)) {
                hovered.toggle();
            }
        } else if (InputUtil.mouseReleased(0)) {
            dragging = null;
            WraithClient.config().save();
        } else if (InputUtil.draggedEnough(1)) {
            // A plain click must not nudge the box by a pixel, so require real travel.
            dragging.draggedTo(mx, my, grabX, grabY);
        }
        drawEditorChrome(context, editor, mx, my);
    }

    /** Guides, outlines and the label chrome for the element under the cursor. */
    private void drawEditorChrome(DrawContext context, HudEditor editor, float mx, float my) {
        int screenW = MCUtil.screenWidth();
        int screenH = MCUtil.screenHeight();
        int accent = WraithClient.modules().gui().theme().accent(180);
        int dim = ColorUtil.argb(90, 255, 255, 255);

        if (dragging != null) {
            // Padding guides: the three insets a HUD box usually wants.
            for (int pad : PADDINGS) {
                int color = pad == PADDINGS[0] ? accent : dim;
                Render2D.hline(context, pad, screenH - pad, screenW - pad * 2, color);
                Render2D.hline(context, pad, pad, screenW - pad * 2, color);
            }
            Render2D.vline(context, screenW / 2f, 0, screenH, dim);
            Render2D.hline(context, 0, screenH / 2f, screenW, dim);
        }

        for (HudModule element : elements()) {
            if (element == dragging || element == hovered) continue;
            if (!element.isEnabled() && !editor.showHidden()) continue;
            Render2D.rect(context, element.boxX() - 1, element.boxY() - 1,
                    element.boxWidth() + 2, element.boxHeight() + 2,
                    ColorUtil.argb(element.isEnabled() ? 60 : 35, 255, 255, 255));
        }

        HudModule focus = dragging != null ? dragging : hovered;
        if (focus == null) return;
        Render2D.rounded(context, focus.boxX() - 3, focus.boxY() - 3,
                focus.boxWidth() + 6, focus.boxHeight() + 6, 3, ColorUtil.withAlpha(accent, 60));
        Render2D.roundedStroke(context, focus.boxX() - 3, focus.boxY() - 3,
                focus.boxWidth() + 6, focus.boxHeight() + 6, 3, accent);

        String label = labelFor(editor, focus);
        if (label.isEmpty()) return;
        int tw = Render2D.textWidth(label);
        float lx = focus.boxX() + focus.boxWidth() / 2f - tw / 2f;
        float ly = focus.boxY() - 13;
        if (ly < 1) ly = focus.boxY() + focus.boxHeight() + 3;
        Render2D.rounded(context, lx - 3, ly - 1, tw + 6, 11, 2, ColorUtil.argb(200, 12, 12, 16));
        Render2D.text(context, label, lx, ly + 1, accent, false);
    }

    private static String labelFor(HudEditor editor, HudModule focus) {
        return switch (editor.hintMode()) {
            case "Name" -> focus.name();
            case "Off" -> "";
            default -> focus.name() + (focus.isEnabled() ? "" : " (disabled)");
        };
    }

    public JsonElement encode(String moduleName) {
        Module module = WraithClient.modules().byName(moduleName);
        if (!(module instanceof HudElementAccessor accessor)) return null;
        JsonObject json = new JsonObject();
        json.addProperty("anchor", accessor.hudAnchor().ordinal());
        json.addProperty("x", accessor.hudOffsetX());
        json.addProperty("y", accessor.hudOffsetY());
        return json;
    }

    public void decode(String moduleName, JsonElement element) {
        Module module = WraithClient.modules().byName(moduleName);
        if (!(module instanceof HudElementAccessor accessor) || element == null || !element.isJsonObject()) return;
        JsonObject json = element.getAsJsonObject();
        if (json.has("anchor")) accessor.hudSetAnchor(Anchor.fromOrdinal(json.get("anchor").getAsInt()));
        if (json.has("x")) accessor.hudSetOffsetX(json.get("x").getAsFloat());
        if (json.has("y")) accessor.hudSetOffsetY(json.get("y").getAsFloat());
    }
}
