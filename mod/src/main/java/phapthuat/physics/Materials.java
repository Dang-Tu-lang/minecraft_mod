package phapthuat.physics;

import java.util.List;

/** Bảng vật liệu duy nhất của mod (spec mục 7.2 và lõi sinh vật ở 7.3). Sách đọc từ chính bảng này. */
public final class Materials {
    public static final Material WOOD = Material.builder("wood").thermal(700, 1700).ignites(300).build();
    public static final Material WOOL = Material.builder("wool").thermal(300, 1360).ignites(570).build();
    public static final Material LEAVES = Material.builder("leaves").thermal(150, 2000).ignites(280).estimated().build();
    public static final Material HAY = Material.builder("hay").thermal(150, 1500).ignites(250).estimated().build();
    public static final Material COAL_BLOCK = Material.builder("coal_block").thermal(1350, 1260).ignites(450).build();
    public static final Material WAX = Material.builder("wax").thermal(900, 2900).ignites(250).build();
    public static final Material TNT = Material.builder("tnt").thermal(1650, 1130).ignites(240).build();
    public static final Material WATER = Material.builder("water").thermal(1000, 4186)
            .boils(4186, 100, 2_257_000).build();
    public static final Material ICE = Material.builder("ice").thermal(917, 2090).melts(0, 334_000)
            .boils(4186, 100, 2_257_000).brittle(9e9, 5e-5, 1e6).build();
    public static final Material SNOW = Material.builder("snow").thermal(300, 2090).melts(0, 334_000)
            .boils(4186, 100, 2_257_000).build();
    public static final Material SAND = Material.builder("sand").thermal(1600, 830).melts(1713, 156_000).build();
    public static final Material STONE = Material.builder("stone").thermal(2700, 790).melts(1215, 400_000)
            .brittle(50e9, 8e-6, 10e6).build();
    public static final Material GLASS = Material.builder("glass").thermal(2500, 840).brittle(70e9, 9e-6, 30e6).build();
    public static final Material OBSIDIAN = Material.builder("obsidian").thermal(2400, 840)
            .brittle(70e9, 6e-6, 30e6).estimated().build();
    public static final Material LAVA = Material.builder("lava").thermal(2700, 1200).melts(1200, 400_000)
            .hot(1200).build();
    public static final Material MAGMA = Material.builder("magma").thermal(2700, 1000).hot(700).estimated().build();
    public static final Material FIRE = Material.builder("fire").hot(800).maxExtractable(20_000).estimated().build();
    public static final Material CAMPFIRE = Material.builder("campfire").hot(800).maxExtractable(200_000)
            .estimated().build();
    public static final Material AIR = Material.builder("air").thermal(PhysicsConstants.AIR_DENSITY, PhysicsConstants.AIR_CV)
            .build();
    public static final Material LIVING_CORE = Material.builder("living_core").thermal(1000, 3500).ignites(400)
            .estimated().build();
    /** Khối không có trong bảng khi Nâng tính khối lượng cả khối: ρ 1500 kg/m³ (ước lượng game, spec 8.9). */
    public static final Material DEFAULT_SOLID = Material.builder("default_solid").thermal(1500, 800).estimated()
            .build();

    private static final List<Material> ALL = List.of(WOOD, WOOL, LEAVES, HAY, COAL_BLOCK, WAX, TNT, WATER, ICE, SNOW,
            SAND, STONE, GLASS, OBSIDIAN, LAVA, MAGMA, FIRE, CAMPFIRE, AIR, LIVING_CORE, DEFAULT_SOLID);

    private Materials() {}

    public static List<Material> all() {
        return ALL;
    }
}
