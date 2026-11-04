package phapthuat.spell.impl;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import phapthuat.physics.Element;
import phapthuat.physics.EnergySplit;
import phapthuat.physics.EntityMass;
import phapthuat.physics.ImpulseCalc;
import phapthuat.physics.PhysicsConstants;
import phapthuat.spell.CastContext;
import phapthuat.spell.CastReport;
import phapthuat.spell.Fmt;
import phapthuat.spell.InscribedSpell;
import phapthuat.spell.Shape;
import phapthuat.spell.Spell;
import phapthuat.spell.SpellParam;
import phapthuat.spell.SpellUnit;

/**
 * Xung Lực (spec 8.3): động năng ½·m·v² thành vận tốc v = √(2E/m). Người thi triển không đứng vững thì nhận xung
 * lượng ngược chiều theo định luật 3 Newton.
 */
public final class ImpulseSpell implements Spell {
    public static final String ID = "xung_luc";
    public static final SpellParam ENERGY = new SpellParam("energy", "phapthuat.unit.mana", 0, 100, 1);
    /** Chỉ số trong hướng dẫn của {@link #DIRECTION}: 0 = Đẩy, 1 = Kéo. */
    public static final SpellParam DIRECTION = SpellParam.choice("direction",
            List.of("phapthuat.choice.push", "phapthuat.choice.pull"), 0);
    public static final int PUSH = 0;
    public static final int PULL = 1;

    /** Ticks mỗi giây của Minecraft. */
    private static final double TICKS_PER_SECOND = 20;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Element element() {
        return Element.DONG;
    }

    @Override
    public Set<Shape> shapes() {
        return EnumSet.of(Shape.DIEM, Shape.TIA, Shape.CAU, Shape.BAN_THAN);
    }

    @Override
    public List<SpellParam> params() {
        return List.of(ENERGY, DIRECTION);
    }

    @Override
    public double upfrontCostMana(InscribedSpell spell) {
        return spell.param(ENERGY);
    }

    /** Sinh vật, vật phẩm rơi, xe goòng, thuyền — không tác động lên khối (spec 8.3). */
    @Override
    public boolean affects(ServerLevel level, SpellUnit unit, Shape shape) {
        return switch (unit) {
            case SpellUnit.BlockUnit b -> false;
            case SpellUnit.AirUnit a -> false;
            case SpellUnit.EntityUnit e -> pushable(e.entity());
        };
    }

    private static boolean pushable(Entity entity) {
        if (entity instanceof LivingEntity living) return living.isAlive() && !living.isSpectator();
        if (entity instanceof ItemEntity item) return item.isAlive();
        return entity instanceof AbstractMinecart || entity instanceof Boat;
    }

    @Override
    public CastReport apply(CastContext ctx, List<SpellUnit> units) {
        if (units.isEmpty()) {
            return new CastReport(false, Component.translatable("phapthuat.impulse.nothing", displayName()));
        }
        double perUnitJ = EnergySplit.perUnit(ctx.spell().param(ENERGY) * ctx.joulesPerMana(), units.size());
        boolean pull = (int) ctx.spell().param(DIRECTION) == PULL;
        Vec3 look = ctx.caster().getViewVector(1.0f);
        Vec3 center = ctx.center();
        List<String> details = new ArrayList<>();
        double minSpeed = Double.MAX_VALUE;
        double maxSpeed = 0;
        double totalImpulse = 0;
        boolean selfLaunch = ctx.spell().shape() == Shape.BAN_THAN;
        for (SpellUnit unit : units) {
            if (!(unit instanceof SpellUnit.EntityUnit entityUnit)) continue;
            Entity entity = entityUnit.entity();
            double mass = massKg(entity);
            double speed = ImpulseCalc.speed(perUnitJ, mass);
            Vec3 dir = direction(ctx.spell().shape(), pull, look, center, entity);
            entity.setDeltaMovement(dir.scale(speed / TICKS_PER_SECOND));
            entity.hurtMarked = true;
            entity.hasImpulse = true;
            particles(ctx.level(), entity);
            details.add(Component.translatable("phapthuat.impulse.unit", entity.getDisplayName(),
                    Fmt.num(mass, 0), Fmt.num(speed, 1)).getString());
            minSpeed = Math.min(minSpeed, speed);
            maxSpeed = Math.max(maxSpeed, speed);
            totalImpulse += ImpulseCalc.impulse(perUnitJ, mass);
        }
        if (details.isEmpty()) {
            return new CastReport(false, Component.translatable("phapthuat.impulse.nothing", displayName()));
        }
        MutableComponent line = reportLine(units.size(), ctx, details, minSpeed, maxSpeed, perUnitJ);
        double recoil = selfLaunch ? 0 : recoilSpeed(ctx, totalImpulse);
        if (recoil > 0) {
            applyRecoil(ctx, look, recoil, pull, center);
            line.append(Component.translatable("phapthuat.impulse.recoil", Fmt.num(recoil, 1)));
        }
        return new CastReport(true, line);
    }

