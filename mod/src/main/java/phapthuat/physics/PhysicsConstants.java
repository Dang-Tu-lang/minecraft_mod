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

    // Kế hoạch 2 (spec 7.1, 8.3–8.9).
    /** Điện trường đánh thủng không khí khô, V/m; trời mưa nhân {@link #RAIN_BREAKDOWN_FACTOR}. */
    public static final double BREAKDOWN_V_PER_M = 3e6;
    public static final double RAIN_BREAKDOWN_FACTOR = 0.8;
    /** Điện dung cơ thể người, F. */
    public static final double BODY_CAPACITANCE_F = 100e-12;
    /** Độ từ thẩm chân không, H/m. */
    public static final double MU0 = 1.2566e-6;
    /** Hiệu suất phát sáng cực đại tại 555 nm, lm/W. */
    public static final double LUMENS_PER_WATT_PEAK = 683;
    /** Cường độ bức xạ gây đau / làm gỗ bắt cháy, W/m². */
    public static final double PAIN_W_M2 = 2_500;
    public static final double IGNITE_W_M2 = 12_500;
    /** Tia cực tím làm undead bốc cháy / UV-C làm mù, W/m² (ước lượng game). */
    public static final double UV_BURN_W_M2 = 10;
    public static final double UVC_BLIND_W_M2 = 1;
    /** Oxy trong 1 m³ không khí, kg. */
    public static final double O2_KG_PER_M3 = 0.276;
    /** Quy tắc Thornton: nhiệt tỏa ra trên mỗi kg O₂ khi đốt chất hữu cơ, J/kg. */
    public static final double THORNTON_J_PER_KG_O2 = 13.1e6;
    /** Hiệu suất tạo neutron (ước lượng game). */
    public static final double NEUTRON_EFFICIENCY = 2e-4;
    /** Năng lượng 1 than đá và thời gian cháy trong lò của nó (tick); nhiên liệu khác tính theo tỉ lệ thời gian cháy. */
    public static final double COAL_ENERGY_J = 8.2e6;
    public static final int COAL_BURN_TICKS = 1600;
    /** Vận tốc tối đa phép đặt cho bất kỳ thực thể nào, m/s (2 khối/tick). */
    public static final double MAX_SPELL_SPEED = 40;
    /** Khối lượng người thi triển khi tính giật lùi, kg. */
    public static final double CASTER_MASS_KG = 70;

    private PhysicsConstants() {}

    public static double toKelvin(double celsius) {
        return celsius + KELVIN_OFFSET;
    }
}
