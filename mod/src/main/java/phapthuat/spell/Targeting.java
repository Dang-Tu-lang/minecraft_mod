package phapthuat.spell;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import phapthuat.physics.ShapeMath;
import phapthuat.spell.SpellUnit.AirUnit;
import phapthuat.spell.SpellUnit.BlockUnit;
import phapthuat.spell.SpellUnit.EntityUnit;
import phapthuat.util.Raycast;

/** Biến thao tác + hướng nhìn thành danh sách đơn vị chịu tác động (spec mục 6.5). */
public final class Targeting {
    public static final double RANGE = 16;

    public record Aim(Vec3 point, BlockPos cell, @Nullable Direction face, @Nullable Entity entity, boolean miss) {}

    public record Target(List<SpellUnit> units, Vec3 center, @Nullable Direction face) {}

    private Targeting() {}

    /** Chỗ người chơi đang nhìn, tầm 16 khối. Nhìn vào khoảng không thì là ô không khí ở 16 khối (kẹp trong thế giới). */
    public static Aim aim(ServerPlayer player) {
        HitResult hit = Raycast.look(player, RANGE);
        if (hit instanceof EntityHitResult eh) {
            return new Aim(eh.getLocation(), eh.getEntity().blockPosition(), null, eh.getEntity(), false);
        }
        if (hit instanceof BlockHitResult bh && hit.getType() != HitResult.Type.MISS) {
            return new Aim(bh.getLocation(), bh.getBlockPos().immutable(), bh.getDirection(), null, false);
        }
        Vec3 point = player.getEyePosition().add(player.getViewVector(1.0f).scale(RANGE));
        ServerLevel level = player.serverLevel();
        double y = Math.max(level.getMinBuildHeight(), Math.min(level.getMaxBuildHeight() - 1, point.y));
        Vec3 clamped = new Vec3(point.x, y, point.z);
        return new Aim(clamped, BlockPos.containing(clamped), null, null, true);
    }

    public static Target resolve(ServerPlayer player, InscribedSpell inscribed, Spell spell) {
        ServerLevel level = player.serverLevel();
        Shape shape = inscribed.shape();
        Aim aim = shape == Shape.BAN_THAN ? null : aim(player);
        List<SpellUnit> raw = switch (shape) {
            case DIEM -> List.of(pointUnit(level, aim));
            case TIA -> rayUnits(player, paramOf(spell, inscribed, shape, "length"));
            case DUONG -> pathUnits(player, aim.cell());
            case CAU -> sphereUnits(level, aim.cell(), paramOf(spell, inscribed, shape, "radius"));
            case TUONG -> wallUnits(level, wallCenter(aim), (int) Math.round(paramOf(spell, inscribed, shape, "width")),
                    (int) Math.round(paramOf(spell, inscribed, shape, "height")), facingAlongX(player));
            case BAN_THAN -> List.of(new EntityUnit(player));
            case DAN -> throw new IllegalStateException("Đạn phép được bắn bởi SpellCaster, không chọn mục tiêu trước");
        };
        List<SpellUnit> units = filter(level, raw, spell, shape);
        Vec3 center = aim == null ? player.position() : aim.point();
        return new Target(units, center, aim == null ? null : aim.face());
    }

    public static List<SpellUnit> filter(ServerLevel level, List<SpellUnit> units, Spell spell, Shape shape) {
        return units.stream().filter(u -> spell.affects(level, u, shape)).toList();
    }

    private static double paramOf(Spell spell, InscribedSpell inscribed, Shape shape, String key) {
        return inscribed.param(shapeParam(spell, shape, key));
    }

    /** Thông số {@code key} của thao tác, theo khoảng mà phép này quy định ({@link Spell#shapeParams}). */
    public static SpellParam shapeParam(Spell spell, Shape shape, String key) {
        for (SpellParam p : spell.shapeParams(shape)) {
            if (p.key().equals(key)) return p;
        }
        throw new IllegalArgumentException("Thao tác " + shape + " không có thông số " + key);
    }

    private static SpellUnit pointUnit(ServerLevel level, Aim aim) {
        if (aim.entity() != null) return new EntityUnit(aim.entity());
        return level.getBlockState(aim.cell()).isAir() ? new AirUnit(aim.cell()) : new BlockUnit(aim.cell());
    }

