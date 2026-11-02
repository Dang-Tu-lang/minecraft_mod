package phapthuat.physics;

import java.util.Map;

/** Khối lượng sinh vật (kg, ước lượng game, spec mục 7.3). */
public final class EntityMass {
    /** Sinh vật không có trong bảng: 100 kg cho mỗi m³ thể tích hitbox. */
    public static final double FALLBACK_KG_PER_M3 = 100;

    private static final Map<String, Double> TABLE = Map.ofEntries(
            Map.entry("minecraft:player", 70.0),
            Map.entry("minecraft:zombie", 70.0),
            Map.entry("minecraft:husk", 70.0),
            Map.entry("minecraft:drowned", 70.0),
            Map.entry("minecraft:zombie_villager", 70.0),
            Map.entry("minecraft:villager", 70.0),
            Map.entry("minecraft:skeleton", 30.0),
            Map.entry("minecraft:stray", 30.0),
            Map.entry("minecraft:creeper", 50.0),
            Map.entry("minecraft:spider", 30.0),
            Map.entry("minecraft:enderman", 80.0),
            Map.entry("minecraft:pig", 100.0),
            Map.entry("minecraft:sheep", 60.0),
            Map.entry("minecraft:cow", 600.0),
            Map.entry("minecraft:horse", 500.0),
            Map.entry("minecraft:chicken", 2.0),
            Map.entry("minecraft:wolf", 40.0),
            Map.entry("minecraft:iron_golem", 500.0));

    private EntityMass() {}

    public static double kg(String entityTypeId, double hitboxVolumeM3) {
        Double known = TABLE.get(entityTypeId);
        return known != null ? known : FALLBACK_KG_PER_M3 * hitboxVolumeM3;
    }

    public static Map<String, Double> table() {
        return TABLE;
    }
}
