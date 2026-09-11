package com.wraith.module;

/** GUI grouping order. */
public enum Category {
    RENDER("Render"),
    HUD("HUD"),
    QOL("QoL"),
    MISC("Misc");

    private final String label;

    Category(String label) { this.label = label; }

    public String label() { return label; }
}
