package phapthuat.physics;

import java.util.List;
import java.util.Map;

/** Lượng sắt (kg) trong thực thể và vật phẩm, cho phép Từ Trường (spec mục 7.3). 1 thỏi sắt = 1 kg. */
public final class IronContent {
    private static final Map<String, Double> ENTITY_KG = Map.of(
            "minecraft:iron_golem", 500.0);
    /** Giáp sắt tính theo số thỏi để chế tạo. */
    private static final Map<String, Double> ARMOR_KG = Map.of(
            "minecraft:iron_helmet", 5.0,
            "minecraft:iron_chestplate", 8.0,
            "minecraft:iron_leggings", 7.0,
            "minecraft:iron_boots", 4.0);
    private static final double MINECART_KG = 5;
    private static final Map<String, Double> ITEM_KG = Map.of(
            "minecraft:iron_ingot", 1.0,
            "minecraft:iron_block", 9.0,
            "minecraft:iron_nugget", 1.0 / 9);

    private IronContent() {}

    /** Sắt của thực thể: golem sắt toàn bộ khối lượng, xe goòng 5 kg, cộng giáp sắt đang mặc. */
    public static double kg(String entityTypeId, List<String> wornItemIds) {
        double kg = ENTITY_KG.getOrDefault(entityTypeId, 0.0);
        if (entityTypeId.endsWith("minecart")) kg += MINECART_KG;
        for (String id : wornItemIds) kg += ARMOR_KG.getOrDefault(id, 0.0);
        return kg;
    }

    /** Sắt của một chồng vật phẩm rơi trên đất: thỏi 1 kg, khối 9 kg, hạt 1/9 kg; đồ sắt khác không tính. */
    public static double itemKg(String itemId, int count) {
        return ITEM_KG.getOrDefault(itemId, 0.0) * count;
    }
}
