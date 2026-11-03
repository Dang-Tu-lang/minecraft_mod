package phapthuat.spell;

import java.util.List;
import java.util.Set;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import phapthuat.physics.Element;

/** Một phép. Tự khai báo thông số và thao tác để sách tự vẽ ô nhập và Pháp Trượng tự hiển thị. */
public interface Spell {
    String id();

    Element element();

    Set<Shape> shapes();

    List<SpellParam> params();

    /** Mana trừ trước khi áp dụng phép. */
    double upfrontCostMana(InscribedSpell spell);

    /** Mana trừ trước khi chi phí phụ thuộc các đơn vị đã chọn (Nâng tính theo khối lượng). Mặc định không phụ thuộc. */
    default double upfrontCostMana(InscribedSpell spell, ServerLevel level, List<SpellUnit> units) {
        return upfrontCostMana(spell);
    }

    /** Thông số của thao tác khi dùng với phép này; phép có thể đổi khoảng (Từ Trường: bán kính Cầu 1–6). */
    default List<SpellParam> shapeParams(Shape shape) {
        return shape.params();
    }

    /** Mọi thông số của một lần khắc: của phép rồi của thao tác. */
    default List<SpellParam> allParams(Shape shape) {
        List<SpellParam> all = new java.util.ArrayList<>(params());
        all.addAll(shapeParams(shape));
        return all;
    }

    /** Đơn vị này có chịu tác động (và được tính vào luật chia năng lượng) với thao tác này không. */
    boolean affects(ServerLevel level, SpellUnit unit, Shape shape);

    /** Áp phép lên các đơn vị: tự chia năng lượng, tự cộng/trừ thêm mana nếu có, tự xử lý phản tác dụng. */
    CastReport apply(CastContext ctx, List<SpellUnit> units);

    default String langKey() {
        return "phapthuat.spell." + id() + ".name";
    }

    default Component displayName() {
        return Component.translatable(langKey());
    }
}
