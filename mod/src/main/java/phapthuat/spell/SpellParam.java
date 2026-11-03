package phapthuat.spell;

/** Một thông số người chơi nhập cho phép hoặc thao tác. Java thuần, không phụ thuộc Minecraft. */
public record SpellParam(String key, String unitLangKey, double min, double max, double defaultValue) {
    public double clamp(double v) {
        return Math.max(min, Math.min(max, v));
    }

    public String langKey() {
        return "phapthuat.param." + key;
    }
}
