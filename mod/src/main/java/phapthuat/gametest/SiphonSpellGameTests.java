package phapthuat.gametest;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.world.entity.monster.Blaze;
import phapthuat.Config;
import phapthuat.PhapThuat;
import phapthuat.mana.ManaManager;
import phapthuat.physics.Materials;
import phapthuat.spell.CastContext;
import phapthuat.spell.InscribedSpell;
import phapthuat.spell.Shape;
import phapthuat.spell.Spell;
import phapthuat.spell.SpellCaster;
import phapthuat.spell.SpellRegistry;
import phapthuat.spell.SpellUnit;
import phapthuat.spell.impl.SiphonSpell;
import phapthuat.thermal.WorldTemps;

/** Các ví dụ số của spec 8.2, ở biome có T_mt = 20°C. */
@GameTestHolder(PhapThuat.MODID)
@PrefixGameTestTemplate(false)
public class SiphonSpellGameTests {
    /** Tầng y = 1 là nền đá của khuôn thử. */
    private static final BlockPos TARGET = new BlockPos(3, 1, 3);

    private static ServerPlayer caster(GameTestHelper helper, double mana) {
        ServerPlayer player = TargetingGameTests.playerAt(helper, 0.5, 2, 0.5, 0, 0);
        ManaManager.set(player, mana);
        return player;
    }

    private static void siphon(GameTestHelper helper, ServerPlayer player, BlockPos rel, double heatMana) {
        BlockPos abs = helper.absolutePos(rel);
        SpellCaster.castOnUnits(player, new InscribedSpell(SiphonSpell.ID, Shape.DIEM, Map.of("heat", heatMana)),
                List.of(new SpellUnit.BlockUnit(abs)), Vec3.atCenterOf(abs), null);
    }

    private static void near(GameTestHelper helper, double expected, double actual, double tolerance, String what) {
        helper.assertTrue(Math.abs(expected - actual) <= tolerance, what + ": mong đợi ≈ " + expected + ", có " + actual);
    }

