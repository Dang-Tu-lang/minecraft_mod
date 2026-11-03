package phapthuat.physics;

/** Kết quả phóng Hồ Quang (spec mục 8.4). */
public enum ArcOutcome {
    /** Đủ điện trường cho cả khoảng cách: tia nối người chơi với mục tiêu. */
    HIT,
    /** Không đủ cho khoảng cách nhưng đủ cho 0,5 m: điện phóng vào chính người chơi. */
    BACKFIRE_CASTER,
    /** Quá thấp cả cho 0,5 m: chỉ lách tách. */
    FIZZLE
}
