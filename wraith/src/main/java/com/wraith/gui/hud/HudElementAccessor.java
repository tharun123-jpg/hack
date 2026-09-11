package com.wraith.gui.hud;

/** Implemented by {@link HudModule}; keeps HudManager free of field access tricks. */
public interface HudElementAccessor {
    Anchor hudAnchor();

    float hudOffsetX();

    float hudOffsetY();

    void hudSetAnchor(Anchor anchor);

    void hudSetOffsetX(float x);

    void hudSetOffsetY(float y);
}
