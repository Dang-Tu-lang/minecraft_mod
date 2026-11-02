package phapthuat;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Các con số cân bằng chỉnh được (spec mục 6.1, 6.8, 8.1, 8.2). Số liệu vật lý nằm ở gói physics. */
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue JOULES_PER_MANA = BUILDER
            .comment("Số Joule ứng với 1 mana.")
            .defineInRange("joulesPerMana", 10_000.0, 1.0, 1e9);
    public static final ModConfigSpec.DoubleValue MAX_MANA = BUILDER
            .comment("Mana tối đa của người chơi.")
            .defineInRange("maxMana", 1000.0, 1.0, 1e9);
    public static final ModConfigSpec.DoubleValue REGEN_PER_SECOND = BUILDER
            .comment("Mana hồi mỗi giây.")
            .defineInRange("regenPerSecond", 2.0, 0.0, 1e6);
    public static final ModConfigSpec.DoubleValue HALF_LIFE_SECONDS = BUILDER
            .comment("Chu kỳ bán rã nhiệt độ của khối và sinh vật về nhiệt độ tự nhiên (giây).")
            .defineInRange("halfLifeSeconds", 20.0, 0.1, 1e6);
    public static final ModConfigSpec.DoubleValue HOT_SOURCE_HALF_LIFE_SECONDS = BUILDER
            .comment("Chu kỳ bán rã hồi nhiệt của dung nham, magma sau khi bị rút nhiệt (giây).")
            .defineInRange("hotSourceHalfLifeSeconds", 300.0, 0.1, 1e6);
    public static final ModConfigSpec.DoubleValue DOSE_HALF_LIFE_SECONDS = BUILDER
            .comment("Chu kỳ bán rã liều phóng xạ (giây).")
            .defineInRange("doseHalfLifeSeconds", 60.0, 0.1, 1e6);
    public static final ModConfigSpec.DoubleValue SIPHON_GAIN_FACTOR = BUILDER
            .comment("Hệ số nhân mana nhận được từ Thu Nhiệt.")
            .defineInRange("siphonGainFactor", 1.0, 0.0, 100.0);
    public static final ModConfigSpec.BooleanValue STEAM_EXPLOSION_BREAKS_BLOCKS = BUILDER
            .comment("Vụ nổ hơi nước có phá khối hay không.")
            .define("steamExplosionBreaksBlocks", true);
    public static final ModConfigSpec.DoubleValue AIR_BURST_KNOCKBACK_PER_KPA = BUILDER
            .comment("Vận tốc hất văng (khối/tick) cho mỗi kPa áp suất dư của khí nóng.")
            .defineInRange("airBurstKnockbackPerKpa", 0.03, 0.0, 10.0);
    public static final ModConfigSpec.IntValue MAX_TRACKED_BLOCKS = BUILDER
            .comment("Số khối tối đa được lưu nhiệt độ trong mỗi thế giới.")
            .defineInRange("maxTrackedBlocks", 10_000, 100, 1_000_000);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static double joulesPerMana() {
        return JOULES_PER_MANA.get();
    }
}
