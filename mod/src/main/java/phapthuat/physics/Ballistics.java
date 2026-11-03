package phapthuat.physics;

/** Chuyển động ném xiên của đạn phép, bỏ qua lực cản (spec mục 6.5). 1 khối = 1 m, 20 tick/giây. */
public final class Ballistics {
    public static final int TICKS_PER_SECOND = 20;

    private Ballistics() {}

    /** Tầm xa trên mặt phẳng: R = v²·sin(2θ)/g. */
    public static double range(double speed, double thetaRad) {
        return speed * speed * Math.sin(2 * thetaRad) / PhysicsConstants.G;
    }

    /** Vận tốc đứng (m/s) sau một tick trọng lực. */
    public static double vyAfterTick(double vy) {
        return vy - PhysicsConstants.G / TICKS_PER_SECOND;
    }
}
