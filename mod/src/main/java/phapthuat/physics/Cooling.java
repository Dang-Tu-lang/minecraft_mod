package phapthuat.physics;

/** Định luật làm nguội Newton viết theo chu kỳ bán rã (spec mục 6.8). */
public final class Cooling {
    private Cooling() {}

    /** Giá trị sau {@code dtSeconds} khi trôi về {@code target}: target + (value − target)·0,5^(dt/halfLife). */
    public static double relax(double value, double target, double dtSeconds, double halfLifeSeconds) {
        if (halfLifeSeconds <= 0) return target;
        return target + (value - target) * Math.pow(0.5, dtSeconds / halfLifeSeconds);
    }
}
