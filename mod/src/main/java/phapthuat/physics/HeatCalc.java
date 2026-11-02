package phapthuat.physics;

/**
 * Lõi tính toán của phép Gia Nhiệt (spec mục 8.1). Năng lượng tính bằng Joule, nhiệt độ bằng °C.
 *
 * <p>Mô hình entanpi từng đoạn: nhiệt độ tăng theo m·c tới điểm chuyển pha kế tiếp; tại đó năng lượng dồn vào
 * bình nguyên (lưu ở {@code phaseProgressJ}) cho tới khi đủ m·L, rồi mới tăng tiếp. Sốc nhiệt (nứt) xét theo ΔT
 * của riêng lần thi triển này, không cộng dồn.
 */
public final class HeatCalc {
    /** Dung sai cho phép so sánh năng lượng, tránh sai số dấu phẩy động làm hụt đúng ngưỡng. */
    private static final double EPS = 1e-6;

    public record HeatResult(HeatTier tier, double finalTempC, double phaseProgressJ, double excessJ,
            double highestTierRequirementJ, boolean backlash, int backlashRadius) {}

    public record SteamBurst(double steamTempK, double workJ, double power) {}

    public record AirBurst(double deltaTK, double finalTempK, double deltaPPa) {}

    private HeatCalc() {}

    public static HeatResult heat(Material m, double massKg, double startTempC, double phaseProgressJ,
            double energyJ) {
        Walk walk = walk(m, massKg, startTempC, phaseProgressJ, energyJ);

        HeatTier tier = walk.tier;
        double excess = walk.excessJ;
        if (tier.compareTo(HeatTier.CRACK) < 0 && cracks(m, massKg, energyJ)) {
            tier = HeatTier.CRACK;
            excess = energyJ - crackRequirement(m, massKg);
        }

        double requirement = highestTierRequirement(m, massKg, startTempC, phaseProgressJ);
        boolean backlash = requirement > 0 && energyJ + EPS >= 3 * requirement;
        int radius = backlash ? (int) Math.min(4, 1 + Math.floor(energyJ / requirement - 3 + 1e-9)) : 0;

        return new HeatResult(tier, walk.tempC, walk.progressJ, Math.max(0, excess), requirement, backlash, radius);
    }

    public static SteamBurst steamBurst(double waterMassKg, double excessJ) {
        double steamTempK = PhysicsConstants.toKelvin(100) + excessJ / (waterMassKg * PhysicsConstants.STEAM_CP);
        double work = PhysicsConstants.WATER_MOL_PER_KG * waterMassKg * PhysicsConstants.R * steamTempK;
        double power = Math.min(PhysicsConstants.MAX_EXPLOSION_POWER, 4 * work / PhysicsConstants.TNT_REFERENCE_WORK_J);
        return new SteamBurst(steamTempK, work, power);
    }

    public static AirBurst airBurst(int airCells, double energyJ, double ambientC) {
        double mass = airCells * PhysicsConstants.AIR_DENSITY;
        double deltaT = mass > 0 ? energyJ / (mass * PhysicsConstants.AIR_CV) : 0;
        double t1 = PhysicsConstants.toKelvin(ambientC);
        double t2 = t1 + deltaT;
        return new AirBurst(deltaT, t2, PhysicsConstants.P0 * (t2 / t1 - 1));
    }

    /** Số giây sinh vật cháy: 3 giây, cộng 1 giây cho mỗi 10 mana dư, tối đa 10. */
    public static int burnSeconds(double excessJ, double joulesPerMana) {
        double excessMana = excessJ / joulesPerMana;
        return (int) Math.min(10, 3 + Math.floor(excessMana / 10));
    }

    private static boolean cracks(Material m, double massKg, double energyJ) {
        return m.brittle() && energyJ + EPS >= crackRequirement(m, massKg);
    }

    private static double crackRequirement(Material m, double massKg) {
        return massKg * m.specificHeat() * m.crackDeltaK();
    }

