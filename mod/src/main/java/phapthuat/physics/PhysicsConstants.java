package phapthuat.physics;

/** Hằng số vật lý dùng chung (spec mục 7.1). Đơn vị SI. */
public final class PhysicsConstants {
    public static final double KELVIN_OFFSET = 273.15;
    public static final double G = 9.81;
    public static final double R = 8.314;
    public static final double P0 = 101_325;
    public static final double AIR_DENSITY = 1.2;
    public static final double AIR_CV = 718;
    /** Lõi 1 lít mà phép nhiệt tác động lên mỗi khối (spec 6.6). */
    public static final double CORE_VOLUME_M3 = 0.001;
    public static final double WATER_MOL_PER_KG = 55.51;
    /** Nhiệt dung riêng đẳng áp của hơi nước, J/(kg·K). */
    public static final double STEAM_CP = 2000;
    public static final double GLASS_BREAK_PA = 7_000;
    public static final double BLAST_DAMAGE_PA = 35_000;
    /** Công giãn nở ứng với công suất nổ 4 (TNT vanilla): p = 4·W / giá trị này. */
    public static final double TNT_REFERENCE_WORK_J = 400_000;
    public static final double MAX_EXPLOSION_POWER = 8;
    /** Không khí nóng từ mức này trở lên làm sinh vật bốc cháy. */
    public static final double HOT_AIR_IGNITE_C = 300;

    private PhysicsConstants() {}

    public static double toKelvin(double celsius) {
        return celsius + KELVIN_OFFSET;
    }
}
