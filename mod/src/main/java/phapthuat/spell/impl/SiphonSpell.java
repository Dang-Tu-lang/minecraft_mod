package phapthuat.spell.impl;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import phapthuat.Config;
import phapthuat.mana.ManaManager;
import phapthuat.physics.Element;
import phapthuat.physics.EnergySplit;
import phapthuat.physics.Material;
import phapthuat.physics.Materials;
import phapthuat.physics.PhysicsConstants;
import phapthuat.physics.SiphonCalc;
import phapthuat.physics.SiphonCalc.SiphonResult;
import phapthuat.spell.CastContext;
import phapthuat.spell.CastReport;
import phapthuat.spell.Fmt;
import phapthuat.spell.InscribedSpell;
import phapthuat.spell.Shape;
import phapthuat.spell.Spell;
import phapthuat.spell.SpellParam;
import phapthuat.spell.SpellUnit;
import phapthuat.thermal.BlockThermal;
import phapthuat.thermal.EntityThermal;
import phapthuat.thermal.MaterialLookup;
import phapthuat.thermal.ThermalStore;
import phapthuat.thermal.WorldTemps;

/**
 * Thu Nhiệt (spec mục 8.2): rút nhiệt khỏi mục tiêu. Vật nóng hơn môi trường cho mana theo hiệu suất Carnot; làm lạnh
 * xuống dưới môi trường thì tốn mana theo COP. Thiếu mana trả công làm lạnh thì phần thiếu rút từ thân nhiệt người
 * thi triển.
 */
public final class SiphonSpell implements Spell {
    public static final String ID = "thu_nhiet";
    public static final SpellParam HEAT = new SpellParam("heat", "phapthuat.unit.mana", 0, 1000, 10);
    public static final double ACTIVATION_COST_MANA = 1;

