package phapthuat.physics;

/** Lõi tính toán của phép Nâng (spec mục 8.9): thế năng E = m·g·h. */
public final class LiftCalc {
    /** Thế năng (J) đổi ra máu mất khi khối rơi trúng: 1 máu cho mỗi kJ. */
    private static final double JOULES_PER_HP = 1000;

    private LiftCalc() {}

    public static double energy(double massKg, double heightM) {
        return massKg * PhysicsConstants.G * heightM;
    }

    /** Máu mất khi khối khối lượng m rơi từ độ cao h trúng sinh vật. */
    public static double impactDamageHp(double massKg, double fallM) {
        return energy(massKg, fallM) / JOULES_PER_HP;
    }
}
