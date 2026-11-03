package phapthuat.physics;

/** Lõi tính toán của phép Đốt Nhiên Liệu (spec mục 8.7): nhiệt cháy, oxy theo quy tắc Thornton, khí CO. */
public final class CombustionCalc {
    /** Từ số ô không khí liên thông này trở lên coi như ngoài trời thoáng. */
    public static final int OPEN_AIR_CELLS = 64;

    /**
     * @param releasedJ nhiệt thực sự tỏa ra (giảm theo tỉ lệ oxy có / oxy cần)
     * @param carbonMonoxide thiếu oxy, sinh khí CO
     */
    public record Combustion(double releasedJ, boolean carbonMonoxide) {}

    private CombustionCalc() {}

    /** Năng lượng một vật phẩm = (thời gian cháy trong lò / 1600) × 8,2 MJ. */
    public static double itemEnergyJ(int burnTicks) {
        return (double) burnTicks / PhysicsConstants.COAL_BURN_TICKS * PhysicsConstants.COAL_ENERGY_J;
    }

    public static double oxygenNeededKg(double energyJ) {
        return energyJ / PhysicsConstants.THORNTON_J_PER_KG_O2;
    }

    public static Combustion burn(double energyJ, int connectedAirCells) {
        if (connectedAirCells >= OPEN_AIR_CELLS) return new Combustion(energyJ, false);
        double available = connectedAirCells * PhysicsConstants.O2_KG_PER_M3;
        double needed = oxygenNeededKg(energyJ);
        if (available >= needed) return new Combustion(energyJ, false);
        return new Combustion(energyJ * available / needed, true);
    }
}
