package phapthuat.physics;

/** Lõi tính toán của phép Phát Quang (spec mục 8.6): độ nhạy mắt, quang thông, cường độ theo khoảng cách. */
public final class OpticsCalc {
    /** Bán kính chùm laser, m. */
    public static final double LASER_RADIUS_M = 0.01;
    /** Khoảng cách tối thiểu khi tính nguồn điểm, tránh chia cho 0 khi đứng sát quả cầu, m. */
    private static final double MIN_DISTANCE_M = 0.1;

    private OpticsCalc() {}

    /** Độ nhạy của mắt V(λ) ≈ 1,019·exp(−285,4·(λ_µm − 0,559)²). */
    public static double luminosity(double nm) {
        double d = nm / 1000 - 0.559;
        return 1.019 * Math.exp(-285.4 * d * d);
    }

    /** Quang thông Φ = 683 × V(λ) × P, lm. */
    public static double lumens(double nm, double watts) {
        return PhysicsConstants.LUMENS_PER_WATT_PEAK * luminosity(nm) * watts;
    }

    /** Mức sáng Minecraft = 6 + 4·log₁₀(Φ), làm tròn, kẹp 0–15. */
    public static int lightLevel(double lumens) {
        if (lumens <= 0) return 0;
        long level = Math.round(6 + 4 * Math.log10(lumens));
        return (int) Math.max(0, Math.min(15, level));
    }

    /** Nguồn điểm: I = P/(4πd²), W/m². */
    public static double intensity(double watts, double distanceM) {
        double d = Math.max(distanceM, MIN_DISTANCE_M);
        return watts / (4 * Math.PI * d * d);
    }

    /** Laser bán kính 1 cm: I = P / (π·r²), W/m². */
    public static double laserIntensity(double watts) {
        return watts / (Math.PI * LASER_RADIUS_M * LASER_RADIUS_M);
    }

    public static boolean isVisible(double nm) {
        return nm >= 380 && nm <= 750;
    }

    public static boolean isUv(double nm) {
        return nm < 400;
    }

    public static boolean isUvc(double nm) {
        return nm < 280;
    }

    public static boolean isInfrared(double nm) {
        return nm > 750;
    }
}
