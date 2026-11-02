package phapthuat.gametest;

import java.util.EnumSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import phapthuat.physics.Element;
import phapthuat.spell.CastContext;
import phapthuat.spell.CastReport;
import phapthuat.spell.InscribedSpell;
import phapthuat.spell.Shape;
import phapthuat.spell.Spell;
import phapthuat.spell.SpellParam;
import phapthuat.spell.SpellUnit;

/** Phép giả chỉ dùng trong GameTest: không làm gì, chỉ ghi lại đơn vị nó nhận được. */
public final class ProbeSpell implements Spell {
    public static final ProbeSpell INSTANCE = new ProbeSpell();
    public static final String ID = "probe_test";

    public record Hit(ServerPlayer caster, List<SpellUnit> units) {}

    public static final Queue<Hit> HITS = new ConcurrentLinkedQueue<>();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Element element() {
        return Element.NHIET;
    }

    @Override
    public Set<Shape> shapes() {
        return EnumSet.allOf(Shape.class);
    }

    @Override
    public List<SpellParam> params() {
        return List.of();
    }

    @Override
    public double upfrontCostMana(InscribedSpell spell) {
        return 0;
    }

    @Override
    public boolean affects(ServerLevel level, SpellUnit unit, Shape shape) {
        return true;
    }

    @Override
    public CastReport apply(CastContext ctx, List<SpellUnit> units) {
        HITS.add(new Hit(ctx.caster(), units));
        return new CastReport(true, Component.literal("probe"));
    }
}
