package phapthuat.spell.impl;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import phapthuat.physics.Element;
import phapthuat.physics.LiftCalc;
import phapthuat.physics.Material;
import phapthuat.physics.Materials;
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

/**
 * Nâng (spec 8.9): thế năng m·g·h. Sinh vật được nâng rồi thả; khối biến thành khối rơi, rơi trúng ai thì gây sát thương
 * m·g·h_rơi / 1 kJ. Không nâng được đá nền, khối không phá được, khối có dữ liệu riêng (rương, lò).
 */
public final class LiftSpell implements Spell {
    public static final String ID = "nang";
    public static final SpellParam HEIGHT = new SpellParam("lift_height", "phapthuat.unit.m", 0.5, 30, 3);
    public static final SpellParam DURATION = new SpellParam("duration", "phapthuat.unit.s", 1, 10, 3);
    /** Thời gian đưa mục tiêu lên tới độ cao mong muốn, giây (spec 8.9). */
    private static final double RISE_SECONDS = 1;
    private static final int TICKS_PER_SECOND = 20;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Element element() {
        return Element.HAP_DAN;
    }

    @Override
    public Set<Shape> shapes() {
        return EnumSet.of(Shape.DIEM, Shape.CAU, Shape.BAN_THAN);
    }

    @Override
    public List<SpellParam> params() {
        return List.of(HEIGHT, DURATION);
    }

    /** Chi phí phụ thuộc mục tiêu: Σ m·g·h (spec 8.9), tính trước khi trừ mana. */
    @Override
    public double upfrontCostMana(InscribedSpell spell, ServerLevel level, List<SpellUnit> units) {
        double height = spell.param(HEIGHT);
        double total = 0;
        for (SpellUnit unit : units) total += LiftCalc.energy(massKg(level, unit), height);
        return total / phapthuat.Config.joulesPerMana();
    }

    @Override
    public double upfrontCostMana(InscribedSpell spell) {
        return 0;
    }

    /** Sinh vật và khối rắn nâng được (spec 8.9). */
    @Override
    public boolean affects(ServerLevel level, SpellUnit unit, Shape shape) {
        return switch (unit) {
            case SpellUnit.EntityUnit e -> e.entity() instanceof LivingEntity living && living.isAlive();
            case SpellUnit.BlockUnit b -> liftable(level, b.pos());
            case SpellUnit.AirUnit a -> false;
        };
    }

    /** Khối nâng được: rắn, cứng ≥ 0, không phải đá nền, không có block entity (rương, lò, ...). */
    private static boolean liftable(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;
        if (state.is(Blocks.BEDROCK)) return false;
        if (state.getDestroySpeed(level, pos) < 0) return false;
        BlockEntity be = level.getBlockEntity(pos);
        return be == null;
    }

    @Override
    public CastReport apply(CastContext ctx, List<SpellUnit> units) {
        if (units.isEmpty()) {
            return new CastReport(false, Component.translatable("phapthuat.lift.nothing", displayName()));
        }
        double height = ctx.spell().param(HEIGHT);
        int holdTicks = (int) Math.round(ctx.spell().param(DURATION) * TICKS_PER_SECOND);
        List<String> lines = new ArrayList<>();
        double totalMana = 0;
        for (SpellUnit unit : units) {
            double mass = massKg(ctx.level(), unit);
            totalMana += LiftCalc.energy(mass, height) / ctx.joulesPerMana();
            Component name;
            switch (unit) {
                case SpellUnit.EntityUnit eu -> {
                    if (!(eu.entity() instanceof LivingEntity living)) continue;
                    liftEntity(ctx.level(), living, height, holdTicks);
                    name = living.getDisplayName();
                }
                case SpellUnit.BlockUnit bu -> {
                    name = liftBlock(ctx.level(), bu.pos(), height, holdTicks);
                    if (name == null) continue;
                }
                default -> {
                    continue;
                }
            }
            particles(ctx.level(), ctx.center());
            lines.add(Component.translatable("phapthuat.lift.unit", name, Fmt.num(mass, 0), Fmt.num(height, 1))
                    .getString());
        }
        if (lines.isEmpty()) {
            return new CastReport(false, Component.translatable("phapthuat.lift.nothing", displayName()));
        }
        return new CastReport(true, Component.translatable("phapthuat.lift.done", displayName(),
                Fmt.num(height, 1), Component.literal(String.join("; ", lines)),
                Fmt.num(totalMana, 1)));
    }

