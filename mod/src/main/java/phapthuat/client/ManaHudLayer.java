package phapthuat.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import phapthuat.mana.ClientManaCache;
import phapthuat.spell.Fmt;

/** Thanh mana xanh dương phía trên thanh máu, kèm chữ "Mana 966,7 / 1000". */
public final class ManaHudLayer implements LayeredDraw.Layer {
    private static final int BAR_WIDTH = 81;
    private static final int BAR_HEIGHT = 3;

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.player.isSpectator()) return;
        double mana = ClientManaCache.mana();
        double max = ClientManaCache.max();
        if (mana < 0 || max <= 0) return;

        int x = g.guiWidth() / 2 - 91;
        int y = g.guiHeight() - 62;
        int filled = (int) Math.round(BAR_WIDTH * Math.min(1, mana / max));
        g.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFF1B1F3A);
        g.fill(x, y, x + filled, y + BAR_HEIGHT, 0xFF3D8BFF);
        String text = "Mana " + Fmt.mana(mana) + " / " + Fmt.num(max, 0);
        g.drawString(mc.font, text, x + BAR_WIDTH + 4, y - 3, 0xFF9CC7FF, true);
    }
}
