package com.wraith.gui;

import com.wraith.WraithClient;
import com.wraith.gui.component.CategoryPanel;
import com.wraith.module.Category;
import com.wraith.module.Module;
import com.wraith.module.Setting;
import com.wraith.modules.misc.ClickGuiModule;
import com.wraith.util.InputUtil;
import com.wraith.util.MCUtil;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * The overlay menu: one draggable panel per category, themed by {@link Theme}.
 *
 * Two rules make this behave like a real client GUI instead of a mod config
 * screen. First, input is polled per rendered frame and consumed the moment a
 * widget takes it, so the mouse can never click "through" a panel into the world.
 * Second, the game is never paused - no Screen is opened at all.
 */
public final class ClickGui {

    private final List<CategoryPanel> panels = new ArrayList<>();
    private final CategoryPanel[] byCategory;
    private boolean open;
    private float openness;
    private int consumedClicks;
    private String flash;
    private long flashUntil;

    /** Non-null while a drag is in progress, so exactly one widget owns the cursor. */
    public Setting<?> draggingSetting;
    public Module bindingModule;

    public ClickGui(com.wraith.module.ModuleManager modules) {
        byCategory = new CategoryPanel[Category.values().length];
        float x = 6;
        for (Category category : Category.values()) {
            CategoryPanel panel = new CategoryPanel(category, modules.in(category), this);
            panel.x(x);
            panel.y(4);
            panels.add(panel);
            byCategory[category.ordinal()] = panel;
            x += 6;
            panel.collapsed(true);
        }
    }

    public boolean isOpen() { return open; }

    public void setOpen(boolean open) {
        this.open = open;
        if (!open) {
            draggingSetting = null;
            bindingModule = null;
        }
    }

    public void toggle() { setOpen(!open); }

    /** Camera look is suppressed while the menu or the HUD editor owns the cursor. */
    public boolean capturesMouse() {
        if (open) return true;
        com.wraith.modules.misc.HudEditor editor = WraithClient.modules().get(com.wraith.modules.misc.HudEditor.class);
        return editor != null && editor.isEnabled();
    }

    public List<CategoryPanel> panels() { return panels; }

    public CategoryPanel panel(Category category) { return byCategory[category.ordinal()]; }

    public void setTab(String categoryName) {
        if (categoryName == null) return;
        for (CategoryPanel panel : panels) {
            panel.collapsed(!panel.category().label().equalsIgnoreCase(categoryName));
        }
        open = true;
    }

    public void flash(String message) {
        flash = message;
        flashUntil = System.currentTimeMillis() + 1600;
    }

    public Theme theme() {
        ClickGuiModule module = WraithClient.modules().gui();
        return module == null ? new Theme() : module.theme();
    }

    /** True if a widget already ate this frame's primary click. */
    public boolean clickFree() { return consumedClicks == 0 && InputUtil.freshClick(0); }

    public boolean clickFree(int button) {
        return consumedClicks == 0 && (InputUtil.mouseClicked(button)
                || (InputUtil.mouseDown(button) && InputUtil.clickAgeFrames() <= 1));
    }

    public void consumeClick() { consumedClicks++; }

    public void startDrag(Setting<?> setting) {
        draggingSetting = setting;
        consumeClick();
    }

    public void endDrag() {
        if (draggingSetting == null) return;
        draggingSetting = null;
        WraithClient.config().markDirty();
    }

    public void frame(DrawContext context, float tickDelta) {
        ClickGuiModule module = WraithClient.modules().gui();
        if (module != null) {
            int key = module.openKey().intGet();
            if (InputUtil.clicked(key)) toggle();
            if (open && module.closeBehaviour().is("Escape") && InputUtil.clicked(GLFW.GLFW_KEY_ESCAPE)) setOpen(false);
        }
        String profile = module != null ? module.takePendingProfile() : null;
        if (profile != null) WraithClient.config().setProfile(profile);

        consumedClicks = 0;
        // A real Screen (chat, pause, inventory) steals the mouse, so drop the overlay.
        if (open && MCUtil.mc().currentScreen != null) setOpen(false);
        openness = Render2D.animate(openness, open ? 1f : 0f, open ? 16f : 22f);
        if (openness < 0.004f && !open) {
            endDrag();
            return;
        }
        handleBindings();

        int screenW = MCUtil.screenWidth();
        int screenH = MCUtil.screenHeight();
        Theme theme = theme();

        if (module != null && module.dimBackdrop()) {
            int alpha = Math.round(110 * openness);
            Render2D.rect(context, 0, 0, screenW, screenH, com.wraith.util.ColorUtil.argb(alpha, 6, 6, 10));
        }

        float rowX = 6f;
        for (CategoryPanel panel : panels) {
            // Undragged panels ride the auto row; dragged ones keep their position.
            panel.snapToRow(rowX);
            rowX += panel.occupiedWidth(theme) + 4f;
            panel.render(context, theme, tickDelta, openness, screenW, screenH);
        }

        if (!InputUtil.mouseDown(0)) endDrag();
        drawFlash(context, screenW, screenH);
        drawHint(context, screenW, screenH);
    }

    /** Key capture for module binds: the next key pressed (or click) is stored. */
    private void handleBindings() {
        if (bindingModule == null) return;
        if (InputUtil.clicked(GLFW.GLFW_KEY_ESCAPE)) {
            bindingModule = null;
            return;
        }
        if (InputUtil.clicked(GLFW.GLFW_KEY_DELETE)) {
            bindingModule.setBind(GLFW.GLFW_KEY_UNKNOWN);
            WraithClient.config().markDirty();
            bindingModule = null;
            return;
        }
        for (int key = GLFW.GLFW_KEY_UNKNOWN + 1; key <= GLFW.GLFW_KEY_LAST; key++) {
            if (!InputUtil.clicked(key)) continue;
            bindingModule.setBind(key);
            WraithClient.config().markDirty();
            flash(bindingModule.name() + " -> " + InputUtil.keyName(key));
            bindingModule = null;
            return;
        }
        if (InputUtil.mouseClicked(2)) {
            bindingModule.setBind(GLFW.GLFW_KEY_UNKNOWN);
            bindingModule = null;
        }
    }

    public void requestBind(Module module) {
        bindingModule = module;
    }

    public Module binding() { return bindingModule; }

    private void drawFlash(DrawContext context, int screenW, int screenH) {
        if (flash == null || System.currentTimeMillis() > flashUntil) return;
        int tw = Render2D.textWidth(flash);
        float x = screenW / 2f - tw / 2f;
        float y = screenH - 26;
        Render2D.rounded(context, x - 6, y - 4, tw + 12, 14, 4, theme().panel());
        Render2D.text(context, flash, x, y - 1, theme().accent(255), false);
    }

    private void drawHint(DrawContext context, int screenW, int screenH) {
        String text = bindingModule != null
                ? "Press a key for " + bindingModule.name() + "  -  Del clears, Esc cancels"
                : "LMB toggle  -  RMB settings  -  MMB bind  -  drag a header to move";
        int tw = Render2D.textWidth(text);
        Render2D.text(context, text, screenW / 2f - tw / 2f, screenH - 12, theme().textDim(), true);
    }
}
