package phapthuat.physics;

import java.util.Locale;

/** Bảy nguyên tố của hệ phép = bảy dạng năng lượng (spec mục 1.1). */
public enum Element {
    NHIET, DONG, DIEN_TU, QUANG, HOA, HAT_NHAN, HAP_DAN;

    public String langKey() {
        return "phapthuat.element." + name().toLowerCase(Locale.ROOT);
    }
}
