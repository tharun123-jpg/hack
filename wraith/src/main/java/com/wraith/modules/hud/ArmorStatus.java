package com.wraith.modules.hud;

import com.wraith.gui.Render2D;
import com.wraith.gui.hud.Anchor;
import com.wraith.gui.hud.HudModule;
import com.wraith.module.settings.BooleanSetting;
import com.wraith.module.settings.NumberSetting;
import com.wraith.util.ColorUtil;
import com.wraith.util.MCUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

/**
 * Durability of worn armour. The point is the warning colour at a threshold, not
 * the numbers - elytra and netherite quietly break at the worst possible moment.
 */
public class ArmorStatus extends HudModule {

    private final BooleanSetting percentages = bool("Percent", true);
    private final NumberSetting warnAt = num("Warn Under", 20.0, 1.0, 99.0, 1, "%");
    private final BooleanSetting names = bool("Item Names", false);
    private final BooleanSetting onlyWhenWorn = bool("Hide When Empty", true);

    private static final String[] SLOTS = {"Helmet", "Chest", "Legs", "Feet"};

    public ArmorStatus() {
        super("ArmorStatus", "Durability of what you are wearing", Anchor.BOTTOM_CENTER);
    }

    @Override
    protected float draw(DrawContext context, float x, float y, float tickDelta) {
        if (MCUtil.mc().player == null) return 0;
        var armor = MCUtil.mc().player.getInventory().armor;

        float rowW = names.on() ? 96 : 62;
        float rowH = 10;
        int rows = 0;
        for (int i = 0; i < 4; i++) {
            ItemStack stack = i < armor.size() ? armor.get(i) : ItemStack.EMPTY;
            if (stack.isEmpty() && onlyWhenWorn.on()) continue;
            float rowY = y + rows * (rowH + 1);
            float rowX = x - rowW / 2f;
            Render2D.rounded(context, rowX, rowY, rowW, rowH, 2, theme().background());

            String label = SLOTS[i];
            int percent = percent(stack);
            boolean broken = stack.getMaxDamage() > 0 && percent <= 0;
            boolean warn = !broken && percent < warnAt.intGet();
            int color = broken ? ColorUtil.argb(255, 255, 80, 80)
                    : warn ? ColorUtil.argb(255, 255, 196, 90)
                    : theme().accent(255);

            int textX = (int) (rowX + 3);
            Render2D.text(context, label, textX, rowY + 1, ColorUtil.withAlpha(theme().textDim(), 235), false);
            if (names.on() && !stack.isEmpty()) {
                String shortName = shortName(stack);
                Render2D.text(context, shortName, textX + 26, rowY + 1, ColorUtil.withAlpha(theme().text(), 200), false);
            }
            String value = stack.isEmpty() ? "-" : broken ? "BROKEN" : percent + "%";
            Render2D.text(context, value, rowX + rowW - 3 - Render2D.textWidth(value), rowY + 1, color, false);

            if (percentages.on() && stack.getMaxDamage() > 0) {
                float barW = rowW - 6;
                Render2D.rect(context, rowX + 3, rowY + rowH - 2, barW, 1, ColorUtil.argb(90, 255, 255, 255));
                Render2D.rect(context, rowX + 3, rowY + rowH - 2, barW * (percent / 100f), 1, color);
            }
            rows++;
        }
        if (rows == 0) return 0;
        size(rowW, rows * (rowH + 1));
        return rowW;
    }

    private static int percent(ItemStack stack) {
        if (stack.isEmpty() || stack.getMaxDamage() <= 0) return 100;
        return (int) Math.round(100d * (stack.getMaxDamage() - stack.getDamage()) / stack.getMaxDamage());
    }

    private static String shortName(ItemStack stack) {
        String name = stack.getName().getString();
        return name.length() > 11 ? name.substring(0, 10) + "..." : name;
    }
}
