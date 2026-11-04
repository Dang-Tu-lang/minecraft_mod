package phapthuat.spell.impl;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import phapthuat.Config;
import phapthuat.physics.ArcCalc;
import phapthuat.physics.ArcOutcome;
import phapthuat.physics.Element;
import phapthuat.physics.PhysicsConstants;
import phapthuat.spell.CastContext;
import phapthuat.spell.CastReport;
import phapthuat.spell.Fmt;
import phapthuat.spell.InscribedSpell;
import phapthuat.spell.Shape;
import phapthuat.spell.Spell;
import phapthuat.spell.SpellParam;
import phapthuat.spell.SpellUnit;
import phapthuat.thermal.WorldTemps;

/**
 * Hồ Quang (spec 8.4): điện trường E = U/d phải đạt ngưỡng đánh thủng 3 MV/m (trời mưa × 0,8). Đủ thì tia nối người
 * thi triển với mục tiêu, năng lượng phóng thành sát thương và nhiệt; không đủ nhưng đủ cho 0,5 m thì phóng ngược vào
 * chính người thi triển.
 */
public final class ArcSpell implements Spell {
    public static final String ID = "ho_quang";
    public static final SpellParam VOLTAGE = new SpellParam("voltage", "phapthuat.unit.kv", 1, 50_000, 1_000);
    public static final SpellParam ENERGY = new SpellParam("energy", "phapthuat.unit.mana", 0, 200, 5);
    /** Không có mục tiêu thì coi như khoảng cách tới trời, m (spec 8.4). */
    private static final double SKY_DISTANCE_M = 16;
    /** Bán kính điện lan trong nước, khối. */
    private static final double WATER_RADIUS = 4;
    /** Hạt tia hồ quang mỗi khối. */
    private static final int SPARKS_PER_BLOCK = 6;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Element element() {
        return Element.DIEN_TU;
    }

    @Override
    public Set<Shape> shapes() {
        return EnumSet.of(Shape.DIEM);
    }

    @Override
    public List<SpellParam> params() {
        return List.of(VOLTAGE, ENERGY);
    }

    @Override
    public double upfrontCostMana(InscribedSpell spell) {
        return spell.param(ENERGY) + ArcCalc.chargeEnergyJ(spell.param(VOLTAGE) * 1000) / Config.joulesPerMana();
    }

    /** Khối và sinh vật (spec 8.4). */
    @Override
    public boolean affects(ServerLevel level, SpellUnit unit, Shape shape) {
        return switch (unit) {
            case SpellUnit.BlockUnit b -> !level.getBlockState(b.pos()).isAir();
            case SpellUnit.AirUnit a -> true;
            case SpellUnit.EntityUnit e -> e.entity() instanceof LivingEntity living && living.isAlive();
        };
    }

    @Override
    public CastReport apply(CastContext ctx, List<SpellUnit> units) {
        double voltageV = ctx.spell().param(VOLTAGE) * 1000;
        double energyJ = ctx.spell().param(ENERGY) * ctx.joulesPerMana();
        double distance = distanceM(ctx, units);
        boolean raining = ctx.level().isRainingAt(ctx.caster().blockPosition());
        ArcOutcome outcome = ArcCalc.outcome(voltageV, distance, raining);
        double fieldMv = voltageV / distance / 1e6;
        double requiredMv = ArcCalc.requiredVoltage(distance, raining) / distance / 1e6;
        double damage = ArcCalc.damageHp(energyJ);

        if (outcome == ArcOutcome.FIZZLE) {
            shortArc(ctx.level(), ctx.caster());
            return new CastReport(false, Component.translatable("phapthuat.arc.fizzle", displayName(),
                    Fmt.num(fieldMv, 2), Fmt.num(requiredMv, 2)));
        }
        if (outcome == ArcOutcome.BACKFIRE_CASTER) {
            shortArc(ctx.level(), ctx.caster());
            ctx.caster().hurt(ctx.caster().damageSources().lightningBolt(), (float) damage);
            return new CastReport(false, Component.translatable("phapthuat.arc.backfire", displayName(),
                    Fmt.num(fieldMv, 2), Fmt.num(requiredMv, 2), Fmt.num(damage, 1)));
        }
        return strike(ctx, units, energyJ, damage, fieldMv, requiredMv);
    }

    /** d = khoảng cách mắt tới điểm trúng; nhìn vào khoảng không thì d = 16 m (spec 8.4). */
    private static double distanceM(CastContext ctx, List<SpellUnit> units) {
        boolean noTarget = units.isEmpty() || units.getFirst() instanceof SpellUnit.AirUnit;
        if (noTarget) return SKY_DISTANCE_M;
        return Math.max(ArcCalc.BACKFIRE_DISTANCE_M, ctx.caster().getEyePosition().distanceTo(ctx.center()));
    }

