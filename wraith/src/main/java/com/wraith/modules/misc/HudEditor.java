package com.wraith.modules.misc;

import com.wraith.WraithClient;
import com.wraith.event.events.RenderHudEvent;
import com.wraith.module.Category;
import com.wraith.module.Module;
import com.wraith.module.settings.BooleanSetting;
import com.wraith.module.settings.ModeSetting;
import com.wraith.util.MCUtil;
import net.minecraft.client.gui.DrawContext;
import com.wraith.gui.Render2D;
import com.wraith.util.ColorUtil;

/**
 * The drag-and-drop HUD layout mode (Vape calls this the HUD editor).
 * Toggled from the GUI; while on, every HUD element gets an outline and can be
 * moved with the left button or switched with the right button.
 */
public class HudEditor extends Module {

    private final BooleanSetting showHidden = bool("Show Disabled", true);
    private final ModeSetting hint = mode("Hint", "Name + Anchor", "Name", "Off");
    private final BooleanSetting ownsClicks = bool("Capture Clicks", false);

    public HudEditor() {
        super("HudEditor", "Drag HUD elements, snap to edges, right-click to toggle", Category.MISC);
    }

    public boolean showHidden() { return showHidden.on(); }

    /** While true the menu panels are hidden and every click goes to the HUD. */
    public boolean ownsClicks() { return ownsClicks.on(); }
    public String hintMode() { return hint.get(); }

    @Override
    protected void onEnable() {
        WraithClient.gui().setOpen(true);
        WraithClient.gui().setTab("HUD");
    }

    @Override
    protected void onDisable() {
        if (WraithClient.gui() != null) WraithClient.gui().setTab(null);
    }

    @Override
    protected void onRenderHud(RenderHudEvent event) {
        DrawContext context = event.context();
        if (hint.is("Off")) return;
        int width = MCUtil.screenWidth();
        String label = showHidden.on()
                ? "HUD EDITOR - drag to move, right-click to toggle"
                : "HUD EDITOR - drag to move";
        int tw = Render2D.textWidth(label);
        float x = width / 2f - tw / 2f;
        float y = MCUtil.screenHeight() - 12;
        Render2D.rounded(context, x - 5, y - 3, tw + 10, 13, 3,
                ColorUtil.argb(190, 12, 12, 16));
        Render2D.text(context, label, x, y, WraithClient.modules().gui().theme().accent(255), false);
    }
}
