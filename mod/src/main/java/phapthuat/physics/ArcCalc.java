package phapthuat.physics;

/** Lõi tính toán của phép Hồ Quang (spec mục 8.4): điện trường E = U/d, ngưỡng đánh thủng, năng lượng ½·C·U². */
public final class ArcCalc {
    /** Khoảng cách tới vật dẫn gần nhất là chính người chơi khi phóng hụt, m. */
    public static final double BACKFIRE_DISTANCE_M = 0.5;
    /** Năng lượng phóng (J) cho mỗi máu sát thương: 20 kJ. */
    private static final double JOULES_PER_HP = 20_000;
    /** Từ mức này (mana) trở lên có hiệu ứng sét của vanilla. */
    private static final double LIGHTNING_MANA = 50;
    private static final double EPS = 1e-6;

    private ArcCalc() {}

    /** Hiệu điện thế cần để đánh thủng không khí qua khoảng cách d. Trời mưa ngưỡng giảm. */
    public static double requiredVoltage(double distanceM, boolean raining) {
        double field = PhysicsConstants.BREAKDOWN_V_PER_M * (raining ? PhysicsConstants.RAIN_BREAKDOWN_FACTOR : 1);
        return field * distanceM;
    }

    public static ArcOutcome outcome(double voltageV, double distanceM, boolean raining) {
        if (voltageV + EPS >= requiredVoltage(distanceM, raining)) return ArcOutcome.HIT;
        if (voltageV + EPS >= requiredVoltage(BACKFIRE_DISTANCE_M, raining)) return ArcOutcome.BACKFIRE_CASTER;
        return ArcOutcome.FIZZLE;
    }

    /** Năng lượng tích điện cơ thể ½·C·U², tính vào chi phí. */
    public static double chargeEnergyJ(double voltageV) {
        return 0.5 * PhysicsConstants.BODY_CAPACITANCE_F * voltageV * voltageV;
    }

    public static double damageHp(double energyJ) {
        return energyJ / JOULES_PER_HP;
    }

    public static boolean lightningEffects(double energyJ, double joulesPerMana) {
        return energyJ / joulesPerMana + EPS >= LIGHTNING_MANA;
    }
}
