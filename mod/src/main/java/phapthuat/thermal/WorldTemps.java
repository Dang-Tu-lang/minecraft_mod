package phapthuat.thermal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import phapthuat.Config;
import phapthuat.physics.Material;
import phapthuat.physics.Materials;

/** Nhiệt độ môi trường, nhiệt độ tự nhiên và nhiệt độ hiện tại của khối, sinh vật (spec mục 6.4, 6.8). */
public final class WorldTemps {
    /** Thân nhiệt của sinh vật sống. */
    public static final double LIVING_BODY_C = 37;

    private WorldTemps() {}

    /** T_mt (°C) = 25 × nhiệt độ biome tại vị trí đó (có tính độ cao). */
    @SuppressWarnings("deprecation")
    public static double ambientC(Level level, BlockPos pos) {
        return 25.0 * level.getBiome(pos).value().getTemperature(pos);
    }

    public static double naturalC(Level level, BlockPos pos, Material m) {
        if (!Double.isNaN(m.naturalTempC())) return m.naturalTempC();
        double ambient = ambientC(level, pos);
        if (m == Materials.ICE || m == Materials.SNOW) return Math.min(ambient, 0);
        return ambient;
    }

    public static double halfLifeSeconds(Material m) {
        return Double.isNaN(m.naturalTempC()) ? Config.HALF_LIFE_SECONDS.get() : Config.HOT_SOURCE_HALF_LIFE_SECONDS.get();
    }

    /** Trạng thái hiện tại của khối: giá trị đã lưu (đã tính nguội tới bây giờ) hoặc trạng thái tự nhiên. */
    public static BlockThermal currentBlock(ServerLevel level, BlockPos pos, Material m) {
        ThermalStore store = ThermalStore.get(level);
        var saved = store.get(pos, level.getGameTime());
        if (saved.isPresent()) {
            if (saved.get().materialId().equals(m.id())) return saved.get();
            store.remove(pos);
        }
        double natural = naturalC(level, pos, m);
        return new BlockThermal(m.id(), natural, 0, natural, halfLifeSeconds(m));
    }

    public static void saveBlock(ServerLevel level, BlockPos pos, BlockThermal value) {
        ThermalStore.get(level).put(pos, value, level.getGameTime());
    }

    /** Luật oxy (spec 8.1): khối ngậm nước, hoặc có nước ở một trong 6 ô kề, là "trong nước". Máy Đo đo đúng luật này. */
    public static boolean inWater(Level level, BlockPos pos) {
        if (level.getFluidState(pos).is(FluidTags.WATER)) return true;
        for (Direction d : Direction.values()) {
            if (level.getFluidState(pos.relative(d)).is(FluidTags.WATER)) return true;
        }
        return false;
    }

    public static boolean isUndead(Entity entity) {
        return entity.getType().is(EntityTypeTags.UNDEAD);
    }

    public static double naturalEntityC(LivingEntity entity) {
        return isUndead(entity) ? ambientC(entity.level(), entity.blockPosition()) : LIVING_BODY_C;
    }

    /** Trạng thái hiện tại của sinh vật, đã tính nguội và giảm liều tới bây giờ. */
    public static EntityThermal currentEntity(LivingEntity entity) {
        long now = entity.level().getGameTime();
        double natural = naturalEntityC(entity);
        if (!entity.hasData(ThermalAttachments.ENTITY_THERMAL)) return new EntityThermal(natural, 0, now);
        EntityThermal saved = entity.getData(ThermalAttachments.ENTITY_THERMAL);
        if (Double.isNaN(saved.coreTempC())) {
            saved = new EntityThermal(natural, saved.doseGy(), saved.updatedAtTick(), saved.severeApplied());
        }
        return saved.decayedTo(now, natural, Config.HALF_LIFE_SECONDS.get(), Config.DOSE_HALF_LIFE_SECONDS.get());
    }

    public static void saveEntity(LivingEntity entity, double coreTempC, double doseGy) {
        boolean severe = entity.hasData(ThermalAttachments.ENTITY_THERMAL)
                && entity.getData(ThermalAttachments.ENTITY_THERMAL).severeApplied();
        saveEntity(entity, coreTempC, doseGy, severe);
    }

    public static void saveEntity(LivingEntity entity, double coreTempC, double doseGy, boolean severeApplied) {
        entity.setData(ThermalAttachments.ENTITY_THERMAL,
                new EntityThermal(coreTempC, doseGy, entity.level().getGameTime(), severeApplied));
    }
}
