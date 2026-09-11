package com.wraith;

import com.wraith.config.ConfigManager;
import com.wraith.event.EventBus;
import com.wraith.gui.ClickGui;
import com.wraith.gui.hud.HudManager;
import com.wraith.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bootstrap order matters and is deliberate:
 * bus -> config (so loading can touch paths) -> hud -> modules -> gui, then the
 * current profile is applied last, once everything that can be toggled exists.
 */
public final class WraithClient implements ClientModInitializer {

    public static final String NAME = "Wraith";
    public static final String VERSION = "0.1.0";

    private static final Logger LOG = LoggerFactory.getLogger(NAME);
    private static final EventBus BUS = new EventBus();

    private static ConfigManager config;
    private static ModuleManager modules;
    private static HudManager hud;
    private static ClickGui gui;

    @Override
    public void onInitializeClient() {
        LOG.info("Initialising {} {} against Minecraft {}", NAME, VERSION,
                MinecraftClient.getInstance().getGameVersion());

        config = new ConfigManager();
        config.readDefaults();
        hud = new HudManager();
        modules = new ModuleManager();
        gui = new ClickGui(modules);
        config.load(config.currentProfile());

        LOG.info("{} ready: {} modules, {} hud elements", NAME,
                modules.all().size(), hud.elements().size());
    }

    /** Per-frame entry point, called from MixinInGameHud after vanilla HUD draws. */
    public static void onHudFrame(DrawContext context, float tickDelta) {
        if (context == null || MinecraftClient.getInstance().player == null) return;
        com.wraith.util.InputUtil.update();
        modules().pollBinds();
        float delta = tickDelta;
        BUS.post(new com.wraith.event.events.RenderHudEvent(
                context, delta,
                MinecraftClient.getInstance().getWindow().getScaledFramebufferWidth(),
                MinecraftClient.getInstance().getWindow().getScaledFramebufferHeight()));
        // Order matters: elements first, then editor chrome on top of them, then
        // the menu on top of everything so it always wins input.
        hud.frame(context, delta);
        gui.frame(context, delta);
    }

    public static EventBus bus() { return BUS; }
    public static ModuleManager modules() { return modules; }
    public static HudManager hud() { return hud; }
    public static ConfigManager config() { return config; }
    public static ClickGui gui() { return gui; }
    public static Logger log() { return LOG; }
}
