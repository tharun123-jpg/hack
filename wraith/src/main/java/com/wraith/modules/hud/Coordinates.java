package com.wraith.modules.hud;

import com.wraith.gui.Render2D;
import com.wraith.gui.hud.Anchor;
import com.wraith.gui.hud.HudModule;
import com.wraith.module.settings.BooleanSetting;
import com.wraith.module.settings.ModeSetting;
import com.wraith.util.ColorUtil;
import com.wraith.util.MCUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;

/** Where you are, which way you face, and what is under your feet. */
public class Coordinates extends HudModule {

    private final ModeSetting precision = mode("Precision", "Blocks", "Blocks", "Exact");
    private final BooleanSetting facing = bool("Facing", true);
    private final BooleanSetting underfoot = bool("Underfoot", true);
    private final BooleanSetting netherCoord = bool("Nether Equivalent", false);

    public Coordinates() {
        super("Coordinates", "Position, heading and the block below you", Anchor.BOTTOM_LEFT);
    }

    @Override
    protected float draw(DrawContext context, float x, float y, float tickDelta) {
        if (MCUtil.mc().player == null || MCUtil.mc().world == null) return 0;
        var player = MCUtil.mc().player;
        String xyz = precision.is("Exact")
                ? String.format(java.util.Locale.ROOT, "%.1f %.1f %.1f",
                        player.getX(), player.getY(), player.getZ())
                : String.format(java.util.Locale.ROOT, "%d %d %d",
                        player.getBlockX(), player.getBlockY(), player.getBlockZ());

        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add(xyz);
        if (facing.on()) lines.add(facing(player.getYaw()));
        if (netherCoord.on() && isNether()) {
            lines.add("nether " + player.getBlockX() / 8 + " " + player.getBlockY() + " " + player.getBlockZ() / 8);
        }
        if (underfoot.on()) lines.add(blockBelow());

        float widest = 0;
        for (String line : lines) widest = Math.max(widest, Render2D.textWidth(line));
        float boxW = widest + 8;
        float boxH = lines.size() * 10f + 4;
        Render2D.rounded(context, x, y, boxW, boxH, 3, theme().background());

        float cursor = y + 3;
        for (int i = 0; i < lines.size(); i++) {
            Render2D.text(context, lines.get(i), x + 4, cursor,
                    i == 0 ? theme().accent(255) : ColorUtil.withAlpha(theme().textDim(), 230), false);
            cursor += 10;
        }
        size(boxW, boxH);
        return boxW;
    }

    /** RegistryKey#getValue is stable, unlike the dimension-effects accessors. */
    private boolean isNether() {
        var key = MCUtil.mc().player.getWorld().getRegistryKey().getValue();
        return "the_nether".equals(key.getPath());
    }

    private String blockBelow() {
        BlockPos below = MCUtil.mc().player.getBlockPos().down();
        var state = MCUtil.mc().world.getBlockState(below);
        return state.isAir() ? "air" : state.getBlock().getName().getString();
    }

    private static String facing(float yaw) {
        int index = (int) Math.floor(((yaw % 360 + 360) % 360 + 22.5) / 45) % 8;
        return new String[]{"South", "South West", "West", "North West", "North", "North East", "East", "South East"}[index];
    }
}
