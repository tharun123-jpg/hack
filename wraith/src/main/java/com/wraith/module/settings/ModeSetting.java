package com.wraith.module.settings;

import com.wraith.module.Setting;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class ModeSetting extends Setting<String> {

    private final List<String> modes;

    public ModeSetting(String name, String initial, String... modes) {
        this(name, initial, Arrays.asList(modes), null);
    }

    public ModeSetting(String name, String initial, List<String> modes, Consumer<String> onChange) {
        super(name, modes.contains(initial) ? initial : modes.get(0), onChange);
        this.modes = List.copyOf(modes);
    }

    public List<String> modes() { return modes; }

    public boolean is(String mode) { return get().equalsIgnoreCase(mode); }

    public int index() {
        for (int i = 0; i < modes.size(); i++) {
            if (modes.get(i).equalsIgnoreCase(get())) return i;
        }
        return 0;
    }

    /** Clicking the widget advances the cycle; shift-click walks backwards. */
    public void cycle(int delta) {
        set(modes.get(Math.floorMod(index() + delta, modes.size())));
    }

    @Override public String encode() { return get(); }

    @Override public void decode(String raw) {
        for (String mode : modes) {
            if (mode.equalsIgnoreCase(raw)) { set(mode); return; }
        }
    }
}
