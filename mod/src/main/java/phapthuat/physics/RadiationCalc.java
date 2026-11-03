package phapthuat.physics;

/** Lõi tính toán của phép Bức Xạ Neutron (spec mục 8.8): liều D = năng lượng hấp thụ / khối lượng. */
public final class RadiationCalc {
    /** Mỗi khối rắn / khối nước trên đường đi nhân năng lượng với hệ số này. */
    private static final double SOLID_ATTENUATION = 0.5;
    private static final double WATER_ATTENUATION = 0.1;
    /** Phần năng lượng hấp thụ của mục tiêu tán xạ sang sinh vật đứng gần. */
    private static final double SCATTER_FRACTION = 0.05;

    private RadiationCalc() {}

    /** Năng lượng thực sự thành neutron và được hấp thụ: E (mana) × J/mana × hiệu suất tạo neutron. */
    public static double absorbedJ(double mana, double joulesPerMana) {
        return mana * joulesPerMana * PhysicsConstants.NEUTRON_EFFICIENCY;
    }

    public static double attenuation(int solidBlocks, int waterBlocks) {
        return Math.pow(SOLID_ATTENUATION, solidBlocks) * Math.pow(WATER_ATTENUATION, waterBlocks);
    }

    public static double doseGy(double absorbedJ, double massKg) {
        return massKg > 0 ? absorbedJ / massKg : 0;
    }

    public static DoseTier tier(double gy) {
        if (gy >= 10) return DoseTier.SEVERE;
        if (gy >= 6) return DoseTier.WITHER;
        if (gy >= 2) return DoseTier.POISON;
        if (gy >= 0.5) return DoseTier.NAUSEA;
        return DoseTier.NONE;
    }

    /** Liều tán xạ lên sinh vật đứng gần: 5% năng lượng hấp thụ của mục tiêu chia cho khối lượng của nó. */
    public static double scatterDoseGy(double targetAbsorbedJ, double bystanderMassKg) {
        return doseGy(SCATTER_FRACTION * targetAbsorbedJ, bystanderMassKg);
    }
}
