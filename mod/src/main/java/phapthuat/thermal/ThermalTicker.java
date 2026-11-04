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
import phapthuat.physics.DoseTier;
import phapthuat.physics.RadiationCalc;
import phapthuat.physics.SiphonCalc;

/** Dọn bảng nhiệt và trạng thái sinh vật đã về bình thường. Hiệu ứng theo bậc (lạnh, phóng xạ) cũng chạy ở đây. */
public final class ThermalTicker {
    private ThermalTicker() {}

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (level.getGameTime() % 200 == 0) ThermalStore.get(level).sweep(level.getGameTime());
        if (level.getGameTime() % 20 == 0) PlacedLights.get(level).sweep(level);
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
        DoseTier tier = RadiationCalc.tier(now.doseGy());
        boolean severe = now.severeApplied();
        if (tier == DoseTier.SEVERE && !severe) {
            severe = true;
            entity.hurt(entity.damageSources().generic(), entity.getMaxHealth() / 2);
        }
        applyDose(entity, tier);
        applyCold(entity, SiphonCalc.coldTier(natural - now.coreTempC()));
        entity.setData(ThermalAttachments.ENTITY_THERMAL,
                new EntityThermal(now.coreTempC(), now.doseGy(), now.updatedAtTick(), severe));
    }

    /** Hiệu ứng theo bậc liều phóng xạ (spec 8.8): làm mới mỗi giây, tự tắt khi liều giảm khỏi bậc. */
    private static void applyDose(LivingEntity entity, DoseTier tier) {
        if (tier == DoseTier.NONE) return;
        entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 40, 0, false, false));
        switch (tier) {
            case NAUSEA -> entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false));
            case POISON -> {
                entity.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0, false, false));
                entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false));
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false));
            }
            case WITHER, SEVERE -> entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0, false, false));
            default -> {}
        }
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