    /** Năng lượng cần, tính từ trạng thái hiện tại, để đạt bậc cao nhất mà vật liệu có. 0 nếu không có bậc nào. */
    private static double highestTierRequirement(Material m, double massKg, double startTempC, double phaseProgressJ) {
        HeatTier top = highestTier(m);
        return switch (top) {
            case NONE -> 0;
            case CRACK -> crackRequirement(m, massKg);
            default -> walk(m, massKg, startTempC, phaseProgressJ, Double.POSITIVE_INFINITY).usedForTier(top);
        };
    }

    private static HeatTier highestTier(Material m) {
        if (m.boils()) return HeatTier.VAPORIZE;
        if (m.meltable()) return HeatTier.MELT;
        if (m.flammable()) return HeatTier.IGNITE;
        if (m.brittle()) return HeatTier.CRACK;
        return HeatTier.NONE;
    }

    /** Đi dọc đường entanpi với năng lượng cho trước; ghi lại năng lượng đã dùng để chạm từng bậc. */
    private static Walk walk(Material m, double massKg, double startTempC, double phaseProgressJ, double energyJ) {
        Walk w = new Walk(startTempC, phaseProgressJ, energyJ);
        double c = m.specificHeat();

        if (m.flammable()) {
            if (!w.sensible(massKg, c, m.ignitionC())) return w;
            w.reach(HeatTier.IGNITE);
            return w;
        }
        if (m.meltable()) {
            if (!w.sensible(massKg, c, m.meltC())) return w;
            if (!w.plateau(massKg * m.latentFusion())) return w;
            w.reach(HeatTier.MELT);
            if (!m.boils()) return w;
            c = m.liquidSpecificHeat();
        }
        if (m.boils()) {
            if (!w.sensible(massKg, c, m.boilC())) return w;
            if (!w.plateau(massKg * m.latentVaporization())) return w;
            w.reach(HeatTier.VAPORIZE);
            return w;
        }
        w.sensible(massKg, c, Double.POSITIVE_INFINITY);
        return w;
    }

    private static final class Walk {
        double tempC;
        double progressJ;
        double remainingJ;
        /** Năng lượng đã tiêu, cộng dồn trực tiếp (không lấy năng lượng đầu trừ còn lại, vì có thể là vô hạn). */
        double usedJ;
        HeatTier tier = HeatTier.NONE;
        double excessJ;
        private final java.util.EnumMap<HeatTier, Double> usedAt = new java.util.EnumMap<>(HeatTier.class);

        Walk(double tempC, double progressJ, double energyJ) {
            this.tempC = tempC;
            this.progressJ = progressJ;
            this.remainingJ = energyJ;
        }

        /** Tăng nhiệt cảm nhận tới {@code targetC}. Trả false nếu hết năng lượng trước khi tới. */
        boolean sensible(double massKg, double c, double targetC) {
            if (tempC >= targetC) return true;
            double need = massKg * c * (targetC - tempC);
            if (remainingJ + EPS >= need) {
                remainingJ = Math.max(0, remainingJ - need);
                usedJ += need;
                tempC = targetC;
                return true;
            }
            tempC += remainingJ / (massKg * c);
            usedJ += remainingJ;
            remainingJ = 0;
            return false;
        }

        /** Đổ năng lượng vào bình nguyên chuyển pha. Trả false nếu chưa đầy. */
        boolean plateau(double latentTotalJ) {
            double need = Math.max(0, latentTotalJ - progressJ);
            if (remainingJ + EPS >= need) {
                remainingJ = Math.max(0, remainingJ - need);
                usedJ += need;
                progressJ = 0;
                return true;
            }
            progressJ += remainingJ;
            usedJ += remainingJ;
            remainingJ = 0;
            return false;
        }

        void reach(HeatTier reached) {
            tier = reached;
            excessJ = remainingJ;
            usedAt.put(reached, usedJ);
        }

        double usedForTier(HeatTier t) {
            return usedAt.getOrDefault(t, 0.0);
        }
    }
}
