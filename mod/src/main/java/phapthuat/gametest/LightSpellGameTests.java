package phapthuat.gametest;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import phapthuat.PhapThuat;
import phapthuat.mana.ManaManager;
import phapthuat.spell.InscribedSpell;
import phapthuat.spell.Shape;
import phapthuat.spell.SpellCaster;
import phapthuat.spell.SpellUnit;
import phapthuat.spell.impl.LightSpell;
import phapthuat.thermal.PlacedLights;

/** Ví dụ số của spec 8.6: quang thông, mức sáng, ngưỡng UV và hồng ngoại. */
@GameTestHolder(PhapThuat.MODID)
@PrefixGameTestTemplate(false)
public class LightSpellGameTests {
    private static final String TEMPLATE = ThermalGameTests.EMPTY;

    private static ServerPlayer caster(GameTestHelper helper) {
        return TargetingGameTests.playerAt(helper, 3.5, 1, 3.5, 0, 0);
    }

    private static void cast(GameTestHelper helper, ServerPlayer player, Shape shape, double nm, double watts,
            double seconds, Vec3 center) {
        SpellCaster.castOnUnits(player, new InscribedSpell(LightSpell.ID, shape,
                Map.of(LightSpell.WAVELENGTH.key(), nm, LightSpell.POWER.key(), watts,
                        LightSpell.DURATION.key(), seconds)),
                List.of(new SpellUnit.AirUnit(BlockPos.containing(center))), center, null);
    }

    /** 555 nm, 1 W → 692,8 lm → mức sáng 15 ở ô quả cầu. */
    @GameTest(template = TEMPLATE)
    public static void greenLightLevel15(GameTestHelper helper) {
        Vec3 center = helper.absoluteVec(new Vec3(3.5, 2, 3.5));
        cast(helper, caster(helper), Shape.DIEM, 555, 1, 10, center);
        BlockPos cell = BlockPos.containing(center);
        helper.assertTrue(helper.getBlockState(cell).is(Blocks.LIGHT), "ô quả cầu phải là khối sáng, có "
                + helper.getBlockState(cell));
        int level = helper.getBlockState(cell).getValue(LightBlock.LEVEL);
        helper.assertTrue(level == 15, "mức sáng phải là 15, có " + level);
        helper.succeed();
    }

    /** Hết thời gian: khối sáng bị dọn và bảng PlacedLights rỗng. */
    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void lightRemovedAfterDuration(GameTestHelper helper) {
        Vec3 center = helper.absoluteVec(new Vec3(3.5, 2, 3.5));
        cast(helper, caster(helper), Shape.DIEM, 555, 1, 2, center);
        BlockPos cell = BlockPos.containing(center);
        helper.assertTrue(helper.getBlockState(cell).is(Blocks.LIGHT), "lúc đầu phải có khối sáng");
        helper.runAfterDelay(2 * 20 + 40, () -> {
            helper.assertTrue(helper.getBlockState(cell).isAir(), "hết hạn ô phải là không khí, có "
                    + helper.getBlockState(cell));
            helper.assertTrue(PlacedLights.get(helper.getLevel()).isEmpty(), "bảng khối sáng phải rỗng");
            helper.succeed();
        });
    }

    /** 1000 nm là hồng ngoại: không có khối sáng, nhưng mana vẫn bị trừ. */
    @GameTest(template = TEMPLATE)
    public static void infraredNoLight(GameTestHelper helper) {
        ServerPlayer player = caster(helper);
        Vec3 center = helper.absoluteVec(new Vec3(3.5, 2, 3.5));
        double before = ManaManager.get(player);
        cast(helper, player, Shape.DIEM, 1000, 100, 10, center);
        helper.assertTrue(helper.getBlockState(BlockPos.containing(center)).isAir(), "hồng ngoại không đặt khối sáng");
        double spent = before - ManaManager.get(player);
        helper.assertTrue(Math.abs(spent - 0.1) <= 1e-6, "vẫn phải trừ 0,1 mana, trừ " + spent);
        helper.succeed();
    }

    /** 350 nm, 3142 W, zombie cách 4 khối: I ≈ 15,6 W/m² ≥ 10 → undead bốc cháy sau mốc 1 giây. */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void uvBurnsZombie(GameTestHelper helper) {
        Vec3 center = helper.absoluteVec(new Vec3(3.5, 2, 3.5));
        Mob zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 7));
        zombie.setNoAi(true);
        zombie.moveTo(center.x, center.y, center.z + 4);
        cast(helper, caster(helper), Shape.DIEM, 350, 3142, 10, center);
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(zombie.getRemainingFireTicks() > 0, "zombie phải bốc cháy vì tia cực tím");
            helper.succeed();
        });
    }

    /** 1000 nm, 10 W, laser: I = 31 831 W/m² ≥ 12 500 → khối dễ cháy bên cạnh bắt lửa. */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void irLaserIgnitesWood(GameTestHelper helper) {
        helper.setBlock(new BlockPos(3, 2, 5), Blocks.OAK_PLANKS);
        ServerPlayer player = TargetingGameTests.playerAt(helper, 3.5, 1, 3.5, 0, 0);
        Mob target = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 4));
        target.setNoAi(true);
        SpellCaster.castOnUnits(player, new InscribedSpell(LightSpell.ID, Shape.TIA,
                Map.of(LightSpell.WAVELENGTH.key(), 1000.0, LightSpell.POWER.key(), 10.0,
                        LightSpell.DURATION.key(), 2.0,
                        "length", 8.0)),
                List.of(new SpellUnit.EntityUnit(target)), target.position(), null);
        helper.runAfterDelay(25, () -> {
            boolean fire = false;
            for (BlockPos p : new BlockPos[] {new BlockPos(2, 2, 4), new BlockPos(4, 2, 4),
                    new BlockPos(3, 2, 3), new BlockPos(3, 3, 4), new BlockPos(3, 2, 5)}) {
                if (helper.getBlockState(p).is(net.minecraft.tags.BlockTags.FIRE)) fire = true;
            }
            helper.assertTrue(fire, "hồng ngoại mạnh phải làm gỗ bên cạnh bắt lửa");
            helper.succeed();
        });
    }

}
