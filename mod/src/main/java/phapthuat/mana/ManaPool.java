package phapthuat.mana;

/** Quỹ mana: không bao giờ âm, không bao giờ vượt tối đa. Java thuần để test được không cần Minecraft. */
public final class ManaPool {
    private final double max;
    private double current;

    public ManaPool(double max) {
        this(max, max);
    }

    public ManaPool(double max, double current) {
        this.max = max;
        this.current = Math.max(0, Math.min(max, current));
    }

    public double current() {
        return current;
    }

    public double max() {
        return max;
    }

    public boolean trySpend(double mana) {
        if (mana < 0 || mana > current + 1e-9) return false;
        current = Math.max(0, current - mana);
        return true;
    }

    public void add(double mana) {
        current = Math.max(0, Math.min(max, current + mana));
    }

    public void regen(double seconds, double perSecond) {
        add(seconds * perSecond);
    }

    public void fill() {
        current = max;
    }
}
