package phapthuat.spell.impl;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import phapthuat.Config;
import phapthuat.physics.Element;
import phapthuat.physics.EnergySplit;
import phapthuat.physics.HeatCalc;
import phapthuat.physics.HeatCalc.AirBurst;
import phapthuat.physics.HeatCalc.HeatResult;
import phapthuat.physics.HeatCalc.SteamBurst;
import phapthuat.physics.HeatTier;
import phapthuat.physics.Material;
import phapthuat.physics.Materials;
import phapthuat.physics.PhysicsConstants;
import phapthuat.physics.ShapeMath;
import phapthuat.spell.CastContext;
import phapthuat.spell.CastReport;
import phapthuat.spell.Fmt;
import phapthuat.spell.InscribedSpell;
import phapthuat.spell.Shape;
import phapthuat.spell.Spell;
import phapthuat.spell.SpellParam;
import phapthuat.spell.SpellUnit;
import phapthuat.spell.TimedEffects;
import phapthuat.thermal.BlockThermal;
import phapthuat.thermal.EntityThermal;
import phapthuat.thermal.MaterialLookup;
import phapthuat.thermal.ThermalStore;
import phapthuat.thermal.WorldTemps;

/**
 * Gia Nhiệt (spec mục 8.1): đổ nhiệt vào lõi 1 lít của từng đơn vị rồi áp bậc cao nhất đạt tới. Phần áp nhiệt
 * ({@link #applyHeat}) dùng lại được cho mọi phép sinh nhiệt khác.
 */
public final class HeatSpell implements Spell {
    public static final String ID = "gia_nhiet";
    public static final SpellParam ENERGY = new SpellParam("energy", "phapthuat.unit.mana", 0, 1000, 10);

    /** Đám hơi khi nước bốc hơi: bán kính (khối), thời gian (tick), bỏng hơi mỗi giây. */
    private static final double STEAM_RADIUS = 2;
    private static final int STEAM_TICKS = 60;
    private static final float STEAM_DAMAGE_PER_SECOND = 1;
    /** Từ công suất này trở lên thì hơi nước nổ như TNT. */
    private static final double STEAM_EXPLOSION_MIN_POWER = 2;
    /** Vận tốc hất (khối/tick) cho mỗi đơn vị công suất của đám hơi nhỏ. */
    private static final double STEAM_PUSH_PER_POWER = 0.15;
    private static final int BACKLASH_BURN_SECONDS = 3;
    private static final int HOT_AIR_BURN_SECONDS = 3;
    /** Ngọn lửa mana trên khối không cháy được, khoảng 1 giây. */
    private static final int MANA_FLAME_TICKS = 20;
    /** Áp suất dư (Pa) ứng với 1 máu mất thêm, trên ngưỡng sát thương. */
    private static final double BLAST_PA_PER_HEALTH = 5_000;
    private static final Direction[] FIRE_ORDER = {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST,
            Direction.WEST, Direction.DOWN};

    /** Kết quả trên một đơn vị, xếp theo thứ tự liệt kê trong dòng báo nhiều đơn vị. */
    private enum Kind {
        IGNITE("ignite", true), DETONATE("ignite", true), LIGHT("ignite", true), MELT("melt", true),
        VAPORIZE("vaporize", true), CRACK("crack", true), NO_OXYGEN("no_oxygen", false), NONE("none", false),
        INERT("inert", false);

        final String countKey;
        final boolean success;

        Kind(String countKey, boolean success) {
            this.countKey = countKey;
            this.success = success;
        }
    }

    private record UnitResult(Kind kind, Component detail, int backlashRadius) {
        UnitResult(Kind kind, Component detail) {
            this(kind, detail, 0);
        }

