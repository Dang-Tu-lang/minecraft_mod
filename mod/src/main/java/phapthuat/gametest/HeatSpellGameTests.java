package phapthuat.gametest;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import phapthuat.PhapThuat;
import phapthuat.physics.Materials;
import phapthuat.spell.InscribedSpell;
import phapthuat.spell.Shape;
import phapthuat.spell.SpellCaster;
import phapthuat.spell.SpellUnit;
import phapthuat.spell.Targeting;
import phapthuat.spell.impl.HeatSpell;
import phapthuat.thermal.WorldTemps;

/** Các ví dụ số của spec 8.1, ở biome có T_mt = 20°C. */
@GameTestHolder(PhapThuat.MODID)
@PrefixGameTestTemplate(false)
public class HeatSpellGameTests {
    /** Tầng y = 1 là nền đá của khuôn thử (GameTest dọn tầng y = 0 thành không khí), nên mục tiêu nằm trong nền. */
    private static final BlockPos TARGET = new BlockPos(3, 1, 3);
    private static final BlockPos FLOOR_NEIGHBOR = TARGET.north();

    private static ServerPlayer caster(GameTestHelper helper) {
        return TargetingGameTests.playerAt(helper, 0.5, 1, 0.5, 0, 0);
    }

    private static void requireAmbient20(GameTestHelper helper) {
        double ambient = WorldTemps.ambientC(helper.getLevel(), helper.absolutePos(TARGET));
        helper.assertTrue(Math.abs(ambient - 20) < 0.01, "biome của GameTest phải có T_mt = 20°C, có " + ambient);
    }

    private static void heat(GameTestHelper helper, ServerPlayer player, BlockPos rel, double mana, Direction face) {
        BlockPos abs = helper.absolutePos(rel);
        SpellCaster.castOnUnits(player, new InscribedSpell(HeatSpell.ID, Shape.DIEM, Map.of("energy", mana)),
                List.of(new SpellUnit.BlockUnit(abs)), Vec3.atCenterOf(abs), face);
    }

    @GameTest(template = ThermalGameTests.EMPTY)
    public static void woodIgnitesWith33_4Mana(GameTestHelper helper) {
        requireAmbient20(helper);
        helper.setBlock(TARGET, Blocks.OAK_PLANKS);
        heat(helper, caster(helper), TARGET, 33.4, Direction.UP);
        helper.assertTrue(helper.getBlockState(TARGET.above()).is(BlockTags.FIRE), "gỗ phải bốc cháy");
        helper.succeed();
    }

    @GameTest(template = ThermalGameTests.EMPTY)
    public static void woodShortKeepsHeat(GameTestHelper helper) {
        requireAmbient20(helper);
        helper.setBlock(TARGET, Blocks.OAK_PLANKS);
        ServerPlayer player = caster(helper);
        heat(helper, player, TARGET, 31.6, Direction.UP);
        helper.assertBlockNotPresent(Blocks.FIRE, TARGET.above());
        double temp = WorldTemps.currentBlock(helper.getLevel(), helper.absolutePos(TARGET), Materials.WOOD).tempC();
        helper.assertTrue(Math.abs(temp - 285.5) < 0.5, "lõi gỗ phải ≈ 285,5°C, có " + temp);
        heat(helper, player, TARGET, 2, Direction.UP);
        helper.assertTrue(helper.getBlockState(TARGET.above()).is(BlockTags.FIRE), "lần thứ hai phải đủ để cháy");
        helper.succeed();
    }

    @GameTest(template = ThermalGameTests.EMPTY)
    public static void stoneCracksWith5_4Mana(GameTestHelper helper) {
        requireAmbient20(helper);
        helper.setBlock(TARGET, Blocks.STONE);
        heat(helper, caster(helper), TARGET, 5.4, Direction.UP);
        helper.assertBlockPresent(Blocks.AIR, TARGET);
        helper.assertItemEntityPresent(Items.COBBLESTONE, TARGET, 2.0);
        helper.succeed();
    }

    @GameTest(template = ThermalGameTests.EMPTY)
    public static void stoneMeltsWith363Mana(GameTestHelper helper) {
        requireAmbient20(helper);
        helper.setBlock(TARGET, Blocks.STONE);
        heat(helper, caster(helper), TARGET, 363, Direction.UP);
        helper.assertBlockPresent(Blocks.LAVA, TARGET);
        helper.succeed();
    }