    private CastReport strike(CastContext ctx, List<SpellUnit> units, double energyJ, double damage, double fieldMv,
            double requiredMv) {
        ServerLevel level = ctx.level();
        ServerPlayer caster = ctx.caster();
        boolean wet = caster.isInWater() || units.stream().anyMatch(u -> wet(level, u));
        if (wet) return spreadInWater(ctx, energyJ, fieldMv, requiredMv);

        sparks(level, caster.getEyePosition(), ctx.center());
        Component line = null;
        for (SpellUnit unit : units) {
            switch (unit) {
                case SpellUnit.EntityUnit e when e.entity() instanceof LivingEntity living -> {
                    // Hiệu ứng sét trước sát thương: mục tiêu vẫn biến đổi như cú sét thật kể cả khi cú đánh làm nó chết.
                    if (ArcCalc.lightningEffects(energyJ, ctx.joulesPerMana())) lightning(living, level);
                    living.hurt(caster.damageSources().lightningBolt(), (float) damage);
                    Component heat = HeatSpell.applyHeat(ctx, List.of(unit), energyJ, displayName()).message();
                    line = Component.translatable("phapthuat.arc.hit", displayName(), Fmt.num(fieldMv, 2),
                            Fmt.num(requiredMv, 2), living.getDisplayName(), Fmt.num(damage, 1))
                            .append(Component.literal(" — ")).append(heat);
                }
                case SpellUnit.BlockUnit b -> {
                    Component heat = HeatSpell.applyHeat(ctx, List.of(unit), energyJ, displayName()).message();
                    line = Component.translatable("phapthuat.arc.hit_block", displayName(), Fmt.num(fieldMv, 2),
                            Fmt.num(requiredMv, 2), level.getBlockState(b.pos()).getBlock().getName())
                            .append(Component.literal(" — ")).append(heat);
                }
                default -> {}
            }
        }
        if (line == null) {
            return new CastReport(true, Component.translatable("phapthuat.arc.air", displayName(),
                    Fmt.num(fieldMv, 2), Fmt.num(requiredMv, 2)));
        }
        return new CastReport(true, line);
    }

    /** Điện lan trong nước: mọi sinh vật trong nước quanh mục tiêu (kể cả người chơi) chia nhau sát thương. */
    private CastReport spreadInWater(CastContext ctx, double energyJ, double fieldMv, double requiredMv) {
        ServerLevel level = ctx.level();
        Vec3 center = ctx.center();
        List<LivingEntity> wet = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(WATER_RADIUS), e -> e.isAlive() && e.isInWater());
        ServerPlayer caster = ctx.caster();
        if (caster.isInWater() && !wet.contains(caster)) wet.add(caster);
        if (wet.isEmpty()) {
            return new CastReport(false, Component.translatable("phapthuat.arc.water_none", displayName(),
                    Fmt.num(fieldMv, 2), Fmt.num(requiredMv, 2)));
        }
        double damage = ArcCalc.damageHp(energyJ) / wet.size();
        for (LivingEntity e : wet) {
            e.hurt(caster.damageSources().lightningBolt(), (float) damage);
            sparks(level, center, e.getBoundingBox().getCenter());
        }
        return new CastReport(true, Component.translatable("phapthuat.arc.water", displayName(),
                Fmt.num(fieldMv, 2), Fmt.num(requiredMv, 2), wet.size(), Fmt.num(damage, 1)));
    }

    private static boolean wet(ServerLevel level, SpellUnit unit) {
        return switch (unit) {
            case SpellUnit.EntityUnit e -> e.entity().isInWater();
            case SpellUnit.BlockUnit b -> WorldTemps.inWater(level, b.pos());
            case SpellUnit.AirUnit a -> level.getFluidState(a.pos()).isEmpty()
                    ? WorldTemps.inWater(level, a.pos()) : true;
        };
    }

    /** Hiệu ứng sét của vanilla: creeper tích điện, lợn thành piglin xác sống, dân làng thành phù thủy. */
    private static void lightning(LivingEntity entity, ServerLevel level) {
        LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
        bolt.setPos(entity.getX(), entity.getY(), entity.getZ());
        bolt.setVisualOnly(true);
        entity.thunderHit(level, bolt);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, entity.getX(), entity.getY() + entity.getBbHeight() / 2,
                entity.getZ(), 24, 0.6, 0.6, 0.6, 0.2);
    }

    /** Tia ngắn ở tay người thi triển khi phóng hụt hoặc phóng ngược. */
    private static void shortArc(ServerLevel level, ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        sparks(level, eye, eye.add(player.getViewVector(1.0f).scale(1.0)));
    }

    private static void sparks(ServerLevel level, Vec3 from, Vec3 to) {
        int count = (int) Math.max(2, from.distanceTo(to) * SPARKS_PER_BLOCK);
        for (int i = 0; i < count; i++) {
            Vec3 p = from.lerp(to, (i + 1.0) / (count + 1.0));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.01);
        }
    }

    /** Dùng cho sách: ngưỡng đánh thủng quy ra MV/m. */
    public static double breakdownMvPerM() {
        return PhysicsConstants.BREAKDOWN_V_PER_M / 1e6;
    }

    /** Khoảng cách tới trời khi không có mục tiêu, m. */
    public static double skyDistance() {
        return SKY_DISTANCE_M;
    }
}
