package com.wraith.gui.hud;

/** Nine-point anchoring so HUD stays put on resize / GUI-scale change. */
public enum Anchor {
    TOP_LEFT(0, 0), TOP_CENTER(0, 1), TOP_RIGHT(0, 2),
    MIDDLE_LEFT(1, 0), MIDDLE(1, 1), MIDDLE_RIGHT(1, 2),
    BOTTOM_LEFT(2, 0), BOTTOM_CENTER(2, 1), BOTTOM_RIGHT(2, 2);

    final int row;
    final int col;

    Anchor(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public static Anchor fromOrdinal(int ordinal) {
        Anchor[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : TOP_LEFT;
    }
}
