package com.wraith.module;

import java.util.function.Consumer;

/**
 * A named, typed value owned by a {@link Module}. Values round-trip through the
 * config via their String form so the file format stays trivially diffable.
 */
public abstract class Setting<T> {

    private final String name;
    private final Consumer<T> onChange;
    protected T value;
    /** 0..1 animation for GUI widgets; owned by the renderer, not the model. */
    public float animation;

    protected Setting(String name, T initial, Consumer<T> onChange) {
        this.name = name;
        this.value = initial;
        this.onChange = onChange;
    }

    public String name() { return name; }
    public T get() { return value; }

    public void set(T value) {
        if (value == null) return;
        boolean changed = !value.equals(this.value);
        this.value = value;
        if (changed) applyAndNotify();
    }

    protected void applyAndNotify() {
        if (onChange != null) onChange.accept(value);
    }

    public abstract String encode();

    public abstract void decode(String raw);
}
