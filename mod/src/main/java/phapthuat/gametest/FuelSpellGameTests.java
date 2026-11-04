package phapthuat.gametest;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import phapthuat.PhapThuat;
import phapthuat.spell.InscribedSpell;
import phapthuat.spell.Shape;
import phapthuat.spell.SpellCaster;
import phapthuat.spell.SpellUnit;
import phapthuat.spell.impl.FuelSpell;

/** Ví dụ số của spec 8.7: năng lượng nhiên liệu, quy tắc Thornton, khí CO trong vùng kín. */
@GameTestHolder(PhapThuat.MODID)
@PrefixGameTestTemplate(false)
public class FuelSpellGameTests {
    private static final String TEMPLATE = ThermalGameTests.EMPTY;

    private static ServerPlayer caster(GameTestHelper helper) {
        return TargetingGameTests.playerAt(helper, 0.5, 2, 0.5, -90, 0);
    }

    private static void cast(ServerPlayer player, Shape shape, int fuel, int count, List<SpellUnit> units,
            Vec3 center) {
        SpellCaster.castOnUnits(player, new InscribedSpell(FuelSpell.ID, shape,
                Map.of(FuelSpell.FUEL.key(), (double) fuel, FuelSpell.COUNT.key(), (double) count)), units, center,
                null);
    }

    /** 1 than, DIEM vào gỗ cách 6 khối: than bị trừ hết, gỗ bắt lửa. */
    @GameTest(template = TEMPLATE)
    public static void coalIgnitesWood(GameTestHelper helper) {
        ServerPlayer player = caster(helper);
        player.getInventory().add(new ItemStack(Items.COAL));
        BlockPos wood = new BlockPos(5, 2, 0);
        helper.setBlock(wood, Blocks.OAK_PLANKS);
        cast(player, Shape.DIEM, FuelChoicesIndex.COAL, 1, List.of(new SpellUnit.BlockUnit(helper.absolutePos(wood))),
                Vec3.atCenterOf(helper.absolutePos(wood)));
        helper.assertTrue(player.getInventory().countItem(Items.COAL) == 0, "than phải bị trừ hết");
        helper.assertTrue(helper.getBlockState(wood.above()).is(net.minecraft.tags.BlockTags.FIRE)
                        || helper.getBlockState(wood.east()).is(net.minecraft.tags.BlockTags.FIRE),
                "gỗ phải bắt lửa");
        helper.succeed();
    }

    /** 2 chồng que, mỗi chồng 1 cái, count 2: cả hai chồng đều bị trừ. */
    @GameTest(template = TEMPLATE)
    public static void consumesAcrossStacks(GameTestHelper helper) {
        ServerPlayer player = caster(helper);
        player.getInventory().add(new ItemStack(Items.STICK));
        player.getInventory().add(new ItemStack(Items.STICK));
        BlockPos wood = new BlockPos(5, 2, 0);
        helper.setBlock(wood, Blocks.OAK_PLANKS);
        cast(player, Shape.DIEM, FuelChoicesIndex.STICK, 2,
                List.of(new SpellUnit.BlockUnit(helper.absolutePos(wood))),
                Vec3.atCenterOf(helper.absolutePos(wood)));
        helper.assertTrue(player.getInventory().countItem(Items.STICK) == 0, "cả hai que phải bị trừ, còn "
                + player.getInventory().countItem(Items.STICK));
        helper.succeed();
    }

    /** Chỉ có 1 que, count 2: không trừ gì, không có lửa. */
    @GameTest(template = TEMPLATE)
    public static void missingFuelConsumesNothing(GameTestHelper helper) {
        ServerPlayer player = caster(helper);
        player.getInventory().add(new ItemStack(Items.STICK));
        BlockPos wood = new BlockPos(5, 2, 0);
        helper.setBlock(wood, Blocks.OAK_PLANKS);
        cast(player, Shape.DIEM, FuelChoicesIndex.STICK, 2,
                List.of(new SpellUnit.BlockUnit(helper.absolutePos(wood))),
                Vec3.atCenterOf(helper.absolutePos(wood)));
        helper.assertTrue(player.getInventory().countItem(Items.STICK) == 1, "không được trừ que nào");
        helper.assertBlockNotPresent(Blocks.FIRE, new BlockPos(5, 3, 0));
        helper.succeed();
    }

    /** Hộp đá kín 2 ô không khí có một con lợn: 1 than cần 0,626 kg O₂ nhưng chỉ có 0,552 kg → sinh CO. */
    @GameTest(template = TEMPLATE, timeoutTicks = 160)
    public static void enclosedPocketMakesCO(GameTestHelper helper) {
        BlockPos inside = new BlockPos(3, 2, 3);
        BlockPos pocket = new BlockPos(4, 2, 3);
        // Dựng hộp kín 2 ô: đá bao quanh hai ô.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = pocket.offset(dx, dy, dz);
                    if (p.equals(pocket) || p.equals(inside)) continue;
                    helper.setBlock(p, Blocks.STONE);
                }
            }
        }
        BlockPos extra = pocket.east();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = extra.offset(dx, dy, dz);
                    if (p.equals(inside) || p.equals(pocket)) continue;
                    if (helper.getBlockState(p).isAir()) helper.setBlock(p, Blocks.STONE);
                }
            }
        }
        helper.setBlock(pocket, Blocks.AIR);
        Mob pig = helper.spawn(EntityType.PIG, inside);
        pig.setNoAi(true);
        ServerPlayer player = caster(helper);
        player.getInventory().add(new ItemStack(Items.COAL));
        cast(player, Shape.DIEM, FuelChoicesIndex.COAL, 1, List.of(new SpellUnit.AirUnit(helper.absolutePos(pocket))),
                Vec3.atCenterOf(helper.absolutePos(pocket)));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(pig.hasEffect(MobEffects.CONFUSION) || pig.hasEffect(MobEffects.POISON),
                    "lợn trong hộp kín phải trúng khí CO");
            helper.succeed();
        });
    }

    /** Ngoài trời thoáng: không ai trúng độc CO. */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void openAirNoCO(GameTestHelper helper) {
        BlockPos target = new BlockPos(3, 2, 3);
        Mob pig = helper.spawn(EntityType.PIG, target);
        pig.setNoAi(true);
        ServerPlayer player = caster(helper);
        player.getInventory().add(new ItemStack(Items.STICK));
        cast(player, Shape.DIEM, FuelChoicesIndex.STICK, 1, List.of(new SpellUnit.AirUnit(helper.absolutePos(target))),
                Vec3.atCenterOf(helper.absolutePos(target)));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(!pig.hasEffect(MobEffects.POISON), "ngoài trời không được trúng độc");
            helper.succeed();
        });
    }

    /** Chỉ số lựa chọn nhiên liệu, tách ra cho dễ đọc. */
    private static final class FuelChoicesIndex {
        static final int COAL = 0;
        static final int STICK = 5;
    }
}