    @GameTest(template = ThermalGameTests.EMPTY)
    public static void lavaFullDrainGivesObsidianAndMana(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.LAVA);
        ServerPlayer player = caster(helper, 500);
        siphon(helper, player, TARGET, 500);
        helper.assertBlockPresent(Blocks.OBSIDIAN, TARGET);
        near(helper, 500 - 1 + 392.8, ManaManager.get(player), 1, "mana sau khi rút hết dung nham");
        helper.succeed();
    }

    @GameTest(template = ThermalGameTests.EMPTY)
    public static void lavaPartialStaysLavaAndYieldsLess(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.LAVA);
        ServerPlayer player = caster(helper, 500);
        siphon(helper, player, TARGET, 100);
        double afterFirst = ManaManager.get(player);
        siphon(helper, player, TARGET, 100);
        double afterSecond = ManaManager.get(player);
        helper.assertBlockPresent(Blocks.LAVA, TARGET);
        double gainFirst = afterFirst - (500 - 1);
        double gainSecond = afterSecond - (afterFirst - 1);
        helper.assertTrue(gainFirst > 0, "lần đầu phải nhận mana, nhận " + gainFirst);
        helper.assertTrue(gainSecond <= gainFirst + 1e-6, "lần 2 nhận " + gainSecond + " > lần 1 " + gainFirst);
        helper.succeed();
    }

    @GameTest(template = ThermalGameTests.EMPTY)
    public static void waterFreezes(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.WATER);
        ServerPlayer player = caster(helper, 500);
        siphon(helper, player, TARGET, 41.8);
        helper.assertBlockPresent(Blocks.ICE, TARGET);
        near(helper, 500 - 1 - 3.1, ManaManager.get(player), 0.1, "mana sau khi làm đông nước");
        helper.succeed();
    }

    @GameTest(template = ThermalGameTests.EMPTY)
    public static void fireExtinguished(GameTestHelper helper) {
        BlockPos fire = TARGET.above();
        helper.setBlock(fire, Blocks.FIRE);
        siphon(helper, caster(helper, 500), fire, 5);
        helper.assertBlockPresent(Blocks.AIR, fire);
        helper.succeed();
    }

    @GameTest(template = ThermalGameTests.EMPTY)
    public static void overdrawFreezesCaster(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.WATER);
        ServerPlayer player = caster(helper, 1.5);
        siphon(helper, player, TARGET, 500);
        near(helper, 0, ManaManager.get(player), 1e-9, "mana sau khi rút quá sức");
        helper.assertTrue(player.getTicksFrozen() > 0, "người thi triển phải bị đóng băng");
        helper.succeed();
    }

    /** Review: rút đúng 108 (= nhiệt đông đặc) hai lần không được cho mana vô hạn; lần 2 phải làm dung nham nguội. */
    @GameTest(template = ThermalGameTests.EMPTY)
    public static void lavaExactLatentThenCools(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.LAVA);
        ServerPlayer player = caster(helper, 500);
        siphon(helper, player, TARGET, 108);
        siphon(helper, player, TARGET, 108);
        helper.assertBlockPresent(Blocks.LAVA, TARGET);
        double temp = WorldTemps.currentBlock(helper.getLevel(), helper.absolutePos(TARGET), Materials.LAVA).tempC();
        helper.assertTrue(temp < 900, "sau lần rút thứ 2 dung nham phải ≈ 866,7°C, có " + temp);
        helper.succeed();
    }

    /** Spec 7.3: mob miễn lửa miễn phép nhiệt. */
    @GameTest(template = ThermalGameTests.EMPTY)
    public static void siphonIgnoresFireImmune(GameTestHelper helper) {
        Blaze blaze = helper.spawn(EntityType.BLAZE, TARGET.above());
        Spell spell = SpellRegistry.get(SiphonSpell.ID).orElseThrow();
        helper.assertFalse(spell.affects(helper.getLevel(), new SpellUnit.EntityUnit(blaze), Shape.DIEM),
                "quỷ lửa miễn lửa không được chịu Thu Nhiệt");
        helper.succeed();
    }

    /** Khối đổi giữa chừng (không còn là mục tiêu rút nhiệt) phải bị bỏ qua, không crash, không tốn mana. */
    @GameTest(template = ThermalGameTests.EMPTY)
    public static void siphonSkipsNonTargetsWithoutCrash(GameTestHelper helper) {
        BlockPos obsidian = TARGET.east();
        helper.setBlock(TARGET, Blocks.DIRT);
        helper.setBlock(obsidian, Blocks.OBSIDIAN);
        ServerPlayer player = caster(helper, 500);
        InscribedSpell inscribed = new InscribedSpell(SiphonSpell.ID, Shape.CAU, Map.of("heat", 500.0, "radius", 1.0));
        CastContext ctx = new CastContext(helper.getLevel(), player, inscribed, Config.joulesPerMana(),
                Vec3.atCenterOf(helper.absolutePos(TARGET)), null);
        SpellRegistry.get(SiphonSpell.ID).orElseThrow().apply(ctx, List.of(
                new SpellUnit.BlockUnit(helper.absolutePos(TARGET)), new SpellUnit.BlockUnit(helper.absolutePos(obsidian))));
        near(helper, 500, ManaManager.get(player), 1e-9, "mana khi không có mục tiêu hợp lệ");
        helper.assertBlockPresent(Blocks.OBSIDIAN, obsidian);
        helper.succeed();
    }

    @GameTest(template = ThermalGameTests.EMPTY, timeoutTicks = 60)
    public static void coldCoreSlowsAndHurtsPig(GameTestHelper helper) {
        Pig pig = helper.spawn(EntityType.PIG, TARGET.above());
        WorldTemps.saveEntity(pig, WorldTemps.LIVING_BODY_C - 7, 0);
        helper.succeedWhen(() -> {
            helper.assertTrue(pig.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "lợn lạnh 7 K phải chậm chạp");
            helper.assertTrue(pig.getHealth() < pig.getMaxHealth(), "lợn lạnh 7 K phải mất máu");
        });
    }
}
