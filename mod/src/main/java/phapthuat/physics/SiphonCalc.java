package phapthuat.physics;

/**
 * Lõi tính toán của phép Thu Nhiệt (spec mục 8.2). Rút nhiệt từ vật nóng hơn môi trường thì được năng lượng theo
 * hiệu suất Carnot; làm lạnh xuống dưới môi trường thì tốn công theo COP của máy lạnh. Nhiệt độ tính bằng Kelvin
 * trong mọi công thức hiệu suất.
 */
public final class SiphonCalc {
    private static final double EPS = 1e-6;
    /** Không làm lạnh vật gì xuống dưới mức này, để công thức COP không vỡ gần 0 K. */
    private static final double FLOOR_C = -270;

    /**
     * @param manaJoulesDelta dương là năng lượng người chơi nhận được, âm là công phải trả (chưa tính phí kích hoạt)
     * @param transformed mục tiêu đã nguội hẳn / đông hẳn / tắt hẳn
     * @param capped nhiệt yêu cầu vượt quá phần còn rút được; phần dư bị bỏ qua
     */
    public record SiphonResult(double finalTempC, double phaseProgressJ, double heatRemovedJ, double manaJoulesDelta,
            boolean transformed, boolean capped) {}

    private SiphonCalc() {}

    public static ColdTier coldTier(double dropK) {
        if (dropK >= 9) return ColdTier.SEVERE;
        if (dropK >= 5) return ColdTier.FREEZE;
        if (dropK >= 2) return ColdTier.SLOW;
        return ColdTier.NONE;
    }

    public static SiphonResult siphon(Material m, double massKg, double startTempC, double phaseProgressJ,
            double heatJ, double ambientC) {
        Draw d = new Draw(startTempC, phaseProgressJ, heatJ, ambientC);
        boolean transformed;

        if (!Double.isNaN(m.maxExtractableJ())) {
            transformed = d.finite(m.maxExtractableJ());
        } else if (!Double.isNaN(m.naturalTempC())) {
            transformed = drainHot(m, massKg, d, ambientC);
        } else if (m == Materials.WATER) {
            Material ice = Materials.ICE;
            transformed = d.coolTo(massKg, m.specificHeat(), ice.meltC())
                    && d.plateau(massKg * ice.latentFusion(), ice.meltC());
        } else {
            d.coolTo(massKg, m.specificHeat(), FLOOR_C);
            transformed = false;
        }

        double startK = PhysicsConstants.toKelvin(startTempC);
        double ambientK = PhysicsConstants.toKelvin(ambientC);
        double gain = d.aboveJ > 0 ? d.aboveJ * (1 - ambientK / startK) : 0;
        double finalK = PhysicsConstants.toKelvin(d.tempC);
        double cost = d.belowJ > 0 && finalK < ambientK ? d.belowJ / (finalK / (ambientK - finalK)) : 0;

        return new SiphonResult(d.tempC, d.progressJ, d.aboveJ + d.belowJ, gain - cost, transformed,
                d.remainingJ > EPS);
    }

    /** Dung nham, magma: nhả nhiệt đông đặc trước (nếu đang ở điểm nóng chảy), rồi nguội về môi trường. */
    private static boolean drainHot(Material m, double massKg, Draw d, double ambientC) {
        if (m.meltable() && d.tempC >= m.meltC() - EPS) {
            if (!d.plateau(massKg * m.latentFusion(), m.meltC())) return false;
            d.progressJ = 0;
            d.tempC = m.meltC() - EPS;
        }
        return d.coolTo(massKg, m.specificHeat(), ambientC);
    }

    private static final class Draw {
        double tempC;
        double progressJ;
        double remainingJ;
        final double ambientC;
        /** Nhiệt đã rút khi vật còn nóng hơn môi trường / khi đã lạnh hơn môi trường. */
        double aboveJ;
        double belowJ;

        Draw(double tempC, double progressJ, double heatJ, double ambientC) {
            this.tempC = tempC;
            this.progressJ = progressJ;
            this.remainingJ = heatJ;
            this.ambientC = ambientC;
        }

        /** Nguồn nhiệt hữu hạn (lửa): rút tối đa phần còn lại; hết thì tắt và về nhiệt độ môi trường. */
        boolean finite(double maxJ) {
            double available = Math.max(0, maxJ - progressJ);
            double take = Math.min(remainingJ, available);
            remainingJ -= take;
            progressJ += take;
            book(take, tempC);
            if (take + EPS < available) return false;
            tempC = ambientC;
            return true;
        }

        /** Hạ nhiệt cảm nhận tới {@code targetC}. Trả false nếu hết nhiệt yêu cầu trước khi tới. */
        boolean coolTo(double massKg, double c, double targetC) {
            if (tempC <= targetC) return true;
            double need = massKg * c * (tempC - targetC);
            double take = Math.min(remainingJ, need);
            double newTemp = tempC - take / (massKg * c);
            double perK = massKg * c;
            aboveJ += perK * Math.max(0, Math.max(tempC, ambientC) - Math.max(newTemp, ambientC));
            belowJ += perK * Math.max(0, Math.min(tempC, ambientC) - Math.min(newTemp, ambientC));
            remainingJ -= take;
            if (take + EPS >= need) {
                tempC = targetC;
                return true;
            }
            tempC = newTemp;
            return false;
        }

        /** Rút nhiệt chuyển pha (đông đặc) ở nhiệt độ {@code atC}. Trả false nếu chưa rút hết. */
        boolean plateau(double latentTotalJ, double atC) {
            double need = Math.max(0, latentTotalJ - progressJ);
            double take = Math.min(remainingJ, need);
            remainingJ -= take;
            progressJ += take;
            book(take, atC);
            return take + EPS >= need;
        }

        private void book(double joules, double atC) {
            if (atC > ambientC) aboveJ += joules;
            else belowJ += joules;
        }
    }
}
