package phapthuat.thermal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import phapthuat.physics.ColdTier;
import phapthuat.physics.SiphonCalc;

/** Dọn bảng nhiệt và trạng thái sinh vật đã về bình thường. Hiệu ứng theo bậc (lạnh, phóng xạ) cũng chạy ở đây. */
public final class ThermalTicker {
    private ThermalTicker() {}

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % 200 == 0) {
            ThermalStore.get(level).sweep(level.getGameTime());
        }
    }

    /** Khối mới đặt vào một ô không thừa hưởng nhiệt đã lưu của khối cũ ở đó (spec 6.8), kể cả khi cùng vật liệu. */
    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level) ThermalStore.get(level).remove(event.getPos());
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity) || entity.level().isClientSide()) return;
        if (entity.tickCount % 20 != 0 || !entity.hasData(ThermalAttachments.ENTITY_THERMAL)) return;
        EntityThermal now = WorldTemps.currentEntity(entity);
        double natural = WorldTemps.naturalEntityC(entity);
        if (now.settled(natural)) {
            entity.removeData(ThermalAttachments.ENTITY_THERMAL);
            return;
        }
        entity.setData(ThermalAttachments.ENTITY_THERMAL, now);
        applyCold(entity, SiphonCalc.coldTier(natural - now.coreTempC()));
    }

    /** Lạnh theo bậc (spec 8.2): chậm chạp; đóng băng như tuyết bột + 1 máu/giây; mất 3 máu/giây. */
    private static void applyCold(LivingEntity entity, ColdTier tier) {
        if (tier == ColdTier.NONE || !entity.canFreeze()) return;
        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, tier.ordinal() - 1, false, false));
        if (tier == ColdTier.SLOW) return;
        entity.setTicksFrozen(entity.getTicksRequiredToFreeze());
        entity.hurt(entity.damageSources().freeze(), tier == ColdTier.SEVERE ? 3 : 1);
    }
}
