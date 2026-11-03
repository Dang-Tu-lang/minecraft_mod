package phapthuat.spell;

import java.util.Map;

/** Một phép đã khắc: phép nào, thao tác nào, và mọi thông số người chơi đã nhập. Java thuần; codec ở {@link SpellCodecs}. */
public record InscribedSpell(String spellId, Shape shape, Map<String, Double> params) {
    public InscribedSpell {
        params = Map.copyOf(params);
    }

    /** Giá trị đã nhập, NaN nếu thiếu. */
    public double param(String key) {
        Double v = params.get(key);
        return v == null ? Double.NaN : v;
    }

    /** Giá trị đã nhập, hoặc mặc định của thông số nếu thiếu; luôn nằm trong [min, max]. */
    public double param(SpellParam p) {
        Double v = params.get(p.key());
        return p.clamp(v == null || !Double.isFinite(v) ? p.defaultValue() : v);
    }
}
