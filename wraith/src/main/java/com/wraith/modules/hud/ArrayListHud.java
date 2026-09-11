package com.wraith.modules.hud;

import com.wraith.WraithClient;
import com.wraith.event.events.RenderHudEvent;
import com.wraith.gui.Render2D;
import com.wraith.gui.hud.Anchor;
import com.wraith.gui.hud.HudModule;
import com.wraith.module.Module;
import com.wraith.module.settings.BooleanSetting;
import com.wraith.module.settings.ModeSetting;
import com.wraith.module.settings.NumberSetting;
import com.wraith.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The list of currently active modules, width-sorted so the block on the left edge
 * stays rectangular - and animated per row instead of popping in and out.
 */
public class ArrayListHud extends HudModule {

    private final ModeSetting sort = mode("Sort", "Width", "Width", "Name");
    private final BooleanSetting showSuffix = bool("Suffix", true);
    private final BooleanSetting showBinds = bool("Binds", false);
    private final ModeSetting colourMode = mode("Colour", "Fade", "Accent", "Fade", "Rainbow");
    private final NumberSetting animation = num("Slide", 1.0, 0.0, 2.0, 0.1, "x");
    private final BooleanSetting background = bool("Backdrop", true);

    private final Map<String, Float> reveals = new HashMap<>();
    private List<String> order = new ArrayList<>();

    public ArrayListHud() {
        super("ArrayList", "Lists every module that is switched on", Anchor.TOP_RIGHT);
    }

    @Override
    protected float draw(DrawContext context, float x, float y, float tickDelta) {
        List<Module> active = new ArrayList<>(WraithClient.modules().enabled());
        if (active.isEmpty()) {
            reveals.clear();
            order = List.of();
            size(0, 0);
            return 0;
        }
        if (sort.is("Width")) {
            active.sort(Comparator.comparingInt(m -> -Render2D.textWidth(label(m))));
        } else {
            active.sort(Comparator.comparing(Module::name, String.CASE_INSENSITIVE_ORDER));
        }

        float widest = 0;
        for (Module module : active) widest = Math.max(widest, Render2D.textWidth(label(module)));
        widest += 8;

        float rowH = 11f;
        // Anchor-aware growth: bottom-aligned lists should not grow downwards off screen.
        boolean growUp = anchor().row == 2;
        float orderCursor = 0;
        List<String> nextOrder = new ArrayList<>();
        for (int i = 0; i < active.size(); i++) {
            Module module = active.get(i);
            String key = module.name();
            nextOrder.add(key);
            float target = 1f;
            float current = reveals.getOrDefault(key, 0f);
            current = Render2D.animate(current, target, 14f / Math.max(0.05f, animation.floatGet()));
            reveals.put(key, current);

            float rowY = y + (growUp ? (active.size() - 1 - i) * (rowH + 1) : i * (rowH + 1));
            float slide = (1f - current) * -(widest + 6);
            float rowX = x + (anchor().col == 2 ? widest - Render2D.textWidth(label(module)) : 0) + slide;

            int color = switch (colourMode.get()) {
                case "Rainbow" -> ColorUtil.rainbow(0.3f, i, 255);
                case "Accent" -> theme().accent(255);
                default -> ColorUtil.blend(theme().accent(255), theme().accentDeep(255), i / (float) active.size());
            };
            if (background.on()) {
                Render2D.rounded(context, rowX - 4, rowY, widest + 8, rowH, 2,
                        ColorUtil.withAlpha(theme().background(), (int) (210 * current)));
            }
            Render2D.text(context, label(module), rowX, rowY + 1.5f,
                    ColorUtil.blend(theme().textOff(), color, current), false);
        }
        // Fade out entries whose module went off, then drop them.
        reveals.keySet().retainAll(nextOrder);
        order = nextOrder;
        size(widest, active.size() * (rowH + 1));
        return widest;
    }

    private String label(Module module) {
        String name = module.name();
        if (showSuffix.on() && !module.suffix().isEmpty()) name = name + " " + module.suffix();
        if (showBinds.on() && module.bind() > 0) name = name + " [" + com.wraith.util.InputUtil.keyName(module.bind()) + "]";
        return name;
    }
}
