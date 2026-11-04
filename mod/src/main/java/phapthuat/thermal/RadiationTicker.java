package phapthuat.thermal;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import phapthuat.physics.DoseTier;

/**
 * Hiệu ứng theo bậc liều phóng xạ (spec 8.8): buồn nôn, yếu ớt, độc, khô héo. Dùng chung cho {@link ThermalTicker}
 * (mỗi giây) và phép Bức Xạ Neutron (áp ngay lúc thi triển, trước khi liều kịp giảm).
 */
public final class RadiationTicker {
    /** Thời lượng hiệu ứng, tick: đủ dài để lần áp sau gia hạn trước khi hết. */
    private static final int EFFECT_TICKS = 40;

    private RadiationTicker() {}

    public static void applyDose(LivingEntity entity, DoseTier tier) {
        if (tier == DoseTier.NONE) return;
        entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, EFFECT_TICKS, 0, false, false));
        switch (tier) {
            case NAUSEA -> entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, EFFECT_TICKS, 0, false, false));
            case POISON -> {
                entity.addEffect(new MobEffectInstance(MobEffects.POISON, EFFECT_TICKS, 0, false, false));
                entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, EFFECT_TICKS, 0, false, false));
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EFFECT_TICKS, 0, false, false));
            }
            case WITHER, SEVERE -> entity.addEffect(
                    new MobEffectInstance(MobEffects.WITHER, EFFECT_TICKS, 0, false, false));
            default -> {}
        }
    }
}