        UnitResult withBacklash(int radius) {
            return new UnitResult(kind, detail, radius);
        }
    }

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
        return List.of(ENERGY);
    }

    @Override
    public double upfrontCostMana(InscribedSpell spell) {
        return spell.param(ENERGY);
    }

    @Override
    public boolean affects(ServerLevel level, SpellUnit unit, Shape shape) {
        return affectsHeat(level, unit, shape);
    }

    @Override
    public CastReport apply(CastContext ctx, List<SpellUnit> units) {
        return applyHeat(ctx, units, ctx.spell().param(ENERGY) * ctx.joulesPerMana(), displayName());
    }

    /** Mọi khối (khối ngoài bảng vật liệu chỉ nhận ngọn lửa mana), ô không khí với Cầu/Tường, sinh vật không miễn lửa. */
    public static boolean affectsHeat(ServerLevel level, SpellUnit unit, Shape shape) {
        return switch (unit) {
            case SpellUnit.BlockUnit b -> !level.getBlockState(b.pos()).isAir();
            case SpellUnit.AirUnit a -> shape == Shape.CAU || shape == Shape.TUONG;
            case SpellUnit.EntityUnit e -> e.entity() instanceof LivingEntity living && living.isAlive()
                    && !living.fireImmune();
        };
    }

    /** Chia đều {@code totalJoules} cho các đơn vị và áp nhiệt theo spec 8.1. {@code sourceName} đứng đầu dòng báo. */
    public static CastReport applyHeat(CastContext ctx, List<SpellUnit> units, double totalJoules,
            Component sourceName) {
        double perUnit = EnergySplit.perUnit(totalJoules, units.size());
        List<UnitResult> results = new ArrayList<>();
        List<BlockPos> air = new ArrayList<>();
        for (SpellUnit unit : units) {
            switch (unit) {
                case SpellUnit.BlockUnit b -> results.add(heatBlock(ctx, b.pos(), perUnit));
                case SpellUnit.AirUnit a -> air.add(a.pos());
                case SpellUnit.EntityUnit e when e.entity() instanceof LivingEntity living ->
                        results.add(heatEntity(ctx, living, perUnit));
                case SpellUnit.EntityUnit e -> {}
            }
        }
        Component airLine = air.isEmpty() ? null : heatAir(ctx, air, perUnit * air.size());
        if (results.isEmpty() && airLine == null) {
            return new CastReport(false, Component.translatable("phapthuat.heat.nothing", sourceName,
                    Fmt.mana(totalJoules / ctx.joulesPerMana())));
        }
        return report(sourceName, units.size(), perUnit / ctx.joulesPerMana(), results, airLine);
    }

    private static CastReport report(Component source, int n, double manaPerUnit, List<UnitResult> results,
            @Nullable Component airLine) {
        boolean success = airLine != null || results.stream().anyMatch(r -> r.kind().success);
        int backlash = results.stream().mapToInt(UnitResult::backlashRadius).max().orElse(0);
        MutableComponent line;
        if (n == 1) {
            line = Component.translatable("phapthuat.heat.single", source,
                    airLine != null ? airLine : results.getFirst().detail());
        } else {
            Map<String, Integer> counts = new LinkedHashMap<>();
            for (Kind k : Kind.values()) {
                for (UnitResult r : results) {
                    if (r.kind() == k) counts.merge(k.countKey, 1, Integer::sum);
                }
            }
            MutableComponent body = Component.empty();
            for (Map.Entry<String, Integer> c : counts.entrySet()) {
                if (!body.getSiblings().isEmpty()) body.append(", ");
                body.append(Component.translatable("phapthuat.heat.count." + c.getKey(), c.getValue()));
            }
            if (airLine != null) {
                if (!body.getSiblings().isEmpty()) body.append("; ");
                body.append(airLine);
            }
            line = Component.translatable("phapthuat.heat.multi", source, n, Fmt.mana(manaPerUnit), body);
        }
        if (backlash > 0) line.append(Component.translatable("phapthuat.heat.backlash", backlash));
        return new CastReport(success, line);
    }

    // ---- Khối ----

    private static UnitResult heatBlock(CastContext ctx, BlockPos pos, double joules) {
        ServerLevel level = ctx.level();
        BlockState state = level.getBlockState(pos);
        String mana = Fmt.mana(joules / ctx.joulesPerMana());
        Optional<Material> found = MaterialLookup.of(state).filter(HeatSpell::heatable);
        if (found.isEmpty()) {
            manaFlame(level, pos);
            return new UnitResult(Kind.INERT,
                    Component.translatable("phapthuat.heat.inert", state.getBlock().getName(), mana));
        }
        Material m = found.get();
        BlockThermal now = WorldTemps.currentBlock(level, pos, m);
        HeatResult r = HeatCalc.heat(m, m.coreMassKg(), now.tempC(), now.phaseProgressJ(), joules);
        Component name = materialName(m);
        UnitResult result = switch (r.tier()) {
            case NONE -> {
                keepHeat(level, pos, now, r);
                level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 6,
                        0.25, 0.1, 0.25, 0.01);
                yield new UnitResult(Kind.NONE, failLine(name, m, r.finalTempC(), r.phaseProgressJ(), mana));
            }
            case CRACK -> {
                ThermalStore.get(level).remove(pos);
                // Bảng rơi của vanilla: đá → đá cuội, hắc diện thạch → chính nó, thủy tinh/băng → không gì.
                level.destroyBlock(pos, true);
                yield new UnitResult(Kind.CRACK, Component.translatable("phapthuat.heat.crack", name, mana));
            }
            case IGNITE -> ignite(ctx, pos, state, now, r, name, mana);
            case MELT -> {
                melt(level, pos, m);
                yield new UnitResult(Kind.MELT, Component.translatable("phapthuat.heat.melt." + m.id(), mana));
            }
            case VAPORIZE -> vaporize(level, pos, state, m, r, name, mana);
        };
        // Phản tác dụng chỉ với vật cháy được (spec 8.1): khối không cháy được và không khí không có luật này.
        if (r.backlash() && m.flammable()) {
            backlash(level, pos, r.backlashRadius());
            return result.withBacklash(r.backlashRadius());
        }
        return result;
    }

    /** Khối nóng tự nhiên (dung nham, magma, lửa) và khối ngoài bảng chỉ nhận ngọn lửa mana. */
    private static boolean heatable(Material m) {
        return Double.isNaN(m.naturalTempC()) && m != Materials.AIR;
    }

    private static void keepHeat(ServerLevel level, BlockPos pos, BlockThermal now, HeatResult r) {
        WorldTemps.saveBlock(level, pos, new BlockThermal(now.materialId(), r.finalTempC(), r.phaseProgressJ(),
                now.naturalC(), now.halfLifeSeconds()));
    }

    private static UnitResult ignite(CastContext ctx, BlockPos pos, BlockState state, BlockThermal now,
            HeatResult r, Component name, String mana) {
        ServerLevel level = ctx.level();
        String temp = Fmt.tempC(r.finalTempC());
        if (state.is(Blocks.TNT)) {
            // TNT tự mang chất oxy hóa nên vẫn nổ cả khi ở trong nước.
            state.getBlock().onCaughtFire(state, level, pos, ctx.face(), ctx.caster());
            level.removeBlock(pos, false);
            ThermalStore.get(level).remove(pos);
            return new UnitResult(Kind.DETONATE, Component.translatable("phapthuat.heat.detonate", name, temp, mana));
        }
        keepHeat(level, pos, now, r);
        if (WorldTemps.inWater(level, pos)) return noOxygen(name, temp, mana);
        if (CampfireBlock.canLight(state) || CandleBlock.canLight(state)) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, true), Block.UPDATE_ALL_IMMEDIATE);
            return new UnitResult(Kind.LIGHT, Component.translatable("phapthuat.heat.light", name, temp, mana));
        }
        BlockPos fire = firePos(level, pos, fireFace(ctx));
        if (fire == null) return noOxygen(name, temp, mana);
        level.setBlock(fire, BaseFireBlock.getState(level, fire), Block.UPDATE_ALL_IMMEDIATE);
        return new UnitResult(Kind.IGNITE, Component.translatable("phapthuat.heat.ignite", name, temp, mana));
    }

    private static UnitResult noOxygen(Component name, String temp, String mana) {
        return new UnitResult(Kind.NO_OXYGEN, Component.translatable("phapthuat.heat.no_oxygen", name, temp, mana));
    }

    /** Điểm và Đạn đặt lửa cạnh mặt bị nhắm; thao tác khác đặt lửa phía trên. */
    private static Direction fireFace(CastContext ctx) {
        Shape shape = ctx.spell().shape();
        return (shape == Shape.DIEM || shape == Shape.DAN) && ctx.face() != null ? ctx.face() : Direction.UP;
    }

    /** Ô không khí để đặt lửa: ưu tiên mặt cho trước, rồi các mặt khác. Null nếu khối bị bít kín. */
    private static @Nullable BlockPos firePos(ServerLevel level, BlockPos pos, Direction preferred) {
        if (level.getBlockState(pos.relative(preferred)).isAir()) return pos.relative(preferred);
        for (Direction d : FIRE_ORDER) {
            if (level.getBlockState(pos.relative(d)).isAir()) return pos.relative(d);
        }
        return null;
    }

    private static void melt(ServerLevel level, BlockPos pos, Material m) {
        ThermalStore.get(level).remove(pos);
        BlockState into;
        if (m == Materials.SAND) {
            into = Blocks.GLASS.defaultBlockState();
        } else if (m == Materials.STONE) {
            into = Blocks.LAVA.defaultBlockState();
        } else {
            // Băng, tuyết. Như vanilla: ở Nether nước bốc hơi ngay.
            into = level.dimensionType().ultraWarm() ? Blocks.AIR.defaultBlockState() : Blocks.WATER.defaultBlockState();
        }
        level.setBlockAndUpdate(pos, into);
    }

    private static UnitResult vaporize(ServerLevel level, BlockPos pos, BlockState state, Material m, HeatResult r,
            Component name, String mana) {
        ThermalStore.get(level).remove(pos);
        level.setBlockAndUpdate(pos, dried(state));
        SteamBurst burst = HeatCalc.steamBurst(m.coreMassKg(), r.excessJ());
        steam(level, Vec3.atCenterOf(pos), burst.power());
        return new UnitResult(Kind.VAPORIZE, Component.translatable("phapthuat.heat.vaporize", name,
                Fmt.num(burst.steamTempK(), 1), Fmt.num(burst.workJ() / 1000, 1), Fmt.num(burst.power(), 1), mana));
    }

    /** Khối sau khi mất hết nước: bọt biển ướt → khô, bùn → đất, khối ngậm nước → hết nước, còn lại → không khí. */
    private static BlockState dried(BlockState state) {
        if (state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED)) {
            return state.setValue(BlockStateProperties.WATERLOGGED, false);
        }
        if (state.is(Blocks.WET_SPONGE)) return Blocks.SPONGE.defaultBlockState();
        if (state.is(Blocks.MUD)) return Blocks.DIRT.defaultBlockState();
        return Blocks.AIR.defaultBlockState();
    }

    /** Hơi nước giãn nở: luôn có đám hơi bỏng; công suất ≥ 2 thì nổ, nhỏ hơn thì chỉ hất nhẹ. */
    private static void steam(ServerLevel level, Vec3 center, double power) {
        if (power >= STEAM_EXPLOSION_MIN_POWER) {
            // Nguồn nổ null: người thi triển đứng gần cũng chịu như mọi sinh vật khác.
            level.explode(null, center.x, center.y, center.z, (float) power,
                    Config.STEAM_EXPLOSION_BREAKS_BLOCKS.get() ? Level.ExplosionInteraction.TNT
                            : Level.ExplosionInteraction.NONE);
        } else {
            for (LivingEntity e : livingNear(level, center, STEAM_RADIUS)) push(e, center, STEAM_PUSH_PER_POWER * power);
        }
        TimedEffects.add(level, (lvl, age) -> {
            lvl.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z, 8, 1.0, 0.6, 1.0, 0.02);
            if (age % 20 == 0) {
                for (LivingEntity e : livingNear(lvl, center, STEAM_RADIUS)) {
                    e.hurt(lvl.damageSources().inFire(), STEAM_DAMAGE_PER_SECOND);
                }
            }
            return age < STEAM_TICKS - 1;
        });
    }

    private static void manaFlame(ServerLevel level, BlockPos pos) {
        TimedEffects.add(level, (lvl, age) -> {
            lvl.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.02, pos.getZ() + 0.5, 2, 0.25,
                    0.02, 0.25, 0.005);
            return age < MANA_FLAME_TICKS - 1;
        });
    }

    // ---- Sinh vật ----

    private static UnitResult heatEntity(CastContext ctx, LivingEntity e, double joules) {
        String mana = Fmt.mana(joules / ctx.joulesPerMana());
        Material core = Materials.LIVING_CORE;
        EntityThermal now = WorldTemps.currentEntity(e);
        HeatResult r = HeatCalc.heat(core, core.coreMassKg(), now.coreTempC(), 0, joules);
        WorldTemps.saveEntity(e, r.finalTempC(), now.doseGy());
        Component name = e.getDisplayName();
        UnitResult result;
        if (r.tier() != HeatTier.IGNITE) {
            result = new UnitResult(Kind.NONE, failLine(name, core, r.finalTempC(), 0, mana));
        } else if (e.isInWater()) {
            result = noOxygen(name, Fmt.tempC(r.finalTempC()), mana);
        } else {
            int seconds = HeatCalc.burnSeconds(r.excessJ(), ctx.joulesPerMana());
            setOnFire(e, seconds);
            result = new UnitResult(Kind.IGNITE, Component.translatable("phapthuat.heat.entity_ignite", name, seconds,
                    Fmt.tempC(r.finalTempC()), mana));
        }
        if (r.backlash()) {
            backlash(ctx.level(), e.blockPosition(), r.backlashRadius());
            return result.withBacklash(r.backlashRadius());
        }
        return result;
    }

    // ---- Không khí ----

    /** Gộp các ô không khí thành một khối khí rồi áp khí lý tưởng: vỡ kính, hất văng, sát thương, bốc cháy. */
    private static Component heatAir(CastContext ctx, List<BlockPos> cells, double joules) {
        ServerLevel level = ctx.level();
        Vec3 center = centroid(cells);
        AirBurst burst = HeatCalc.airBurst(cells.size(), joules, WorldTemps.ambientC(level, BlockPos.containing(center)));
        boolean hot = burst.finalTempK() - PhysicsConstants.KELVIN_OFFSET >= PhysicsConstants.HOT_AIR_IGNITE_C;

        // Vùng chịu áp suất: các ô khí nóng và mọi khối áp sát chúng.
        Set<BlockPos> zone = new HashSet<>(cells);
        for (BlockPos c : cells) {
            for (Direction d : Direction.values()) zone.add(c.relative(d));
        }
        if (burst.deltaPPa() >= PhysicsConstants.GLASS_BREAK_PA) {
            for (BlockPos p : zone) {
                if (MaterialLookup.of(level.getBlockState(p)).orElse(null) == Materials.GLASS) level.destroyBlock(p, false);
            }
        }
        double knock = Config.AIR_BURST_KNOCKBACK_PER_KPA.get() * burst.deltaPPa() / 1000;
        double damage = (burst.deltaPPa() - PhysicsConstants.BLAST_DAMAGE_PA) / BLAST_PA_PER_HEALTH;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, bounds(zone), e -> inZone(zone, e))) {
            push(e, center, knock);
            if (damage > 0) e.hurt(level.damageSources().explosion(null, null), (float) damage);
            if (hot && !e.fireImmune() && !e.isInWater()) setOnFire(e, HOT_AIR_BURN_SECONDS);
        }
        for (BlockPos c : cells) {
            level.sendParticles(hot ? ParticleTypes.FLAME : ParticleTypes.POOF, c.getX() + 0.5, c.getY() + 0.5,
                    c.getZ() + 0.5, 1, 0.3, 0.3, 0.3, 0.02);
        }
        return Component.translatable("phapthuat.heat.air", Fmt.num(burst.deltaTK(), 1),
                Fmt.num(burst.deltaPPa() / 1000, 1));
    }

    private static Vec3 centroid(List<BlockPos> cells) {
        double x = 0, y = 0, z = 0;
        for (BlockPos c : cells) {
            x += c.getX() + 0.5;
            y += c.getY() + 0.5;
            z += c.getZ() + 0.5;
        }
        return new Vec3(x / cells.size(), y / cells.size(), z / cells.size());
    }

    private static AABB bounds(Set<BlockPos> cells) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos c : cells) {
            minX = Math.min(minX, c.getX());
            minY = Math.min(minY, c.getY());
            minZ = Math.min(minZ, c.getZ());
            maxX = Math.max(maxX, c.getX());
            maxY = Math.max(maxY, c.getY());
            maxZ = Math.max(maxZ, c.getZ());
        }
        return new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }

    private static boolean inZone(Set<BlockPos> zone, LivingEntity e) {
        return zone.contains(e.blockPosition()) || zone.contains(BlockPos.containing(e.getEyePosition()));
    }

    // ---- Dùng chung ----

    /** Lửa bùng ra các ô không khí áp sát khối dễ cháy trong bán kính; người chơi trong bán kính bị cháy. */
    private static void backlash(ServerLevel level, BlockPos origin, int radius) {
        for (ShapeMath.Offset o : ShapeMath.sphere(radius)) {
            BlockPos p = origin.offset(o.dx(), o.dy(), o.dz());
            if (!level.isInWorldBounds(p)) continue;
            BlockState state = level.getBlockState(p);
            if (state.isAir() && hasFlammableNeighbor(level, p) && !WorldTemps.inWater(level, p)) {
                level.setBlock(p, BaseFireBlock.getState(level, p), Block.UPDATE_ALL_IMMEDIATE);
            }
        }
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(origin).inflate(radius),
                p -> !p.isInWater() && !p.fireImmune())) {
            setOnFire(player, BACKLASH_BURN_SECONDS);
        }
    }

    private static boolean hasFlammableNeighbor(ServerLevel level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            if (MaterialLookup.of(level.getBlockState(pos.relative(d))).map(Material::flammable).orElse(false)) {
                return true;
            }
        }
        return false;
    }

    private static List<LivingEntity> livingNear(ServerLevel level, Vec3 center, double radius) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius + 1),
                e -> e.getBoundingBox().getCenter().distanceTo(center) <= radius + 0.5);
    }

    /** Đẩy ra xa {@code from} với vận tốc {@code speed} (khối/tick). */
    private static void push(LivingEntity e, Vec3 from, double speed) {
        Vec3 dir = e.getBoundingBox().getCenter().subtract(from);
        dir = dir.lengthSqr() < 1e-6 ? new Vec3(0, 1, 0) : dir.normalize();
        e.push(dir.x * speed, dir.y * speed, dir.z * speed);
        e.hurtMarked = true;
    }

    private static void setOnFire(LivingEntity e, int seconds) {
        e.setRemainingFireTicks(Math.max(e.getRemainingFireTicks(), seconds * 20));
    }

    /** Dòng thất bại: nhiệt độ đạt được và mốc kế tiếp (hoặc tiến độ chuyển pha nếu đang ở bình nguyên). */
    private static Component failLine(Component name, Material m, double tempC, double progressJ, String mana) {
        double latent = m.latentAt(tempC) * m.coreMassKg();
        if (latent > 0 && progressJ >= 1) {
            return Component.translatable("phapthuat.heat.none_phase", name, Fmt.tempC(tempC),
                    Fmt.num(100 * Math.min(1, progressJ / latent), 0), mana);
        }
        double next = nextThresholdC(m, tempC);
        if (Double.isNaN(next)) return Component.translatable("phapthuat.heat.none", name, Fmt.tempC(tempC), mana);
        return Component.translatable("phapthuat.heat.none_target", name, Fmt.tempC(tempC), Fmt.tempC(next), mana);
    }

    private static double nextThresholdC(Material m, double tempC) {
        for (double t : new double[] {m.ignitionC(), m.meltC(), m.boilC()}) {
            if (!Double.isNaN(t) && t > tempC + 1e-9) return t;
        }
        return Double.NaN;
    }

    private static Component materialName(Material m) {
        return Component.translatable("phapthuat.material." + m.id());
    }
}
