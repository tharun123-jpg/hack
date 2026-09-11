package com.wraith.module.settings;

import com.wraith.module.Setting;

import java.util.function.Consumer;

public class BooleanSetting extends Setting<Boolean> {

    public BooleanSetting(String name, boolean value) { this(name, value, null); }

    public BooleanSetting(String name, boolean value, Consumer<Boolean> onChange) {
        super(name, value, onChange);
    }

    public boolean on() { return get(); }

    public void toggle() { set(!get()); }

    @Override public String encode() { return String.valueOf(get()); }

    @Override public void decode(String raw) { set(Boolean.parseBoolean(raw)); }
}
