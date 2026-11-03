package phapthuat.physics;

/** Lõi tính toán của phép Từ Trường (spec mục 8.5): mật độ năng lượng u = B²/(2μ₀), lực hút tăng theo B². */
public final class MagnetCalc {
    private MagnetCalc() {}

    /** J/m³. */
    public static double energyDensity(double teslas) {
        return teslas * teslas / (2 * PhysicsConstants.MU0);
    }

    /** Năng lượng trường trong quả cầu hình học bán kính r: u × 4/3·π·r³. */
    public static double fieldEnergyJ(double teslas, double radiusM) {
        return energyDensity(teslas) * 4.0 / 3.0 * Math.PI * radiusM * radiusM * radiusM;
    }

    /** Gia tốc hút về tâm a = k·B²·(m_sắt/m), m/s². */
    public static double accel(double k, double teslas, double ironKg, double massKg) {
        if (massKg <= 0 || ironKg <= 0) return 0;
        return k * teslas * teslas * ironKg / massKg;
    }
}
