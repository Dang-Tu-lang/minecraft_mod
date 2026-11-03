package phapthuat.spell;

import java.util.List;

/**
 * Một thông số người chơi nhập cho phép hoặc thao tác. Java thuần, không phụ thuộc Minecraft.
 *
 * @param choiceLangKeys rỗng là thông số số; khác rỗng là thông số lựa chọn, giá trị lưu là chỉ số (kiểu double)
 */
public record SpellParam(String key, String unitLangKey, double min, double max, double defaultValue,
        List<String> choiceLangKeys) {

    public SpellParam {
        choiceLangKeys = List.copyOf(choiceLangKeys);
    }

    public SpellParam(String key, String unitLangKey, double min, double max, double defaultValue) {
        this(key, unitLangKey, min, max, defaultValue, List.of());
    }

    /** Thông số lựa chọn (ví dụ Đẩy/Kéo): giá trị là chỉ số trong {@code choiceLangKeys}. */
    public static SpellParam choice(String key, List<String> choiceLangKeys, int defaultIndex) {
        return new SpellParam(key, "", 0, choiceLangKeys.size() - 1, defaultIndex, choiceLangKeys);
    }

    public boolean isChoice() {
        return !choiceLangKeys.isEmpty();
    }

    /** Kẹp về [min, max]; thông số lựa chọn còn được làm tròn về chỉ số nguyên. */
    public double clamp(double v) {
        double c = Math.max(min, Math.min(max, v));
        return isChoice() ? Math.round(c) : c;
    }

    public String langKey() {
        return "phapthuat.param." + key;
    }
}
