package com.wraith.module;

import com.wraith.WraithClient;
import com.wraith.modules.hud.ArrayListHud;
import com.wraith.modules.hud.ArmorStatus;
import com.wraith.modules.hud.Coordinates;
import com.wraith.modules.hud.Keystrokes;
import com.wraith.modules.hud.StatsHud;
import com.wraith.modules.hud.Watermark;
import com.wraith.modules.misc.ClickGuiModule;
import com.wraith.modules.misc.HudEditor;
import com.wraith.modules.qol.ToggleSprint;
import com.wraith.modules.render.CustomCrosshair;
import com.wraith.modules.render.FullBright;
import com.wraith.modules.render.Freelook;
import com.wraith.modules.render.NoFovShift;
import com.wraith.modules.render.Zoom;
import com.wraith.util.InputUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Owns every module instance: registration, lookup, keybinds, sorting. */
public final class ModuleManager {

    private final Map<Class<? extends Module>, Module> byClass = new LinkedHashMap<>();
    private final List<Module> modules = new ArrayList<>();

    public ModuleManager() {
        WraithClient.bus().register(this);

        // Render / camera
        register(new FullBright());
        register(new Zoom());
        register(new Freelook());
        register(new CustomCrosshair());
        register(new NoFovShift());
        // HUD
        register(new Watermark());
        register(new ArrayListHud());
        register(new StatsHud());
        register(new Coordinates());
        register(new Keystrokes());
        register(new ArmorStatus());
        // QoL
        register(new ToggleSprint());
        // Misc / client chrome
        register(new ClickGuiModule());
        register(new HudEditor());
    }

    public void register(Module module) {
        byClass.put(module.getClass(), module);
        modules.add(module);
    }

    public List<Module> all() { return List.copyOf(modules); }

    public List<Module> in(Category category) {
        return modules.stream().filter(m -> m.category() == category).toList();
    }

    /** Enabled modules, longest-enabled first - matches the ordering a HUD ramp expects. */
    public List<Module> enabled() {
        return modules.stream()
                .filter(Module::isEnabled)
                .filter(m -> !m.isHidden())
                .sorted(Comparator.comparingInt(m -> -m.name().length()))
                .toList();
    }

    @SuppressWarnings("unchecked")
    public <M extends Module> M get(Class<M> type) { return (M) byClass.get(type); }

    public Module byName(String name) {
        for (Module module : modules) {
            if (module.name().equalsIgnoreCase(name)) return module;
        }
        return null;
    }

    /**
     * Binds are polled per rendered frame, not per client tick: the edge flags in
     * InputUtil are only refreshed once per frame, so reading them 20 times a second
     * would see the same press several times and toggle a module two or three times.
     */
    public void pollBinds() {
        for (Module module : modules) {
            int bind = module.bind();
            if (bind > 0 && InputUtil.clicked(bind) && !module.isHidden()) module.toggle();
        }
    }

    public ClickGuiModule gui() { return get(ClickGuiModule.class); }
}
