package phapthuat.spell.impl;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import org.joml.Vector3f;
import phapthuat.physics.Element;
import phapthuat.physics.HeatTier;
import phapthuat.physics.Material;
import phapthuat.physics.Materials;
import phapthuat.physics.OpticsCalc;
import phapthuat.physics.PhysicsConstants;
import phapthuat.spell.CastContext;
import phapthuat.spell.CastReport;
import phapthuat.spell.Fmt;
import phapthuat.spell.InscribedSpell;
import phapthuat.spell.Shape;
import phapthuat.spell.Spell;
import phapthuat.spell.SpellParam;
import phapthuat.spell.SpellUnit;
import phapthuat.spell.TimedEffects;
import phapthuat.thermal.MaterialLookup;
import phapthuat.thermal.PlacedLights;

/**
 * Phát Quang (spec 8.6): bước sóng quyết định ánh sáng là gì. Điểm đặt nguồn sáng theo quang thông; Tia là laser bán
 * kính 1 cm. Mỗi giây các ngưỡng cường độ được áp cho mọi sinh vật quanh nguồn — kể cả người thi triển.
 */
public final class LightSpell implements Spell {
    public static final String ID = "phat_quang";
    public static final SpellParam WAVELENGTH = new SpellParam("wavelength", "phapthuat.unit.nm", 100, 2000, 555);
    public static final SpellParam POWER = new SpellParam("power", "phapthuat.unit.watt", 0.01, 100_000, 1);
    public static final SpellParam DURATION = new SpellParam("duration", "phapthuat.unit.s", 1, 600, 60);
    /** Bán kính chịu ngưỡng cường độ quanh quả cầu, khối. */
    private static final double EFFECT_RADIUS = 8;
    /** Laser dài tối đa, khối. */
    private static final double LASER_RANGE = 16;
    private static final int TICKS_PER_SECOND = 20;
    private static final int BLIND_TICKS = 60;
    private static final int BURN_SECONDS = 3;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Element element() {
        return Element.QUANG;
    }

    @Override
    public Set<Shape> shapes() {
        return EnumSet.of(Shape.DIEM, Shape.TIA);
    }

    @Override
    public List<SpellParam> params() {
        return List.of(WAVELENGTH, POWER, DURATION);
    }

    @Override
    public double upfrontCostMana(InscribedSpell spell) {
        return spell.param(POWER) * spell.param(DURATION) / phapthuat.Config.joulesPerMana();
    }

    /** Không khí và khối rắn (laser chiếu vào khối thì khối đó chịu ngưỡng). */
    @Override
    public boolean affects(ServerLevel level, SpellUnit unit, Shape shape) {
        return switch (unit) {
            case SpellUnit.BlockUnit b -> !level.getBlockState(b.pos()).isAir();
            case SpellUnit.AirUnit a -> shape == Shape.TIA;
            case SpellUnit.EntityUnit e -> e.entity() instanceof LivingEntity living && living.isAlive();
        };
    }

    @Override
    public CastReport apply(CastContext ctx, List<SpellUnit> units) {
        double nm = ctx.spell().param(WAVELENGTH);
        double watts = ctx.spell().param(POWER);
        int ticks = (int) Math.round(ctx.spell().param(DURATION) * TICKS_PER_SECOND);
        boolean visible = OpticsCalc.isVisible(nm);
        double lumens = OpticsCalc.lumens(nm, watts);
        Component lamp = describe(nm, watts, lumens);

        if (ctx.spell().shape() == Shape.TIA) {
            laser(ctx, units, nm, watts, ticks, visible, lamp);
        } else {
            point(ctx, nm, watts, ticks, visible, lumens, lamp);
        }
        return new CastReport(true, lamp);
    }

