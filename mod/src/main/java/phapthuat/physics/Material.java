package phapthuat.physics;

/**
 * Một vật liệu và các hằng số vật lý của nó (spec mục 7.2). Đơn vị SI: kg/m³, J/(kg·K), J/kg, Pa, 1/K.
 * Trường không áp dụng cho vật liệu này mang giá trị {@link Double#NaN}.
 *
 * @param naturalTempC nhiệt độ tự nhiên của khối nóng (dung nham, lửa...); NaN nghĩa là bằng nhiệt độ môi trường
 * @param maxExtractableJ nhiệt tối đa rút được của nguồn nóng hữu hạn (lửa, lửa trại); NaN nếu tính từ m·c·ΔT
 * @param estimated số liệu là ước lượng cho game, sách phải ghi "(ước lượng)"
 */
public record Material(
        String id,
        double density,
        double specificHeat,
        double ignitionC,
        double meltC,
        double latentFusion,
        double liquidSpecificHeat,
        double boilC,
        double latentVaporization,
        double youngsModulus,
        double thermalExpansion,
        double tensileStrength,
        double naturalTempC,
        double maxExtractableJ,
        boolean estimated) {

    public boolean flammable() {
        return !Double.isNaN(ignitionC);
    }

    public boolean meltable() {
        return !Double.isNaN(meltC);
    }

    public boolean boils() {
        return !Double.isNaN(boilC);
    }

    public boolean brittle() {
        return !Double.isNaN(tensileStrength);
    }

    public double coreMassKg() {
        return density * PhysicsConstants.CORE_VOLUME_M3;
    }

    /** Độ tăng nhiệt độ đột ngột đủ làm vật nứt vỡ: σ_bền / (E·α). */
    public double crackDeltaK() {
        return brittle() ? tensileStrength / (youngsModulus * thermalExpansion) : Double.NaN;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private double density = Double.NaN;
        private double specificHeat = Double.NaN;
        private double ignitionC = Double.NaN;
        private double meltC = Double.NaN;
        private double latentFusion = Double.NaN;
        private double liquidSpecificHeat = Double.NaN;
        private double boilC = Double.NaN;
        private double latentVaporization = Double.NaN;
        private double youngsModulus = Double.NaN;
        private double thermalExpansion = Double.NaN;
        private double tensileStrength = Double.NaN;
        private double naturalTempC = Double.NaN;
        private double maxExtractableJ = Double.NaN;
        private boolean estimated;

        private Builder(String id) {
            this.id = id;
        }

        public Builder thermal(double density, double specificHeat) {
            this.density = density;
            this.specificHeat = specificHeat;
            return this;
        }

        public Builder ignites(double ignitionC) {
            this.ignitionC = ignitionC;
            return this;
        }

        public Builder melts(double meltC, double latentFusion) {
            this.meltC = meltC;
            this.latentFusion = latentFusion;
            return this;
        }

        public Builder boils(double liquidSpecificHeat, double boilC, double latentVaporization) {
            this.liquidSpecificHeat = liquidSpecificHeat;
            this.boilC = boilC;
            this.latentVaporization = latentVaporization;
            return this;
        }

        public Builder brittle(double youngsModulus, double thermalExpansion, double tensileStrength) {
            this.youngsModulus = youngsModulus;
            this.thermalExpansion = thermalExpansion;
            this.tensileStrength = tensileStrength;
            return this;
        }

        public Builder hot(double naturalTempC) {
            this.naturalTempC = naturalTempC;
            return this;
        }

        public Builder maxExtractable(double joules) {
            this.maxExtractableJ = joules;
            return this;
        }

        public Builder estimated() {
            this.estimated = true;
            return this;
        }

        public Material build() {
            return new Material(id, density, specificHeat, ignitionC, meltC, latentFusion, liquidSpecificHeat,
                    boilC, latentVaporization, youngsModulus, thermalExpansion, tensileStrength, naturalTempC,
                    maxExtractableJ, estimated);
        }
    }
}
