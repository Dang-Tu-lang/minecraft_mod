package phapthuat.gametest;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import phapthuat.PhapThuat;
import phapthuat.entity.SpellProjectile;
import phapthuat.spell.InscribedSpell;
import phapthuat.spell.Shape;
import phapthuat.spell.SpellRegistry;
import phapthuat.spell.SpellUnit;
import phapthuat.spell.Targeting;

@GameTestHolder(PhapThuat.MODID)
@PrefixGameTestTemplate(false)
public class TargetingGameTests {
    static ServerPlayer playerAt(GameTestHelper helper, double x, double y, double z, float yaw, float pitch) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(x, y, z));
        player.moveTo(pos.x, pos.y, pos.z, yaw, pitch);
        return player;
    }

    @GameTest(template = ThermalGameTests.EMPTY)
    public static void sphereResolvesSevenCells(GameTestHelper helper) {
        List<SpellUnit> units = Targeting.sphereUnits(helper.getLevel(), helper.absolutePos(new BlockPos(3, 3, 3)), 1);
        helper.assertTrue(units.size() == 7, "cầu r = 1 phải có 7 ô, có " + units.size());
        helper.succeed();
    }

    /** GameTest dựng rào chắn bao quanh vùng thử (cả trần); dọn cột phía trên để nhìn thấy bầu trời. */
    static void clearSkyAbove(GameTestHelper helper, int x, int z) {
        for (int y = 1; y <= 30; y++) {
            helper.getLevel().setBlock(helper.absolutePos(new BlockPos(x, y, z)), Blocks.AIR.defaultBlockState(), 3);
        }
    }

    @GameTest(template = ThermalGameTests.EMPTY)
    public static void pointAtSkyGivesAirAt16(GameTestHelper helper) {
        SpellRegistry.register(ProbeSpell.INSTANCE);
        clearSkyAbove(helper, 3, 3);
        ServerPlayer player = playerAt(helper, 3.5, 1, 3.5, 0, -90);
        var target = Targeting.resolve(player, new InscribedSpell(ProbeSpell.ID, Shape.DIEM, Map.of()),
                ProbeSpell.INSTANCE);
        helper.assertTrue(target.units().size() == 1 && target.units().getFirst() instanceof SpellUnit.AirUnit,
                "phải có đúng 1 ô không khí, có " + target.units());
        BlockPos cell = ((SpellUnit.AirUnit) target.units().getFirst()).pos();
        double expectedY = player.getEyeY() + Targeting.RANGE;
        helper.assertTrue(Math.abs(cell.getY() + 0.5 - expectedY) <= 1, "ô không khí ở y = " + cell.getY()
                + ", mong đợi ≈ " + expectedY);
        helper.succeed();
    }

    @GameTest(template = ThermalGameTests.EMPTY, timeoutTicks = 260)
    public static void projectileDespawnsAfter10s(GameTestHelper helper) {
        SpellRegistry.register(ProbeSpell.INSTANCE);
        clearSkyAbove(helper, 3, 3);
        ServerPlayer player = playerAt(helper, 3.5, 1, 3.5, 0, -90);
        SpellProjectile p = SpellProjectile.launch(player,
                new InscribedSpell(ProbeSpell.ID, Shape.DAN, Map.of("speed", 100.0)), 100);
        helper.runAfterDelay(SpellProjectile.LIFETIME_TICKS + 2, () -> {
            helper.assertTrue(p.isRemoved(), "đạn phép vẫn còn sau 10 giây");
            helper.succeed();
        });
    }

    @GameTest(template = ThermalGameTests.EMPTY, timeoutTicks = 80)
    public static void projectileFallsBackOnCaster(GameTestHelper helper) {
        SpellRegistry.register(ProbeSpell.INSTANCE);
        clearSkyAbove(helper, 3, 3);
        ServerPlayer player = playerAt(helper, 3.5, 1, 3.5, 0, -90);
        SpellProjectile.launch(player, new InscribedSpell(ProbeSpell.ID, Shape.DAN, Map.of("speed", 6.0)), 6);
        helper.succeedWhen(() -> helper.assertTrue(ProbeSpell.HITS.stream().anyMatch(h -> h.caster() == player
                && h.units().stream().anyMatch(u -> u instanceof SpellUnit.EntityUnit e && e.entity() == player)),
                "đạn chưa rơi trúng người bắn"));
    }
}