    /** Quả cầu ở ô không khí trước mặt khối đang nhìn; chỉ ánh sáng nhìn thấy mới đặt khối sáng. */
    private static void point(CastContext ctx, double nm, double watts, int ticks, boolean visible, double lumens,
            Component lamp) {
        ServerLevel level = ctx.level();
        Vec3 center = ctx.center();
        BlockPos cell = ctx.airCell();
        boolean lit = visible && OpticsCalc.lightLevel(lumens) > 0 && level.getBlockState(cell).isAir();
        if (lit) {
            level.setBlock(cell, Blocks.LIGHT.defaultBlockState().setValue(
                    net.minecraft.world.level.block.LightBlock.LEVEL, OpticsCalc.lightLevel(lumens)),
                    Block.UPDATE_ALL_IMMEDIATE);
            PlacedLights.get(level).add(cell, level.getGameTime() + ticks);
        }
        Vector3f color = color(nm);
        TimedEffects.add(level, (lvl, age) -> {
            DustParticleOptions dust = new DustParticleOptions(color, 1.0f);
            lvl.sendParticles(dust, center.x, center.y, center.z, 3, 0.25, 0.25, 0.25, 0);
            if (age % TICKS_PER_SECOND == 0) pointDamage(lvl, center, nm, watts);
            return age < ticks - 1;
        });
        if (lit) {
            ctx.caster().displayClientMessage(lamp, false);
        }
    }