    /** Sinh vật: tắt trọng lực, đưa lên đều trong 1 giây, giữ tới hết thời gian rồi thả (rơi theo vanilla). */
    private static void liftEntity(ServerLevel level, LivingEntity living, double height, int holdTicks) {
        living.setNoGravity(true);
        double startY = living.getY();
        double targetY = startY + height;
        int riseTicks = (int) Math.max(1, RISE_SECONDS * TICKS_PER_SECOND);
        TimedEffects.add(level, (lvl, age) -> {
            if (!living.isAlive()) return false;
            if (age <= riseTicks) {
                double t = (double) age / riseTicks;
                living.teleportTo(living.getX(), startY + height * t, living.getZ());
            } else if (age <= riseTicks + holdTicks) {
                living.teleportTo(living.getX(), targetY, living.getZ());
            } else {
                living.setNoGravity(false);
                return false;
            }
            living.setDeltaMovement(Vec3.ZERO);
            living.hurtMarked = true;
            return true;
        });
    }

    /**
     * Khối: thay bằng khối rơi, nâng như sinh vật, hết thời gian thì thả. Sát thương khi rơi trúng = m·g·h_rơi / 1 kJ
     * (spec 8.9) do {@link #applyFallDamage} áp, không dùng luật rơi của vanilla. Trả tên khối, null nếu không nâng được.
     */
    private static Component liftBlock(ServerLevel level, BlockPos pos, double height, int holdTicks) {
        if (!liftable(level, pos)) return null;
        BlockState state = level.getBlockState(pos);
        Component name = state.getBlock().getName();
        double massKg = blockMassKg(level, pos);
        level.removeBlock(pos, false);
        FallingBlockEntity falling = FallingBlockEntity.fall(level, pos, state);
        falling.setNoGravity(true);
        falling.dropItem = false;
        double startY = falling.getY();
        int riseTicks = (int) Math.max(1, RISE_SECONDS * TICKS_PER_SECOND);
        TimedEffects.add(level, (lvl, age) -> {
            if (!falling.isAlive()) return false;
            if (age <= riseTicks) {
                double t = (double) age / riseTicks;
                falling.teleportTo(falling.getX(), startY + height * t, falling.getZ());
            } else if (age <= riseTicks + holdTicks) {
                falling.teleportTo(falling.getX(), startY + height, falling.getZ());
            } else {
                falling.setNoGravity(false);
                return false;
            }
            falling.setDeltaMovement(Vec3.ZERO);
            return true;
        });
        impactWatch(level, falling, massKg);
        return name;
    }

    /**
     * Theo dõi khối rơi tới khi nó chạm đất (vanilla đặt khối rồi xóa thực thể trong tick của nó, nên phát hiện qua
     * việc thực thể không còn sống). Sinh vật trong hộp va chạm mất m·g·h_rơi / 1 kJ máu (spec 8.9).
     */
    private static void impactWatch(ServerLevel level, FallingBlockEntity falling, double massKg) {
        double[] peakY = {falling.getY()};
        TimedEffects.add(level, (lvl, age) -> {
            if (falling.isAlive()) {
                peakY[0] = Math.max(peakY[0], falling.getY());
                return true;
            }
            applyFallDamage(level, falling, massKg, peakY[0] - falling.getY());
            return false;
        });
    }

    /** Khối rơi trúng ai thì người đó mất máu theo thế năng đã tiêu; chỉ trúng một lần cho mỗi khối. */
    private static void applyFallDamage(ServerLevel level, FallingBlockEntity falling, double massKg,
            double fallM) {
        double hp = LiftCalc.impactDamageHp(massKg, Math.max(0, fallM));
        if (hp <= 0) return;
        // Khối rơi vanilla đã tự đặt lại thành khối và biến mất: xét quanh ô đó, không xét hộp của thực thể.
        AABB box = new AABB(BlockPos.containing(falling.position())).inflate(0.5);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (living.isSpectator() || living instanceof ServerPlayer player && player.isCreative()) continue;
            living.hurt(level.damageSources().fallingBlock(falling), (float) hp);
        }
    }

    /** Khối lượng mục tiêu: bảng sinh vật, hoặc ρ × 1 m³ cho khối (spec 8.9). */
    static double massKg(ServerLevel level, SpellUnit unit) {
        return switch (unit) {
            case SpellUnit.EntityUnit e -> ImpulseSpell.massKg(e.entity());
            case SpellUnit.BlockUnit b -> blockMassKg(level, b.pos());
            case SpellUnit.AirUnit a -> 0;
        };
    }

    private static double blockMassKg(ServerLevel level, BlockPos pos) {
        Material material = MaterialLookup.of(level.getBlockState(pos)).orElse(Materials.DEFAULT_SOLID);
        return material.density();
    }

    private static void particles(ServerLevel level, Vec3 center) {
        level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 10, 0.5, 0.1, 0.5, 0.02);
    }

    /** Khối lượng một khối theo bảng vật liệu — dùng cho sách. */
    public static double blockMassKg(BlockState state) {
        Material material = MaterialLookup.of(state).orElse(Materials.DEFAULT_SOLID);
        return material.density();
    }
}