    @GameTest(template = ThermalGameTests.EMPTY)
    public static void iceMeltsWith30_7Mana(GameTestHelper helper) {
        requireAmbient20(helper);
        helper.setBlock(TARGET, Blocks.ICE);
        heat(helper, caster(helper), TARGET, 30.7, Direction.UP);
        helper.assertBlockPresent(Blocks.WATER, TARGET);
        helper.succeed();
    }

    @GameTest(template = ThermalGameTests.EMPTY)
    public static void waterVaporizesWith259_3Mana(GameTestHelper helper) {
        requireAmbient20(helper);
        helper.setBlock(TARGET, Blocks.WATER);
        heat(helper, caster(helper), TARGET, 259.3, Direction.UP);
        helper.assertBlockPresent(Blocks.AIR, TARGET);
        helper.assertBlockPresent(Blocks.STONE, FLOOR_NEIGHBOR);
        helper.succeed();
    }

    @GameTest(template = ThermalGameTests.EMPTY)
    public static void steamBombWith359_3Mana(GameTestHelper helper) {
        requireAmbient20(helper);
        helper.setBlock(TARGET, Blocks.WATER);
        helper.assertBlockPresent(Blocks.STONE, FLOOR_NEIGHBOR);
        heat(helper, caster(helper), TARGET, 359.3, Direction.UP);
        helper.assertBlockNotPresent(Blocks.WATER, TARGET);
        helper.assertBlockNotPresent(Blocks.STONE, FLOOR_NEIGHBOR);
        helper.succeed();
    }

    @GameTest(template = ThermalGameTests.EMPTY)
    public static void tntPrimed(GameTestHelper helper) {
        requireAmbient20(helper);
        helper.setBlock(TARGET, Blocks.TNT);
        heat(helper, caster(helper), TARGET, 42, Direction.UP);
        helper.assertEntityPresent(EntityType.TNT);
        ServerLevel level = helper.getLevel();
        level.getEntities(EntityType.TNT, new AABB(helper.absolutePos(TARGET)).inflate(4), e -> true)
                .forEach(e -> e.discard());
        helper.succeed();
    }

    @GameTest(template = ThermalGameTests.EMPTY)
    public static void noFireUnderwater(GameTestHelper helper) {
        requireAmbient20(helper);
        helper.setBlock(TARGET, Blocks.OAK_PLANKS);
        helper.setBlock(TARGET.above(), Blocks.WATER);
        heat(helper, caster(helper), TARGET, 40, Direction.UP);
        helper.assertBlockPresent(Blocks.WATER, TARGET.above());
        helper.assertBlockPresent(Blocks.OAK_PLANKS, TARGET);
        helper.succeed();
    }

    @GameTest(template = ThermalGameTests.EMPTY)
    public static void airBurstBreaksGlass(GameTestHelper helper) {
        requireAmbient20(helper);
        BlockPos center = new BlockPos(3, 3, 3);
        BlockPos glass = new BlockPos(3, 3, 5);
        helper.setBlock(glass, Blocks.GLASS);
        BlockPos abs = helper.absolutePos(center);
        var units = Targeting.sphereUnits(helper.getLevel(), abs, 1);
        SpellCaster.castOnUnits(caster(helper),
                new InscribedSpell(HeatSpell.ID, Shape.CAU, Map.of("energy", 50.0, "radius", 1.0)), units,
                Vec3.atCenterOf(abs), null);
        helper.assertBlockPresent(Blocks.AIR, glass);
        helper.succeed();
    }

    @GameTest(template = ThermalGameTests.EMPTY)
    public static void backlashBurnsCaster(GameTestHelper helper) {
        requireAmbient20(helper);
        helper.setBlock(TARGET, Blocks.OAK_PLANKS);
        ServerPlayer player = TargetingGameTests.playerAt(helper, 4.5, 1, 3.5, 90, 0);
        heat(helper, player, TARGET, 100, Direction.UP);
        helper.assertTrue(player.getRemainingFireTicks() > 0, "người chơi đứng sát phải bị bỏng vì phản tác dụng");
        helper.succeed();
    }
}
