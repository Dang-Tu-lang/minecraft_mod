package phapthuat.spell.impl;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import phapthuat.Config;
import phapthuat.physics.Element;
import phapthuat.physics.IronContent;
import phapthuat.physics.MagnetCalc;
import phapthuat.spell.CastContext;
import phapthuat.spell.CastReport;
import phapthuat.spell.Fmt;
import phapthuat.spell.InscribedSpell;
import phapthuat.spell.Shape;
import phapthuat.spell.Spell;
import phapthuat.spell.SpellParam;
import phapthuat.spell.SpellUnit;
import phapthuat.spell.Targeting;
import phapthuat.spell.TimedEffects;

/**
 * Từ Trường (spec 8.5): trường B trong quả cầu bán kính r chứa năng lượng u·V, hút mọi thứ chứa sắt về tâm với gia tốc
 * a = k·B²·(m_sắt/m). Hiệu ứng chạy mỗi tick suốt thời gian, nên đi vào vùng là bị hút ngay.
 */
public final class MagnetSpell implements Spell {
    public static final String ID = "tu_truong";
    public static final SpellParam FIELD = new SpellParam("field", "phapthuat.unit.tesla", 0.1, 5, 0.5);
    public static final SpellParam DURATION = new SpellParam("duration", "phapthuat.unit.s", 1, 10, 3);
    /** Bán kính Cầu của phép này rộng hơn thao tác thường (spec 8.5: 1–6 m). */
    public static final SpellParam RADIUS = new SpellParam("radius", "phapthuat.unit.m", 1, 6, 2);
    private static final int TICKS_PER_SECOND = 20;
    /** Gia tốc m/s² đổi ra khối/tick². */
    private static final double TICKS_SQUARED_PER_SECOND_SQUARED = TICKS_PER_SECOND * TICKS_PER_SECOND;
    /** Màu hạt đường sức. */
    private static final Vector3f FIELD_COLOR = new Vector3f(0.55f, 0.75f, 1f);

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
        return EnumSet.of(Shape.CAU);
    }

    @Override
    public List<SpellParam> params() {
        return List.of(FIELD, DURATION);
    }

    @Override
    public List<SpellParam> shapeParams(Shape shape) {
        return List.of(RADIUS);
    }

    @Override
    public double upfrontCostMana(InscribedSpell spell) {
        double radius = spell.param(RADIUS);
        return MagnetCalc.fieldEnergyJ(spell.param(FIELD), radius) / Config.joulesPerMana();
    }

    /** Thực thể nào cũng được, miễn chứa sắt (spec 8.5). */
    @Override
    public boolean affects(ServerLevel level, SpellUnit unit, Shape shape) {
        return unit instanceof SpellUnit.EntityUnit e && ironKg(e.entity()) > 0;
    }

    @Override
    public CastReport apply(CastContext ctx, List<SpellUnit> units) {
        ServerLevel level = ctx.level();
        Vec3 center = ctx.center();
        double teslas = ctx.spell().param(FIELD);
        double radius = ctx.spell().param(RADIUS);
        int ticks = (int) Math.round(ctx.spell().param(DURATION) * TICKS_PER_SECOND);
        double energyJ = MagnetCalc.fieldEnergyJ(teslas, radius);
        TimedEffects.add(level, (lvl, age) -> {
            pull(lvl, center, radius, teslas);
            particles(lvl, center, radius);
            return age < ticks - 1;
        });
        return new CastReport(true, net.minecraft.network.chat.Component.translatable("phapthuat.magnet.done",
                displayName(), Fmt.num(teslas, 2), Fmt.num(radius, 1), energyJ / ctx.joulesPerMana(),
                Fmt.num(ctx.spell().param(DURATION), 0)));
    }

    /** Mỗi tick: mọi thực thể chứa sắt trong cầu được cộng vận tốc hướng về tâm. */
    private static void pull(ServerLevel level, Vec3 center, double radius, double teslas) {
        AABB box = new AABB(center, center).inflate(radius + 2);
        for (Entity entity : level.getEntities((Entity) null, box, e -> !e.isSpectator() && ironKg(e) > 0)) {
            Vec3 body = entity.getBoundingBox().getCenter();
            if (body.distanceTo(center) > radius + 0.5) continue;
            Vec3 toCenter = center.subtract(body);
            if (toCenter.lengthSqr() < 1e-4) continue;
            double mass = ImpulseSpell.massKg(entity);
            double accel = MagnetCalc.accel(Config.MAGNET_K.get(), teslas, ironKg(entity), mass);
            Vec3 step = toCenter.normalize().scale(accel / TICKS_SQUARED_PER_SECOND_SQUARED);
            entity.setDeltaMovement(entity.getDeltaMovement().add(step));
            entity.hurtMarked = true;
            entity.hasImpulse = true;
            // Người chơi tự điều khiển vận tốc mỗi tick nên cộng thẳng vào vị trí mới thấy tác dụng.
            if (entity instanceof Player player) player.setPos(player.getX() + step.x, player.getY() + step.y,
                    player.getZ() + step.z);
        }
    }

    private static void particles(ServerLevel level, Vec3 center, double radius) {
        DustParticleOptions dust = new DustParticleOptions(FIELD_COLOR, 1.0f);
        for (int i = 0; i < 12; i++) {
            double theta = level.random.nextDouble() * Math.PI * 2;
            double phi = Math.acos(2 * level.random.nextDouble() - 1);
            double r = radius * (0.4 + 0.6 * level.random.nextDouble());
            double x = center.x + r * Math.sin(phi) * Math.cos(theta);
            double y = center.y + r * Math.cos(phi);
            double z = center.z + r * Math.sin(phi) * Math.sin(theta);
            level.sendParticles(dust, x, y, z, 1, 0, 0, 0, 0);
        }
    }

    /** Sắt của thực thể (kg): golem, xe goòng, giáp sắt đang mặc, vật phẩm sắt rơi (spec 7.3). */
    static double ironKg(Entity entity) {
        if (entity instanceof ItemEntity item) {
            var key = BuiltInRegistries.ITEM.getKey(item.getItem().getItem());
            return key == null ? 0 : IronContent.itemKg(key.toString(), item.getItem().getCount());
        }
        var type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (type == null) return 0;
        return IronContent.kg(type.toString(), wornItemIds(entity));
    }

    private static List<String> wornItemIds(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return List.of();
        List<String> ids = new java.util.ArrayList<>();
        for (var stack : living.getArmorSlots()) {
            if (stack.isEmpty()) continue;
            var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (key != null) ids.add(key.toString());
        }
        return ids;
    }
}
