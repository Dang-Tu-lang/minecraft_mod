package phapthuat.spell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import phapthuat.item.WandItem;
import phapthuat.registry.ModDataComponents;

/**
 * Kiểm tra phép gửi lên từ sách trước khi ghi vào Pháp Trượng: dữ liệu từ client không bao giờ được tin, mọi thông
 * số đều được kẹp về [min, max] hoặc bị từ chối.
 */
public final class InscribeValidator {
    private InscribeValidator() {}

    /**
     * Phép phải tồn tại và cho phép thao tác này. Mỗi thông số của phép và của thao tác: thiếu thì lấy mặc định,
     * NaN/vô cực thì từ chối, có giá trị thì kẹp. Khóa lạ bị bỏ.
     */
    public static Optional<InscribedSpell> validate(InscribedSpell raw) {
        Optional<Spell> spell = SpellRegistry.get(raw.spellId());
        if (spell.isEmpty() || !spell.get().shapes().contains(raw.shape())) return Optional.empty();
        Map<String, Double> clean = new HashMap<>();
        for (SpellParam p : spell.get().allParams(raw.shape())) {
            Double v = raw.params().get(p.key());
            if (v == null) {
                clean.put(p.key(), p.defaultValue());
            } else if (!Double.isFinite(v)) {
                return Optional.empty();
            } else {
                clean.put(p.key(), p.clamp(v));
            }
        }
        return Optional.of(new InscribedSpell(raw.spellId(), raw.shape(), clean));
    }

    /** Ghi phép vào ô {@code slot} (0..4) của Pháp Trượng ở tay phụ. Trả false nếu bị từ chối. */
    public static boolean inscribe(ServerPlayer player, int slot, InscribedSpell raw) {
        ItemStack wand = player.getOffhandItem();
        if (!(wand.getItem() instanceof WandItem) || slot < 0 || slot >= WandSlots.SIZE) return false;
        Optional<InscribedSpell> valid = validate(raw);
        if (valid.isEmpty()) return false;
        wand.set(ModDataComponents.WAND_SLOTS.get(), WandItem.slots(wand).with(slot, valid.get()));
        Component name = SpellRegistry.get(raw.spellId()).orElseThrow().displayName();
        player.displayClientMessage(Component.translatable("phapthuat.inscribe.done", name, slot + 1), true);
        return true;
    }
}