    /** Kết quả trên một đơn vị: nhiệt độ đầu/cuối và môi trường để báo η hoặc COP. */
    private record Drawn(Component detail, SiphonResult result, double startC, double ambientC) {}

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
        return EnumSet.of(Shape.DIEM, Shape.CAU, Shape.BAN_THAN);
    }

    @Override
    public List<SpellParam> params() {
        return List.of(HEAT);
    }

    @Override
    public double upfrontCostMana(InscribedSpell spell) {
        return ACTIVATION_COST_MANA;
    }

    /** Dung nham nguồn, magma, lửa, lửa trại đang cháy, nước nguồn và sinh vật không miễn lửa (spec 7.3). */
    @Override
    public boolean affects(ServerLevel level, SpellUnit unit, Shape shape) {
        return switch (unit) {
            case SpellUnit.BlockUnit b -> target(level.getBlockState(b.pos())) != null;
            case SpellUnit.AirUnit a -> false;
            case SpellUnit.EntityUnit e -> e.entity() instanceof LivingEntity living && living.isAlive()
                    && !living.fireImmune();
        };
    }

    /** Vật liệu rút nhiệt được của khối này, null nếu khối không phải mục tiêu của Thu Nhiệt. */
    private static @Nullable Material target(BlockState state) {
        Material m = MaterialLookup.of(state).orElse(null);
        boolean ok = m == Materials.LAVA || m == Materials.MAGMA || m == Materials.FIRE || m == Materials.CAMPFIRE
                || (m == Materials.WATER && state.is(Blocks.WATER));
        return ok ? m : null;
    }

    @Override
    public CastReport apply(CastContext ctx, List<SpellUnit> units) {
        if (units.isEmpty()) {
            return new CastReport(false, Component.translatable("phapthuat.siphon.nothing", displayName()));
        }
        double perUnit = EnergySplit.perUnit(ctx.spell().param(HEAT) * ctx.joulesPerMana(), units.size());
        List<Drawn> drawn = new ArrayList<>();
        for (SpellUnit unit : units) {
            switch (unit) {
                case SpellUnit.BlockUnit b -> {
                    Drawn d = siphonBlock(ctx.level(), b.pos(), perUnit);
                    if (d != null) drawn.add(d);
                }
                case SpellUnit.EntityUnit e when e.entity() instanceof LivingEntity living ->
                        drawn.add(siphonEntity(living, perUnit));
                default -> {}
            }
        }
        if (drawn.isEmpty()) {
            return new CastReport(false, Component.translatable("phapthuat.siphon.nothing", displayName()));
        }

        double factor = Config.SIPHON_GAIN_FACTOR.get();
        double netJ = 0;
        boolean capped = false;
        for (Drawn d : drawn) {
            double delta = d.result().manaJoulesDelta();
            netJ += delta > 0 ? delta * factor : delta;
            capped |= d.result().capped();
        }
        double overdrawMana = settle(ctx, netJ / ctx.joulesPerMana());
        return report(drawn, netJ / ctx.joulesPerMana(), capped, overdrawMana);
    }

    /** Cộng mana được, trừ mana tốn. Trả về số mana còn thiếu (đã rút từ thân nhiệt người thi triển), 0 nếu đủ. */
    private static double settle(CastContext ctx, double netMana) {
        ServerPlayer caster = ctx.caster();
        if (netMana >= 0) {
            ManaManager.add(caster, netMana);
            return 0;
        }
        double owed = -netMana;
        double have = ManaManager.get(caster);
        if (have >= owed) {
            ManaManager.trySpend(caster, owed);
            return 0;
        }
        ManaManager.set(caster, 0);
        double missing = owed - have;
        freezeCaster(ctx, missing * ctx.joulesPerMana());
        return missing;
    }

    /** Phản tác dụng: công làm lạnh còn thiếu được rút thẳng từ lõi của người thi triển. */
    private static void freezeCaster(CastContext ctx, double joules) {
        ServerPlayer caster = ctx.caster();
        Material core = Materials.LIVING_CORE;
        EntityThermal now = WorldTemps.currentEntity(caster);
        SiphonResult r = SiphonCalc.siphon(core, core.coreMassKg(), now.coreTempC(), 0, joules,
                WorldTemps.ambientC(ctx.level(), caster.blockPosition()));
        WorldTemps.saveEntity(caster, r.finalTempC(), now.doseGy());
        caster.setTicksFrozen(caster.getTicksRequiredToFreeze());
    }

    /** Null nếu khối đã đổi giữa lúc chọn mục tiêu và lúc rút (không còn là mục tiêu): bỏ qua, không tốn gì. */
    private static @Nullable Drawn siphonBlock(ServerLevel level, BlockPos pos, double joules) {
        BlockState state = level.getBlockState(pos);
        Material m = target(state);
        if (m == null) return null;
        double ambient = WorldTemps.ambientC(level, pos);
        BlockThermal now = WorldTemps.currentBlock(level, pos, m);
        // Lửa và lửa trại không có khối lượng lõi: nhiệt rút được là con số cố định trong bảng.
        double mass = Double.isNaN(m.density()) ? 0 : m.coreMassKg();
        SiphonResult r = SiphonCalc.siphon(m, mass, now.tempC(), now.phaseProgressJ(), joules, ambient);
        Component name = Component.translatable("phapthuat.material." + m.id());
        Component detail;
        if (r.transformed()) {
            ThermalStore.get(level).remove(pos);
            level.setBlockAndUpdate(pos, cooledInto(state, m));
            detail = Component.translatable("phapthuat.siphon.transformed." + m.id());
        } else {
            WorldTemps.saveBlock(level, pos, new BlockThermal(m.id(), r.finalTempC(), r.phaseProgressJ(),
                    now.naturalC(), now.halfLifeSeconds()));
            detail = partialLine(m, name, r);
        }
        level.sendParticles(ParticleTypes.SNOWFLAKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 8, 0.3,
                0.2, 0.3, 0.01);
        return new Drawn(detail, r, now.tempC(), ambient);
    }

    private static BlockState cooledInto(BlockState state, Material m) {
        if (m == Materials.LAVA) return Blocks.OBSIDIAN.defaultBlockState();
        if (m == Materials.MAGMA) return Blocks.NETHERRACK.defaultBlockState();
        if (m == Materials.CAMPFIRE) return state.setValue(BlockStateProperties.LIT, false);
        if (m == Materials.WATER) return Blocks.ICE.defaultBlockState();
        return Blocks.AIR.defaultBlockState();
    }

    /** Tiến độ âm là nhiệt đã rút ở bình nguyên (đang đông dở, hoặc phần nhiệt của lửa đã rút). */
    private static Component partialLine(Material m, Component name, SiphonResult r) {
        double removed = -r.phaseProgressJ();
        if (!Double.isNaN(m.maxExtractableJ())) {
            return Component.translatable("phapthuat.siphon.fire_partial", name,
                    Fmt.num(100 * removed / m.maxExtractableJ(), 0));
        }
        double latent = freezeLatent(m, r.finalTempC()) * m.coreMassKg();
        if (latent > 0 && removed >= 1) {
            return Component.translatable("phapthuat.siphon.cooled_phase", name, Fmt.tempC(r.finalTempC()),
                    Fmt.num(100 * Math.min(1, removed / latent), 0));
        }
        return Component.translatable("phapthuat.siphon.cooled", name, Fmt.tempC(r.finalTempC()));
    }

    /** Nhiệt đông đặc riêng (J/kg) ở bình nguyên hiện tại. Nước đông theo số liệu của băng. */
    private static double freezeLatent(Material m, double tempC) {
        if (m == Materials.WATER) return Materials.ICE.latentAt(tempC);
        return m.latentAt(tempC);
    }

    private static Drawn siphonEntity(LivingEntity e, double joules) {
        Material core = Materials.LIVING_CORE;
        EntityThermal now = WorldTemps.currentEntity(e);
        double ambient = WorldTemps.ambientC(e.level(), e.blockPosition());
        SiphonResult r = SiphonCalc.siphon(core, core.coreMassKg(), now.coreTempC(), 0, joules, ambient);
        WorldTemps.saveEntity(e, r.finalTempC(), now.doseGy());
        MutableComponent detail = Component.translatable("phapthuat.siphon.entity", e.getDisplayName(),
                Fmt.tempC(r.finalTempC()));
        if (e.isOnFire()) {
            e.clearFire();
            detail.append(Component.translatable("phapthuat.siphon.doused"));
        }
        return new Drawn(detail, r, now.coreTempC(), ambient);
    }

    private CastReport report(List<Drawn> drawn, double netMana, boolean capped, double overdrawMana) {
        MutableComponent line;
        if (drawn.size() == 1) {
            line = Component.translatable("phapthuat.siphon.single", displayName(), drawn.getFirst().detail());
        } else {
            long transformed = drawn.stream().filter(d -> d.result().transformed()).count();
            line = Component.translatable("phapthuat.siphon.multi", displayName(), drawn.size(),
                    Component.translatable("phapthuat.siphon.count", transformed, drawn.size() - transformed));
        }
        line.append(money(drawn, netMana));
        if (capped) line.append(Component.translatable("phapthuat.siphon.capped"));
        if (overdrawMana > 0) line.append(Component.translatable("phapthuat.siphon.overdraw", Fmt.mana(overdrawMana)));
        return new CastReport(true, line);
    }

    /** Mana được hoặc tốn; với một đơn vị thì kèm η (được) hoặc COP (tốn) để người chơi học công thức. */
    private static Component money(List<Drawn> drawn, double netMana) {
        if (Math.abs(netMana) < 0.05) return Component.translatable("phapthuat.siphon.even");
        String amount = Fmt.mana(Math.abs(netMana));
        if (drawn.size() != 1) {
            return Component.translatable(netMana > 0 ? "phapthuat.siphon.gain" : "phapthuat.siphon.cost", amount);
        }
        Drawn d = drawn.getFirst();
        double ambientK = PhysicsConstants.toKelvin(d.ambientC());
        if (netMana > 0) {
            double eta = 1 - ambientK / PhysicsConstants.toKelvin(d.startC());
            return Component.translatable("phapthuat.siphon.gain_eta", amount, Fmt.num(eta, 2));
        }
        double finalK = PhysicsConstants.toKelvin(d.result().finalTempC());
        if (finalK >= ambientK) return Component.translatable("phapthuat.siphon.cost", amount);
        return Component.translatable("phapthuat.siphon.cost_cop", amount, Fmt.num(finalK / (ambientK - finalK), 1));
    }
}