    /** Laser theo hướng lúc thi triển, dừng ở khối rắn đầu tiên, mỗi giây áp ngưỡng cường độ theo chùm. */
    private static void laser(CastContext ctx, List<SpellUnit> units, double nm, double watts, int ticks,
            boolean visible, Component lamp) {
        ServerLevel level = ctx.level();
        Vec3 eye = ctx.caster().getEyePosition();
        Vec3 dir = ctx.caster().getViewVector(1.0f).normalize();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(dir.scale(LASER_RANGE)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, ctx.caster()));
        Vec3 end = hit.getType() == HitResult.Type.MISS ? eye.add(dir.scale(LASER_RANGE)) : hit.getLocation();
        double intensity = OpticsCalc.laserIntensity(watts);
        Vector3f color = color(nm);
        TimedEffects.add(level, (lvl, age) -> {
            dustLine(lvl, eye, end, color);
            if (age % TICKS_PER_SECOND == 0) laserDamage(lvl, eye, end, nm, watts, visible);
            return age < ticks - 1;
        });
        // Bản thân tia cũng vẽ ngay một lần để người chơi thấy hướng.
        dustLine(level, eye, end, color);
    }

    private static void laserDamage(ServerLevel level, Vec3 start, Vec3 end, double nm, double watts,
            boolean visible) {
        AABB box = new AABB(start, end).inflate(1);
        double intensity = OpticsCalc.laserIntensity(watts);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (e.getBoundingBox().inflate(0.3).clip(start, end).isEmpty()) continue;
            thresholds(level, e, nm, intensity);
            if (visible && watts >= 1) {
                if (e instanceof ServerPlayer player) {
                    player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLIND_TICKS, 0, false, false));
                } else if (e instanceof Mob mob) {
                    mob.setTarget(null);
                }
            }
        }
    }

    private static void pointDamage(ServerLevel level, Vec3 center, double nm, double watts) {
        AABB box = new AABB(center, center).inflate(EFFECT_RADIUS + 1);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            double d = e.getBoundingBox().getCenter().distanceTo(center);
            if (d > EFFECT_RADIUS) continue;
            double intensity = OpticsCalc.intensity(watts, d);
            thresholds(level, e, nm, intensity);
            // UV-C mạnh tại chỗ người chơi: bỏng giác mạc, mù tạm thời (spec 8.6).
            if (e instanceof ServerPlayer player && OpticsCalc.isUvc(nm) && intensity >= PhysicsConstants.UVC_BLIND_W_M2
                    && player.getBoundingBox().getCenter().distanceTo(center) <= EFFECT_RADIUS) {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLIND_TICKS, 0, false, false));
            }
        }
    }

    /** Các ngưỡng cường độ dùng chung cho nguồn điểm và laser (spec 8.6). */
    private static void thresholds(ServerLevel level, LivingEntity e, double nm, double intensity) {
        if (OpticsCalc.isUv(nm) && intensity >= PhysicsConstants.UV_BURN_W_M2 && phapthuat.thermal.WorldTemps
                .isUndead(e)) {
            e.setRemainingFireTicks(Math.max(e.getRemainingFireTicks(), BURN_SECONDS * 20));
        }
        if (OpticsCalc.isUvc(nm) && intensity >= PhysicsConstants.UV_BURN_W_M2) {
            e.hurt(level.damageSources().magic(), 1);
        }
        if (OpticsCalc.isInfrared(nm)) {
            if (intensity >= PhysicsConstants.IGNITE_W_M2 && !e.fireImmune() && !e.isInWater()) {
                e.setRemainingFireTicks(Math.max(e.getRemainingFireTicks(), BURN_SECONDS * 20));
            } else if (intensity >= PhysicsConstants.PAIN_W_M2) {
                e.hurt(level.damageSources().inFire(), 1);
            }
            if (intensity >= PhysicsConstants.IGNITE_W_M2) igniteNear(level, e);
        }
    }

    /** Khối dễ cháy quanh sinh vật bắt lửa khi hồng ngoại đủ mạnh. */
    private static void igniteNear(ServerLevel level, LivingEntity e) {
        BlockPos origin = e.blockPosition();
        for (Direction d : Direction.values()) {
            BlockPos fire = origin.relative(d);
            if (!level.getBlockState(fire).isAir()) continue;
            boolean flammable = MaterialLookup.of(level.getBlockState(origin.relative(d)))
                    .map(Material::flammable).orElse(false);
            if (level.getBlockState(fire).isAir() && hasFlammableBeside(level, fire)) {
                level.setBlock(fire, BaseFireBlock.getState(level, fire), Block.UPDATE_ALL_IMMEDIATE);
                return;
            }
        }
    }

    private static boolean hasFlammableBeside(ServerLevel level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            if (MaterialLookup.of(level.getBlockState(pos.relative(d))).map(Material::flammable).orElse(false)) {
                return true;
            }
        }
        return false;
    }

    private static void dustLine(ServerLevel level, Vec3 from, Vec3 to, Vector3f color) {
        DustParticleOptions dust = new DustParticleOptions(color, 1.0f);
        int count = (int) Math.max(4, from.distanceTo(to) * 4);
        for (int i = 0; i < count; i++) {
            Vec3 p = from.lerp(to, (i + 1.0) / (count + 1.0));
            level.sendParticles(dust, p.x, p.y, p.z, 1, 0.01, 0.01, 0.01, 0);
        }
    }

    /** Màu hạt theo bước sóng: quang phổ đơn giản, ngoài vùng nhìn thấy dùng tím hoặc đỏ thẫm. */
    private static Vector3f color(double nm) {
        if (nm < 380) return new Vector3f(0.4f, 0.1f, 0.6f);
        if (nm > 750) return new Vector3f(0.5f, 0.05f, 0.05f);
        double t = (nm - 380) / (750 - 380);
        float r = (float) Math.max(0, Math.min(1, 1.5 - Math.abs(4 * t - 3)));
        float g = (float) Math.max(0, Math.min(1, 1.5 - Math.abs(4 * t - 2)));
        float b = (float) Math.max(0, Math.min(1, 1.5 - Math.abs(4 * t - 1)));
        return new Vector3f(r, g, b);
    }

    private static Component describe(double nm, double watts, double lumens) {
        String band = OpticsCalc.isVisible(nm) ? "phapthuat.light.visible"
                : OpticsCalc.isUv(nm) ? "phapthuat.light.uv" : "phapthuat.light.infrared";
        return Component.translatable("phapthuat.light.done", Component.translatable("phapthuat.spell.phat_quang.name"),
                Fmt.num(nm, 0), Fmt.num(watts, 2), Component.translatable(band), Fmt.num(lumens, 1),
                OpticsCalc.lightLevel(lumens));
    }
}
