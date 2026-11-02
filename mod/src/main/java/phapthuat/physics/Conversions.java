package phapthuat.physics;

import java.util.EnumMap;
import java.util.Map;

/**
 * Hiệu suất chuyển hóa giữa các dạng năng lượng (spec mục 7.4), dành cho chuỗi ghép phép ở đợt sau.
 * Nhiệt → Động trả về {@link Double#NaN}: phải tính theo Carnot η = 1 − T_lạnh/T_nóng.
 * Cặp không có trong bảng trả về 0 (không chuyển trực tiếp được).
 */
public final class Conversions {
    private static final Map<Element, Map<Element, Double>> TABLE = new EnumMap<>(Element.class);

    static {
        put(Element.DIEN_TU, Element.NHIET, 1.0);
        put(Element.DIEN_TU, Element.DONG, 0.9);
        put(Element.DIEN_TU, Element.QUANG, 0.4);
        put(Element.DONG, Element.NHIET, 1.0);
        put(Element.DONG, Element.DIEN_TU, 0.9);
        put(Element.HOA, Element.NHIET, 1.0);
        put(Element.HOA, Element.DIEN_TU, 0.6);
        put(Element.QUANG, Element.NHIET, 1.0);
        put(Element.QUANG, Element.DIEN_TU, 0.2);
        put(Element.HAT_NHAN, Element.NHIET, 1.0);
        put(Element.HAP_DAN, Element.DONG, 1.0);
        put(Element.NHIET, Element.DONG, Double.NaN);
    }

    private Conversions() {}

    private static void put(Element from, Element to, double efficiency) {
        TABLE.computeIfAbsent(from, k -> new EnumMap<>(Element.class)).put(to, efficiency);
    }

    public static double efficiency(Element from, Element to) {
        Map<Element, Double> row = TABLE.get(from);
        return row == null ? 0 : row.getOrDefault(to, 0.0);
    }
}
