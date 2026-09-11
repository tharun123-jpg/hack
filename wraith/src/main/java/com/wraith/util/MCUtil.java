package com.wraith.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;

/** Static accessors shared by every module, GUI widget and util class. */
public final class MCUtil {

    private MCUtil() {}

    public static MinecraftClient mc() { return MinecraftClient.getInstance(); }

    public static long window() {
        Window window = mc().getWindow();
        return window == null ? 0L : window.getHandle();
    }

    public static int screenWidth() { return scaled(mc().getWindow().getFramebufferWidth()); }
    public static int screenHeight() { return scaled(mc().getWindow().getFramebufferHeight()); }

    /** framebuffer pixels -> GUI pixels (the value vanilla calls `scaleFactor`). */
    public static int scaled(int framebufferValue) {
        Window window = mc().getWindow();
        double sf = window.getScaleFactor();
        return sf <= 0 ? framebufferValue : (int) Math.round(framebufferValue / sf);
    }

    public static boolean inGame() {
        MinecraftClient mc = mc();
        return mc != null && mc.player != null && mc.world != null;
    }

    public static boolean noScreen() { return mc().currentScreen == null; }

    public static void chat(String message) {
        if (mc().player == null) return;
        mc().player.sendMessage(net.minecraft.text.Text.literal(message), false);
    }
}
