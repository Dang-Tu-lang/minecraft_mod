package phapthuat.physics;

/** Lõi tính toán của phép Xung Lực (spec mục 8.3): động năng E = ½·m·v², xung lượng p = m·v. */
public final class ImpulseCalc {
    private ImpulseCalc() {}

    /** Vận tốc đạt được v = √(2E/m), kẹp {@link PhysicsConstants#MAX_SPELL_SPEED}. */
    public static double speed(double energyJ, double massKg) {
        if (massKg <= 0 || energyJ <= 0) return 0;
        return Math.min(PhysicsConstants.MAX_SPELL_SPEED, Math.sqrt(2 * energyJ / massKg));
    }

    /** Xung lượng p = √(2·m·E), không kẹp (định luật 3 Newton: người thi triển nhận xung lượng ngược chiều). */
    public static double impulse(double energyJ, double massKg) {
        if (massKg <= 0 || energyJ <= 0) return 0;
        return Math.sqrt(2 * massKg * energyJ);
    }

    /** Vận tốc giật lùi của người thi triển: tổng xung lượng / khối lượng người, kẹp vận tốc tối đa. */
    public static double recoilSpeed(double totalImpulse) {
        return Math.min(totalImpulse / PhysicsConstants.CASTER_MASS_KG, PhysicsConstants.MAX_SPELL_SPEED);
    }
}
