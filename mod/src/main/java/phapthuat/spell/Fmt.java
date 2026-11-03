package phapthuat.spell;

import java.util.Locale;

/** Định dạng số kiểu Việt Nam (dấu phẩy thập phân) cho mọi chữ hiển thị trong game. */
public final class Fmt {
    private static final Locale VI = Locale.forLanguageTag("vi-VN");

    private Fmt() {}

    public static String num(double v, int decimals) {
        return String.format(VI, "%." + decimals + "f", v);
    }

    public static String mana(double v) {
        return num(v, 1);
    }

    public static String tempC(double v) {
        return num(v, 1) + "°C";
    }
}
