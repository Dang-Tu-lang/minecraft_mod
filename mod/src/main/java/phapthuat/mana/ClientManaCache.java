package phapthuat.mana;

/** Giá trị mana mà client nhận được từ server, để vẽ HUD. Không import lớp client nên nạp ở đâu cũng an toàn. */
public final class ClientManaCache {
    private static volatile double mana = -1;
    private static volatile double max = 1000;

    private ClientManaCache() {}

    public static void set(double value, double maxValue) {
        mana = value;
        max = maxValue;
    }

    /** Âm nghĩa là chưa nhận được gói đồng bộ nào. */
    public static double mana() {
        return mana;
    }

    public static double max() {
        return max;
    }
}
