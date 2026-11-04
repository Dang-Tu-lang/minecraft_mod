package phapthuat.spell.impl;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import phapthuat.physics.DoseTier;
import phapthuat.physics.Element;
import phapthuat.physics.EntityMass;
import phapthuat.physics.RadiationCalc;
import phapthuat.spell.CastContext;
import phapthuat.spell.CastReport;
import phapthuat.spell.Fmt;
import phapthuat.spell.InscribedSpell;
import phapthuat.spell.Shape;
import phapthuat.spell.Spell;
import phapthuat.spell.SpellParam;
import phapthuat.spell.SpellUnit;
import phapthuat.thermal.EntityThermal;
import phapthuat.thermal.WorldTemps;

/**
 * Bức Xạ Neutron (spec 8.8): năng lượng phát ra chuyển thành neutron với hiệu suất 2×10⁻⁴, đi qua khối rắn và nước
 * thì giảm, rồi cộng dồn thành liều (Gy) trên từng sinh vật. Hiệu ứng theo bậc liều do {@code ThermalTicker} áp.
 */
public final class RadiationSpell implements Spell {
    public static final String ID = "buc_xa_neutron";
    public static final SpellParam ENERGY = new SpellParam("energy", "phapthuat.unit.mana", 0, 1000, 10);
    /** Bán kính tán xạ quanh điểm tác động, khối (spec 8.8). */
    private static final double SCATTER_RADIUS = 3;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Element element() {
        return Element.HAT_NHAN;
    }

    @Override
    public Set<Shape> shapes() {
        return EnumSet.of(Shape.DIEM, Shape.TIA, Shape.CAU);
    }

    @Override
    public List<SpellParam> params() {
        return List.of(ENERGY);
    }

    @Override
    public double upfrontCostMana(InscribedSpell spell) {
        return spell.param(ENERGY);
    }

    /** Chỉ sinh vật (spec 8.8). */
    @Override
    public boolean affects(ServerLevel level, SpellUnit unit, Shape shape) {
        return unit instanceof SpellUnit.EntityUnit e && e.entity() instanceof LivingEntity living && living.isAlive();
    }

    @Override
    public CastReport apply(CastContext ctx, List<SpellUnit> units) {
        ServerLevel level = ctx.level();
        Vec3 eye = ctx.caster().getEyePosition();
        double totalJ = RadiationCalc.absorbedJ(ctx.spell().param(ENERGY), ctx.joulesPerMana());
        double perUnit = units.isEmpty() ? 0 : totalJ / units.size();
        List<Component> lines = new ArrayList<>();
        double strongestAbsorbed = 0;
        Vec3 center = ctx.center();
        Set<LivingEntity> affected = new HashSet<>();
        for (SpellUnit unit : units) {
            if (!(unit instanceof SpellUnit.EntityUnit eu) || !(eu.entity() instanceof LivingEntity living)) continue;
            double[] shield = shield(level, eye, living.getBoundingBox().getCenter());
            double absorbed = perUnit * RadiationCalc.attenuation((int) shield[0], (int) shield[1]);
            double mass = ImpulseSpell.massKg(living);
            double dose = RadiationCalc.doseGy(absorbed, mass);
            addDose(living, dose);
            affected.add(living);
            strongestAbsorbed = Math.max(strongestAbsorbed, absorbed);
            particles(level, eye, living.getBoundingBox().getCenter());
            if (lines.size() < 4) {
                EntityThermal now = WorldTemps.currentEntity(living);
                lines.add(Component.translatable("phapthuat.radiation.unit", living.getDisplayName(),
                        Fmt.num(mass, 0), Fmt.num(dose, 2), Fmt.num(now.doseGy(), 2),
                        Component.translatable("phapthuat.dose." + RadiationCalc.tier(now.doseGy()).name()
                                .toLowerCase(java.util.Locale.ROOT))));
            }
        }
        if (lines.isEmpty()) {
            return new CastReport(false, Component.translatable("phapthuat.radiation.nothing", displayName()));
        }
        scatter(level, center, strongestAbsorbed, affected);
        return new CastReport(true, Component.translatable("phapthuat.radiation.done", displayName(),
                Component.literal(String.join("; ", lines.stream().map(Component::getString).toList()))));
    }

    /** Cộng dồn liều vào trạng thái đã lưu của sinh vật (spec 6.8). */
    private static void addDose(LivingEntity living, double gy) {
        EntityThermal now = WorldTemps.currentEntity(living);
        WorldTemps.saveEntity(living, now.coreTempC(), now.doseGy() + gy);
    }

    /** Tán xạ: sinh vật khác trong 3 khối quanh điểm tác động (kể cả người chơi) nhận 5% năng lượng hấp thụ. */
    private static void scatter(ServerLevel level, Vec3 center, double absorbedJ, Set<LivingEntity> skip) {
        if (absorbedJ <= 0) return;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(SCATTER_RADIUS), LivingEntity::isAlive)) {
            if (skip.contains(e)) continue;
            double dose = RadiationCalc.scatterDoseGy(absorbedJ, ImpulseSpell.massKg(e));
            if (dose > 0) addDose(e, dose);
        }
    }

    /** Số khối rắn và số khối nước trên đoạn thẳng mắt → mục tiêu (spec 8.8). */
    private static double[] shield(ServerLevel level, Vec3 eye, Vec3 target) {
        BlockHitResult hit = level.clip(new ClipContext(eye, target, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, (net.minecraft.world.entity.Entity) null));
        Vec3 stop = hit.getType() == HitResult.Type.MISS ? target : hit.getLocation();
        double solid = 0;
        double water = 0;
        for (BlockPos pos : cellsBetween(eye, stop)) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) {
                if (level.getFluidState(pos).is(FluidTags.WATER)) {
                    water++;
                } else {
                    solid++;
                }
            } else if (level.getFluidState(pos).is(FluidTags.WATER)) {
                water++;
            }
        }
        return new double[] {solid, water};
    }

    private static List<BlockPos> cellsBetween(Vec3 from, Vec3 to) {
        List<BlockPos> cells = new ArrayList<>();
        double distance = from.distanceTo(to);
        int steps = (int) Math.ceil(distance * 4);
        BlockPos last = null;
        for (int i = 0; i <= steps; i++) {
            BlockPos pos = BlockPos.containing(from.lerp(to, (double) i / Math.max(1, steps)));
            if (!pos.equals(last)) {
                cells.add(pos);
                last = pos;
            }
        }
        return cells;
    }

    private static void particles(ServerLevel level, Vec3 from, Vec3 to) {
        int count = (int) Math.max(4, from.distanceTo(to) * 3);
        for (int i = 0; i < count; i++) {
            Vec3 p = from.lerp(to, (i + 1.0) / (count + 1.0));
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0);
        }
    }

    /** Bậc liều hiện tại của sinh vật — dùng cho sách và Máy Đo. */
    public static DoseTier tierOf(LivingEntity living) {
        return RadiationCalc.tier(WorldTemps.currentEntity(living).doseGy());
    }
}
