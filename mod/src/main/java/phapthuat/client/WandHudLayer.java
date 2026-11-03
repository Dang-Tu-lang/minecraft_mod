package phapthuat.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import phapthuat.item.WandItem;
import phapthuat.spell.Fmt;
import phapthuat.spell.InscribedSpell;
import phapthuat.spell.Spell;
import phapthuat.spell.SpellParam;
import phapthuat.spell.SpellRegistry;
import phapthuat.spell.WandSlots;

/** Khi cầm Pháp Trượng: dòng phía trên thanh vật phẩm, ví dụ "[3/5] Gia Nhiệt · Điểm · năng lượng 33,3 mana". */
public final class WandHudLayer implements LayeredDraw.Layer {
    /** Cao hơn dòng chữ của thanh mana để không đè lên nhau. */
    private static final int OFFSET_FROM_BOTTOM = 76;

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.player.isSpectator()) return;
        ItemStack stack = mc.player.getMainHandItem();
        if (!(stack.getItem() instanceof WandItem)) return;
        g.drawCenteredString(mc.font, line(WandItem.slots(stack)), g.guiWidth() / 2,
                g.guiHeight() - OFFSET_FROM_BOTTOM, 0xFFE8D6FF);
    }

    private static Component line(WandSlots slots) {
        int index = slots.selected() + 1;
        Optional<InscribedSpell> current = slots.current();
        if (current.isEmpty()) return Component.translatable("phapthuat.wand.hud_empty", index, WandSlots.SIZE);
        InscribedSpell inscribed = current.get();
        Optional<Spell> spell = SpellRegistry.get(inscribed.spellId());
        Component name = spell.map(Spell::displayName).orElse(Component.literal(inscribed.spellId()));
        Component shape = Component.translatable(inscribed.shape().langKey());

        List<SpellParam> params = spell.map(s -> s.allParams(inscribed.shape())).orElse(List.of());
        if (params.isEmpty()) return Component.translatable("phapthuat.wand.hud_short", index, WandSlots.SIZE, name, shape);
        List<String> parts = new ArrayList<>();
        for (SpellParam p : params) {
            if (p.isChoice()) {
                // Thông số lựa chọn hiện tên lựa chọn (ví dụ "Kéo") thay vì chỉ số.
                parts.add(I18n.get(p.choiceLangKeys().get((int) inscribed.param(p))));
                continue;
            }
            parts.add(I18n.get("phapthuat.param." + p.key()).toLowerCase(Locale.ROOT) + " "
                    + Fmt.num(inscribed.param(p), 1) + " " + I18n.get(p.unitLangKey()));
        }
        return Component.translatable("phapthuat.wand.hud", index, WandSlots.SIZE, name, shape,
                String.join(" · ", parts));
    }
}
