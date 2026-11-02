package phapthuat.spell;

import java.util.OptionalDouble;
import java.util.regex.Pattern;

/** Đọc số người chơi gõ vào sách: nhận cả dấu phẩy kiểu Việt lẫn dấu chấm làm dấu thập phân. */
public final class ParamParser {
    private static final Pattern NUMBER = Pattern.compile("[+-]?(\\d+([.,]\\d*)?|[.,]\\d+)([eE][+-]?\\d+)?");

    private ParamParser() {}

    public static OptionalDouble parse(String text) {
        if (text == null) return OptionalDouble.empty();
        String s = text.trim();
        if (!NUMBER.matcher(s).matches()) return OptionalDouble.empty();
        double v = Double.parseDouble(s.replace(',', '.'));
        return Double.isFinite(v) ? OptionalDouble.of(v) : OptionalDouble.empty();
    }
}
