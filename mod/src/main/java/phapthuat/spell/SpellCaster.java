package phapthuat.spell;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import phapthuat.Config;
import phapthuat.entity.SpellProjectile;
import phapthuat.mana.ManaManager;

/** Thi triển một phép đã khắc: trả mana, chọn mục tiêu (hoặc bắn đạn), áp phép, báo kết quả lên thanh hành động. */
public final class SpellCaster {
    private SpellCaster() {}

    public static CastReport cast(ServerPlayer player, InscribedSpell inscribed) {
        Optional<Spell> found = SpellRegistry.get(inscribed.spellId());
        if (found.isEmpty()) return send(player, fail("phapthuat.cast.unknown"));
        Spell spell = found.get();
        if (inscribed.shape() == Shape.DAN) {
            double speed = inscribed.param(Targeting.shapeParam(spell, Shape.DAN, "speed"));
            // Đạn phép tốn thêm động năng lúc phóng (spec 6.5).
            double launchMana = SpellProjectile.launchEnergyJ(speed) / Config.joulesPerMana();
            if (!ManaManager.trySpend(player, spell.upfrontCostMana(inscribed) + launchMana)) {
                return send(player, fail("phapthuat.cast.no_mana"));
            }
            SpellProjectile.launch(player, inscribed, speed);
            return send(player, new CastReport(true, Component.translatable("phapthuat.cast.launched",
                    spell.displayName(), Fmt.num(speed, 1))));
        }
        // Chọn mục tiêu trước rồi mới trừ mana: chi phí của một số phép (Nâng) phụ thuộc mục tiêu.
        Targeting.Target target = Targeting.resolve(player, inscribed, spell);
        if (!ManaManager.trySpend(player, spell.upfrontCostMana(inscribed, player.serverLevel(), target.units()))) {
            return send(player, fail("phapthuat.cast.no_mana"));
        }
        return send(player, apply(player, spell, inscribed, target.units(), target.center(), target.face()));
    }

    /** Trả mana rồi áp phép lên các đơn vị cho sẵn (bỏ qua bước chọn mục tiêu). Dùng cho GameTest. */
    public static CastReport castOnUnits(ServerPlayer player, InscribedSpell inscribed, List<SpellUnit> units,
            Vec3 center, @Nullable Direction face) {
        Optional<Spell> found = SpellRegistry.get(inscribed.spellId());
        if (found.isEmpty()) return send(player, fail("phapthuat.cast.unknown"));
        Spell spell = found.get();
        List<SpellUnit> affected = Targeting.filter(player.serverLevel(), units, spell, inscribed.shape());
        if (!ManaManager.trySpend(player, spell.upfrontCostMana(inscribed, player.serverLevel(), affected))) {
            return send(player, fail("phapthuat.cast.no_mana"));
        }
        return send(player, apply(player, spell, inscribed, affected, center, face));
    }

    /** Áp phép mà không trả mana (đạn phép đã trả lúc bắn). Đơn vị được lọc theo {@link Spell#affects}. */
    public static CastReport applyOnUnits(ServerPlayer player, InscribedSpell inscribed, List<SpellUnit> units,
            Vec3 center, @Nullable Direction face) {
        Optional<Spell> found = SpellRegistry.get(inscribed.spellId());
        if (found.isEmpty()) return send(player, fail("phapthuat.cast.unknown"));
        Spell spell = found.get();
        return send(player, apply(player, spell, inscribed,
                Targeting.filter(player.serverLevel(), units, spell, inscribed.shape()), center, face));
    }

    private static CastReport apply(ServerPlayer player, Spell spell, InscribedSpell inscribed,
            List<SpellUnit> units, Vec3 center, @Nullable Direction face) {
        CastContext ctx = new CastContext(player.serverLevel(), player, inscribed, Config.joulesPerMana(), center,
                face);
        return spell.apply(ctx, units);
    }

    private static CastReport fail(String key) {
        return new CastReport(false, Component.translatable(key));
    }

    private static CastReport send(ServerPlayer player, CastReport report) {
        player.displayClientMessage(report.message(), true);
        return report;
    }
}
