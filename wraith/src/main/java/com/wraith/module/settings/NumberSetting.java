package com.wraith.module.settings;

import com.wraith.module.Setting;

import java.util.function.Consumer;

public class NumberSetting extends Setting<Double> {

    private final double min;
    private final double max;
    private final double step;
    private final String suffix;

    public NumberSetting(String name, double value, double min, double max, double step) {
        this(name, value, min, max, step, "", null);
    }

    public NumberSetting(String name, double value, double min, double max, double step, String suffix,
                         Consumer<Double> onChange) {
        super(name, clamp(value, min, max, step), onChange);
        this.min = min;
        this.max = max;
        this.step = step;
        this.suffix = suffix;
    }

    public double min() { return min; }
    public double max() { return max; }
    public double step() { return step; }
    public String suffix() { return suffix; }

    public float progress() {
        return max - min <= 0 ? 0f : (float) ((get() - min) / (max - min));
    }

    public void setProgress(float progress) {
        double raw = min + (max - min) * Math.min(1f, Math.max(0f, progress));
        set(clamp(raw, min, max, step));
    }

    public float floatGet() { return get().floatValue(); }
    public int intGet() { return (int) Math.round(get()); }

    @Override public void set(Double value) {
        super.set(clamp(value, min, max, step));
    }

    private static double clamp(double value, double min, double max, double step) {
        double v = Math.min(max, Math.max(min, value));
        if (step > 0) v = Math.round(v / step) * step;
        return Math.min(max, Math.max(min, v));
    }

    @Override public String encode() { return String.valueOf(get()); }

    @Override public void decode(String raw) {
        try {
            set(Double.parseDouble(raw));
        } catch (NumberFormatException ignored) {
            // A corrupt profile entry resets to the default instead of killing the load.
        }
    }
}
