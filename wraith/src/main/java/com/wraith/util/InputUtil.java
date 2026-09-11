package com.wraith.util;

import org.lwjgl.glfw.GLFW;

/**
 * GLFW polled once per rendered frame. Polling (rather than Screen overrides or
 * the keybinding API) is what makes the GUI feel like Vape: it tracks the mouse
 * at render rate instead of client-tick rate, works while no Screen is open, and
 * needs no extra Fabric module.
 */
public final class InputUtil {

    private static final int KEY_SLOTS = GLFW.GLFW_KEY_LAST + 1;
    private static final boolean[] KEY_DOWN = new boolean[KEY_SLOTS];
    private static final boolean[] KEY_CLICKED = new boolean[KEY_SLOTS];
    private static final boolean[] BTN_DOWN = new boolean[8];
    private static final boolean[] BTN_CLICKED = new boolean[8];
    private static final boolean[] BTN_RELEASED = new boolean[8];

    private static double lastMouseX;
    private static double lastMouseY;
    private static double clickedAtX;
    private static double clickedAtY;
    private static int clickAgeFrames = 99;
    private static boolean shiftHeld;

    private static long lastFrameNanos;
    private static float frameDeltaSeconds;

    private InputUtil() {}

    /** Called first thing in the per-frame HUD hook, before anything reads state. */
    public static void update() {
        long now = System.nanoTime();
        if (lastFrameNanos != 0) frameDeltaSeconds = Math.min(0.1f, (now - lastFrameNanos) / 1_000_000_000f);
        lastFrameNanos = now;

        long window = MCUtil.window();
        if (window == 0L) return;

        for (int key = 0; key < KEY_SLOTS; key++) {
            boolean pressed = GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
            KEY_CLICKED[key] = pressed && !KEY_DOWN[key];
            KEY_DOWN[key] = pressed;
        }

        for (int button = 0; button < BTN_DOWN.length; button++) {
            boolean pressed = GLFW.glfwGetMouseButton(window, button) == GLFW.GLFW_PRESS;
            BTN_CLICKED[button] = pressed && !BTN_DOWN[button];
            BTN_RELEASED[button] = !pressed && BTN_DOWN[button];
            BTN_DOWN[button] = pressed;
        }

        lastMouseX = mcMouseX();
        lastMouseY = mcMouseY();
        if (BTN_CLICKED[0]) {
            clickedAtX = lastMouseX;
            clickedAtY = lastMouseY;
            clickAgeFrames = 0;
        } else {
            clickAgeFrames++;
        }
        shiftHeld = isDown(GLFW.GLFW_KEY_LEFT_SHIFT) || isDown(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    /** Seconds since the previous rendered frame, clamped for tab-out safety. */
    public static float frameDelta() { return frameDeltaSeconds == 0 ? 0.0167f : frameDeltaSeconds; }

    public static boolean isDown(int key) {
        return key >= 0 && key < KEY_SLOTS && KEY_DOWN[key];
    }

    /** True for the single frame the key goes down. */
    public static boolean clicked(int key) {
        return key >= 0 && key < KEY_SLOTS && KEY_CLICKED[key];
    }

    public static boolean mouseDown(int button) {
        return button >= 0 && button < BTN_DOWN.length && BTN_DOWN[button];
    }

    public static boolean mouseClicked(int button) {
        return button >= 0 && button < BTN_CLICKED.length && BTN_CLICKED[button];
    }

    public static boolean mouseReleased(int button) {
        return button >= 0 && button < BTN_RELEASED.length && BTN_RELEASED[button];
    }

    public static double mouseX() { return lastMouseX; }
    public static double mouseY() { return lastMouseY; }

    /**
     * True while the cursor is still inside the given box *and* has not travelled
     * more than {@code slop} pixels from where the primary button went down. This
     * is what keeps a HUD drag from also firing the toggle underneath it.
     */
    public static boolean draggedEnough(double slop) {
        return Math.hypot(lastMouseX - clickedAtX, lastMouseY - clickedAtY) > slop;
    }

    public static double clickOriginX() { return clickedAtX; }
    public static double clickOriginY() { return clickedAtY; }
    public static int clickAgeFrames() { return clickAgeFrames; }
    public static boolean shiftHeld() { return shiftHeld; }

    /** A click is only live for a couple of frames, so widgets can latch onto it. */
    public static boolean freshClick(int button) {
        return mouseClicked(button) || (BTN_DOWN[button] && clickAgeFrames <= 1);
    }

    public static boolean inBox(double px, double py, double x, double y, double w, double h) {
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }

    public static String keyName(int key) {
        if (key <= 0) return "None";
        String name = GLFW.glfwGetKeyName(key, 0);
        if (name != null && !name.isBlank()) return name.toUpperCase();
        return switch (key) {
            case GLFW.GLFW_KEY_SPACE -> "Space";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LShift";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RShift";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCtrl";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCtrl";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LAlt";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RAlt";
            case GLFW.GLFW_KEY_ESCAPE -> "Esc";
            case GLFW.GLFW_KEY_TAB -> "Tab";
            case GLFW.GLFW_KEY_ENTER -> "Enter";
            case GLFW.GLFW_KEY_BACKSPACE -> "Backspace";
            default -> key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z
                    ? String.valueOf((char) ('A' + key - GLFW.GLFW_KEY_A))
                    : key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9
                            ? String.valueOf((char) ('0' + key - GLFW.GLFW_KEY_0))
                            : "Key" + key;
        };
    }

    private static double mcMouseX() {
        return MCUtil.mc().mouse == null ? 0 : MCUtil.mc().mouse.getX();
    }

    private static double mcMouseY() {
        return MCUtil.mc().mouse == null ? 0 : MCUtil.mc().mouse.getY();
    }
}
