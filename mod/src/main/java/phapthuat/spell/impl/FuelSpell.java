package phapthuat.spell.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import phapthuat.physics.CombustionCalc;
import phapthuat.physics.Element;
import phapthuat.spell.CastContext;
import phapthuat.spell.CastReport;
import phapthuat.spell.Fmt;
import phapthuat.spell.InscribedSpell;
import phapthuat.spell.Shape;
import phapthuat.spell.Spell;
import phapthuat.spell.SpellParam;
import phapthuat.spell.SpellUnit;

/**
 * Đốt Nhiên Liệu (spec 8.7): nhiệt của phản ứng cháy thay cho mana, cần oxy theo quy tắc Thornton. Thiếu oxy trong
 * vùng kín thì cháy không hết và sinh khí CO — người đứng trong vùng cũng trúng độc.
 */
public final class FuelSpell implements Spell {
    public static final String ID = "dot_nhien_lieu";
    public static final SpellParam FUEL = SpellParam.choice("fuel",
            FuelChoices.all().stream().map(k -> "phapthuat.fuel." + k.id()).toList(), 0);
    public static final SpellParam COUNT = new SpellParam("count", "phapthuat.unit.items", 1, 16, 1);
    /** Phí kích hoạt, mana. */
    private static final double ACTIVATION_MANA = 1;
    private static final int CO_DURATION_TICKS = 200;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Element element() {
        return Element.HOA;
    }

    @Override
    public Set<Shape> shapes() {
        return EnumSet.of(Shape.DIEM, Shape.TIA, Shape.DUONG, Shape.CAU, Shape.TUONG, Shape.DAN);
    }

    @Override
    public List<SpellParam> params() {
        return List.of(FUEL, COUNT);
    }

    @Override
    public double upfrontCostMana(InscribedSpell spell) {
        return ACTIVATION_MANA;
    }

    @Override
    public boolean affects(ServerLevel level, SpellUnit unit, Shape shape) {
        return HeatSpell.affectsHeat(level, unit, shape);
    }

    @Override
    public CastReport apply(CastContext ctx, List<SpellUnit> units) {
        int count = (int) Math.round(ctx.spell().param(COUNT));
        int kind = (int) Math.round(ctx.spell().param(FUEL));
        List<ItemStack> fuel = takeFuel(ctx.caster(), kind, count);
        if (fuel.isEmpty()) {
            return new CastReport(false, Component.translatable("phapthuat.fuel.missing", displayName(), count,
                    Component.translatable(FuelChoices.langKey(kind))));
        }
        double energyJ = 0;
        for (ItemStack stack : fuel) energyJ += CombustionCalc.itemEnergyJ(FuelChoices.burnTicks(stack));
        BlockPos centerCell = ctx.airCell();
        int cells = countAir(ctx.level(), centerCell);
        CombustionCalc.Combustion burn = CombustionCalc.burn(energyJ, cells);
        CastReport heat = HeatSpell.applyHeat(ctx, units, burn.releasedJ(), displayName());
        if (burn.carbonMonoxide()) carbonMonoxide(ctx, centerCell, cells);
        Component header = burn.carbonMonoxide()
                ? Component.translatable("phapthuat.fuel.co", displayName(), count,
                        Component.translatable(FuelChoices.langKey(kind)), Fmt.num(energyJ / 1e6, 2),
                        Fmt.num(100 * burn.releasedJ() / energyJ, 0), Fmt.num(cells * 0.276, 2))
                : Component.translatable("phapthuat.fuel.done", displayName(), count,
                        Component.translatable(FuelChoices.langKey(kind)), Fmt.num(energyJ / 1e6, 2));
        return new CastReport(heat.success(), header.copy().append(Component.literal(" — ")).append(heat.message()));
    }

    /** Lấy đúng {@code count} vật phẩm khớp từ túi đồ, nhiều chồng cũng được; chế độ sáng tạo không trừ. */
    private static List<ItemStack> takeFuel(ServerPlayer player, int kind, int count) {
        List<ItemStack> taken = new ArrayList<>();
        if (player.hasInfiniteMaterials()) {
            ItemStack sample = new ItemStack(net.minecraft.world.item.Items.COAL);
            taken.add(sample);
            return taken;
        }
        int missing = count;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && missing > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!FuelChoices.matches(kind, stack)) continue;
            int take = Math.min(missing, stack.getCount());
            taken.add(stack.copyWithCount(take));
            stack.shrink(take);
            missing -= take;
        }
        if (missing > 0) {
            // Thiếu thì trả lại những gì đã lấy: không trừ gì cả.
            for (ItemStack stack : taken) player.getInventory().add(stack);
            return List.of();
        }
        player.getInventory().setChanged();
        return taken;
    }

    /** Đếm ô không khí liên thông với tâm (flood fill 6 hướng), dừng ở 64 (spec 8.7). */
    private static int countAir(ServerLevel level, BlockPos start) {
        if (!level.getBlockState(start).isAir()) return 0;
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && seen.size() < CombustionCalc.OPEN_AIR_CELLS) {
            BlockPos pos = queue.poll();
            for (Direction d : Direction.values()) {
                BlockPos next = pos.relative(d);
                if (seen.contains(next) || !level.isInWorldBounds(next)) continue;
                if (!level.getBlockState(next).isAir()) continue;
                seen.add(next);
                queue.add(next);
                if (seen.size() >= CombustionCalc.OPEN_AIR_CELLS) break;
            }
        }
        return seen.size();
    }

    /** Khí CO: mọi sinh vật trong vùng kín (kể cả người chơi) buồn nôn và trúng độc 10 giây. */
    private static void carbonMonoxide(CastContext ctx, BlockPos center, int cells) {
        ServerLevel level = ctx.level();
        Vec3 point = Vec3.atCenterOf(center);
        double radius = Math.cbrt(cells) + 1;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(point, point).inflate(radius),
                e -> e.isAlive() && !e.getType().is(net.minecraft.tags.EntityTypeTags.UNDEAD))) {
            e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, CO_DURATION_TICKS, 0, false, false));
            e.addEffect(new MobEffectInstance(MobEffects.POISON, CO_DURATION_TICKS, 0, false, false));
        }
    }

    /** Nhiên liệu của một vật phẩm, J — dùng cho sách và Máy Đo. */
    public static double itemEnergyJ(ItemStack stack) {
        return CombustionCalc.itemEnergyJ(FuelChoices.burnTicks(stack));
    }
}
