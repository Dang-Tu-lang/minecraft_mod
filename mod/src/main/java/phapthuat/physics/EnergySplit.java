package phapthuat.physics;

/** Luật chia năng lượng đều cho các đơn vị của một thao tác (spec mục 6.5). */
public final class EnergySplit {
    private EnergySplit() {}

    public static double perUnit(double total, int units) {
        return units <= 0 ? 0 : total / units;
    }
}