    private static SpellUnit cellUnit(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() ? new AirUnit(pos) : new BlockUnit(pos);
    }

    public static List<SpellUnit> sphereUnits(ServerLevel level, BlockPos center, double radius) {
        List<SpellUnit> units = new ArrayList<>();
        Set<BlockPos> cells = new HashSet<>();
        for (ShapeMath.Offset o : ShapeMath.sphere(radius)) {
            BlockPos pos = center.offset(o.dx(), o.dy(), o.dz());
            if (!level.isInWorldBounds(pos)) continue;
            cells.add(pos);
            units.add(cellUnit(level, pos));
        }
        addEntitiesIn(level, cells, new AABB(center).inflate(radius + 1), units);
        return units;
    }

    public static List<SpellUnit> wallUnits(ServerLevel level, BlockPos center, int width, int height,
            boolean facingAlongX) {
        List<SpellUnit> units = new ArrayList<>();
        Set<BlockPos> cells = new HashSet<>();
        for (ShapeMath.Offset o : ShapeMath.wall(width, height, facingAlongX)) {
            BlockPos pos = center.offset(o.dx(), o.dy(), o.dz());
            if (!level.isInWorldBounds(pos)) continue;
            cells.add(pos);
            units.add(cellUnit(level, pos));
        }
        addEntitiesIn(level, cells, new AABB(center).inflate(Math.max(width, height)), units);
        return units;
    }

    /** Tia thẳng từ mắt: mọi thực thể trên đường và khối rắn đầu tiên chặn đường (tia dừng ở đó). */
    public static List<SpellUnit> rayUnits(ServerPlayer player, double length) {
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0f).scale(length));
        BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                player));
        Vec3 stop = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        List<SpellUnit> units = new ArrayList<>();
        for (Entity e : level.getEntities(player, new AABB(eye, stop).inflate(1.0), e -> !e.isSpectator())) {
            if (e.getBoundingBox().inflate(0.3).clip(eye, stop).isPresent()) units.add(new EntityUnit(e));
        }
        if (block.getType() != HitResult.Type.MISS) units.add(new BlockUnit(block.getBlockPos().immutable()));
        return units;
    }

    /** Vệt trên mặt đất từ chân người chơi (bỏ ô người chơi đang đứng) tới điểm nhìn: khối mặt trên và sinh vật trên đó. */
    public static List<SpellUnit> pathUnits(ServerPlayer player, BlockPos targetCell) {
        ServerLevel level = player.serverLevel();
        BlockPos feet = player.blockPosition();
        List<SpellUnit> units = new ArrayList<>();
        Set<BlockPos> above = new HashSet<>();
        List<ShapeMath.Column> columns = ShapeMath.groundLine(feet.getX(), feet.getZ(), targetCell.getX(),
                targetCell.getZ());
        for (ShapeMath.Column c : columns.subList(Math.min(1, columns.size()), columns.size())) {
            for (int y = feet.getY() + 2; y >= feet.getY() - 4; y--) {
                BlockPos pos = new BlockPos(c.x(), y, c.z());
                if (!level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()) {
                    units.add(new BlockUnit(pos));
                    above.add(pos.above());
                    break;
                }
            }
        }
        AABB box = new AABB(feet).minmax(new AABB(targetCell)).inflate(2);
        addEntitiesIn(level, above, box, units);
        return units;
    }

    private static void addEntitiesIn(ServerLevel level, Set<BlockPos> cells, AABB box, List<SpellUnit> units) {
        for (Entity e : level.getEntities((Entity) null, box, e -> !e.isSpectator())) {
            if (cells.contains(e.blockPosition())) units.add(new EntityUnit(e));
        }
    }

    private static BlockPos wallCenter(Aim aim) {
        if (aim.face() != null) return aim.cell().relative(aim.face());
        return aim.cell();
    }

    private static boolean facingAlongX(ServerPlayer player) {
        Vec3 look = player.getViewVector(1.0f);
        return Math.abs(look.x) >= Math.abs(look.z);
    }
}
