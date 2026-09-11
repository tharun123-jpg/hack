package com.wraith.module;

import com.wraith.WraithClient;
import com.wraith.event.Event;
import com.wraith.event.EventTarget;
import com.wraith.event.events.RenderHudEvent;
import com.wraith.event.events.TickEvent;
import com.wraith.event.events.WorldEvent;
import com.wraith.module.settings.BooleanSetting;
import com.wraith.module.settings.ColorSetting;
import com.wraith.module.settings.ModeSetting;
import com.wraith.module.settings.NumberSetting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for everything the user can toggle.
 *
 * The base auto-registers itself on the bus while enabled, so subclasses only
 * override the hooks they care about instead of wiring listeners by hand.
 */
public abstract class Module {

    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();

    private boolean enabled;
    private boolean extended;
    private int bind = GLFW.GLFW_KEY_UNKNOWN;
    private long enabledSince;
    private boolean hidden;

    /** Draw counter for HUD elements; also used for fade-in animations. */
    public float fade;

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public String name() { return name; }
    public String displayName() { return name; }
    public String description() { return description; }
    public Category category() { return category; }
    public List<Setting<?>> settings() { return settings; }
    public boolean isEnabled() { return enabled; }
    public boolean isExtended() { return extended; }
    public int bind() { return bind; }
    public void setBind(int key) { this.bind = key; }
    public void setExtended(boolean extended) { this.extended = extended; }
    /** Client chrome (the menu itself) has no business appearing in the HUD list. */
    public boolean isHidden() { return hidden; }
    protected void setHidden(boolean hidden) { this.hidden = hidden; }
    public long enabledTicks() { return enabledSince == 0 ? 0 : System.currentTimeMillis() - enabledSince; }

    protected <S extends Setting<?>> S add(S setting) {
        settings.add(setting);
        return setting;
    }

    protected BooleanSetting bool(String name, boolean value) { return add(new BooleanSetting(name, value)); }
    protected NumberSetting num(String name, double v, double min, double max, double step) {
        return add(new NumberSetting(name, v, min, max, step));
    }
    protected NumberSetting num(String name, double v, double min, double max, double step, String suffix) {
        return add(new NumberSetting(name, v, min, max, step, suffix, null));
    }
    protected ModeSetting mode(String name, String initial, String... modes) {
        return add(new ModeSetting(name, initial, modes));
    }
    protected ColorSetting color(String name, int rgb) { return add(new ColorSetting(name, rgb)); }

    @SuppressWarnings("unchecked")
    protected <S extends Setting<?>> S get(String name) {
        for (Setting<?> setting : settings) {
            if (setting.name().equalsIgnoreCase(name)) return (S) setting;
        }
        throw new IllegalArgumentException("No setting '" + name + "' on " + this.name);
    }

    public final void toggle() { setEnabled(!enabled); }

    /** Called by the manager and by config load; keeps the bus in sync. */
    public final void setEnabled(boolean target) {
        if (enabled == target) return;
        enabled = target;
        if (target) {
            enabledSince = System.currentTimeMillis();
            WraithClient.bus().register(this);
            onEnable();
        } else {
            WraithClient.bus().unregister(this);
            enabledSince = 0;
            onDisable();
        }
        if (WraithClient.config() != null) WraithClient.config().markDirty();
        onToggleChanged();
    }

    /** Force a re-dispatch after a reflective/late registration (config load). */
    public final void reloadHooks() {
        if (!enabled) return;
        WraithClient.bus().unregister(this);
        WraithClient.bus().register(this);
    }

    /** Hook: fired when the module is switched on. */
    protected void onEnable() {}

    /** Hook: fired when the module is switched off; restore anything onEnable touched. */
    protected void onDisable() {}

    /** Hook: fired every client tick while enabled. */
    protected void onTick() {}

    /** Hook: fired every frame while enabled, after the vanilla HUD. */
    protected void onRenderHud(RenderHudEvent event) {}

    /** Hook: world join/leave. */
    protected void onWorld(WorldEvent event) {}

    /** Hook: after enable/disable, for the GUI (sound, animation, toasts). */
    protected void onToggleChanged() {}

    /** Right-hand label in the ArrayList HUD, e.g. "2.4x" or "Smooth". */
    public String suffix() { return ""; }

    @EventTarget
    private void handleTick(TickEvent event) { onTick(); }

    @EventTarget
    private void handleRenderHud(RenderHudEvent event) { onRenderHud(event); }

    @EventTarget
    private void handleWorld(WorldEvent event) { onWorld(event); }

    /** Lets subclasses post events (e.g. a HUD asking the GUI to re-layout). */
    protected final <E extends Event> E post(E event) { return WraithClient.bus().post(event); }
}