    private MutableComponent reportLine(int n, CastContext ctx, List<String> details, double min, double max,
            double perUnitJ) {
        if (n == 1) {
            return Component.translatable("phapthuat.impulse.single", displayName(), details.getFirst());
        }
        return Component.translatable("phapthuat.impulse.multi", displayName(), n,
                Fmt.mana(perUnitJ / ctx.joulesPerMana()), n, Fmt.num(min, 1), Fmt.num(max, 1));
    }

    /** Khối lượng của thực thể: bảng khối lượng sinh vật, còn lại 100 kg/m³ thể tích hitbox (spec 7.3). */
    static double massKg(Entity entity) {
        double volume = entity.getBbWidth() * entity.getBbWidth() * entity.getBbHeight();
        return EntityMass.kg(typeId(entity), volume);
    }

    private static String typeId(Entity entity) {
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return key == null ? "minecraft:unknown" : key.toString();
    }

    /** Hướng đẩy của một đơn vị: Cầu tỏa ra từ tâm, các thao tác khác theo hướng nhìn; Kéo thì ngược lại. */
    private static Vec3 direction(Shape shape, boolean pull, Vec3 look, Vec3 center, Entity entity) {
        Vec3 dir;
        if (shape == Shape.CAU) {
            Vec3 fromCenter = entity.getBoundingBox().getCenter().subtract(center);
            dir = fromCenter.lengthSqr() < 1e-6 ? Vec3.ZERO : fromCenter.normalize();
        } else {
            dir = look.normalize();
        }
        return pull ? dir.scale(-1) : dir;
    }

    /** Giật ngược: chỉ khi người chơi không đứng vững (đang bay/rơi hoặc đứng trên băng). */
    private static double recoilSpeed(CastContext ctx, double totalImpulse) {
        if (!losesFooting(ctx.level(), ctx.caster())) return 0;
        return ImpulseCalc.recoilSpeed(totalImpulse);
    }

    private static void applyRecoil(CastContext ctx, Vec3 look, double recoil, boolean pull, Vec3 center) {
        ServerPlayer player = ctx.caster();
        Vec3 dir;
        if (pull) {
            dir = center.subtract(player.position());
            dir = dir.lengthSqr() < 1e-6 ? look.normalize() : dir.normalize();
        } else {
            dir = look.normalize().scale(-1);
        }
        // Cộng thêm vào vận tốc đang có rồi kẹp lại: nhiều lần thi triển liên tiếp khi đang rơi không được vượt 40 m/s.
        Vec3 wanted = player.getDeltaMovement().add(dir.x * recoil / TICKS_PER_SECOND,
                dir.y * recoil / TICKS_PER_SECOND, dir.z * recoil / TICKS_PER_SECOND);
        double cap = PhysicsConstants.MAX_SPELL_SPEED / TICKS_PER_SECOND;
        player.setDeltaMovement(wanted.length() > cap ? wanted.normalize().scale(cap) : wanted);
        player.hurtMarked = true;
    }

    /** Người chơi mất chỗ đứng khi đang bay/rơi, hoặc đứng trên băng, băng nén, băng lam (spec 8.3). */
    private static boolean losesFooting(ServerLevel level, ServerPlayer player) {
        BlockPos below = player.blockPosition().below();
        BlockState state = level.getBlockState(below);
        if (state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE)) return true;
        return !player.onGround();
    }

    private static void particles(ServerLevel level, Entity entity) {
        level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5,
                entity.getZ(), 6, 0.2, 0.2, 0.2, 0.05);
    }
}
