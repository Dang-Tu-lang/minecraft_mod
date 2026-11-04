package phapthuat.gametest;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import phapthuat.PhapThuat;
import phapthuat.mana.ManaManager;
import phapthuat.spell.CastReport;
import phapthuat.spell.InscribedSpell;
import phapthuat.spell.Shape;
import phapthuat.spell.SpellCaster;
import phapthuat.spell.SpellUnit;
import phapthuat.spell.impl.LiftSpell;

/** Ví dụ số của spec 8.9: thế năng m·g·h, khối rơi gây sát thương m·g·h_rơi / 1 kJ, khối cấm nâng. */
@GameTestHolder(PhapThuat.MODID)
@PrefixGameTestTemplate(false)
public class LiftSpellGameTests {
    private static final String TEMPLATE = ThermalGameTests.EMPTY;

    private static ServerPlayer caster(GameTestHelper helper) {
        return TargetingGameTests.playerAt(helper, 0.5, 2, 0.5, -90, 0);
    }

    private static CastReport cast(ServerPlayer player, List<SpellUnit> units, Vec3 center, double height,
            double seconds) {
        return SpellCaster.castOnUnits(player, new InscribedSpell(LiftSpell.ID, Shape.DIEM,
                Map.of(LiftSpell.HEIGHT.key(), height, LiftSpell.DURATION.key(), seconds)), units, center, null);
    }

    /** Zombie 70 kg lên 10 m: m·g·h = 6,9 kJ → 0,69 mana. Sau 30 tick Y tăng ≥ 9. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void liftZombie10m(GameTestHelper helper) {
        ServerPlayer player = caster(helper);
        Mob zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 3));
        zombie.setNoAi(true);
        double startY = zombie.getY();
        double before = ManaManager.get(player);
        CastReport report = cast(player, List.of(new SpellUnit.EntityUnit(zombie)), zombie.position(), 10, 3);
        double spent = before - ManaManager.get(player);
        helper.assertTrue(report.success(), "phải nâng được zombie, dòng báo: " + report.message().getString());
        helper.assertTrue(Math.abs(spent - 0.69) <= 0.01, "phải tốn ≈ 0,69 mana, tốn " + spent);
        helper.runAfterDelay(30, () -> {
            double risen = zombie.getY() - startY;
            helper.assertTrue(risen >= 9, "sau 30 tick zombie phải lên ≥ 9 m, lên " + risen);
            helper.succeed();
        });
    }

    /** Khối đá 2700 kg lên 5 m: 132,4 kJ → 13,2 mana. Ô cũ thành không khí, có khối rơi. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void liftStoneBlock(GameTestHelper helper) {
        BlockPos rel = new BlockPos(4, 2, 0);
        helper.setBlock(rel, Blocks.STONE);
        ServerPlayer player = caster(helper);
        double before = ManaManager.get(player);
        CastReport report = cast(player, List.of(new SpellUnit.BlockUnit(helper.absolutePos(rel))),
                Vec3.atCenterOf(helper.absolutePos(rel)), 5, 3);
        double spent = before - ManaManager.get(player);
        helper.assertTrue(report.success(), "phải nâng được khối đá, dòng báo: " + report.message().getString());
        helper.assertTrue(Math.abs(spent - 13.2) <= 0.05, "phải tốn ≈ 13,2 mana, tốn " + spent);
        helper.assertBlockNotPresent(Blocks.STONE, rel);
        Vec3 center = Vec3.atCenterOf(helper.absolutePos(rel));
        List<FallingBlockEntity> falling = helper.getLevel().getEntitiesOfClass(FallingBlockEntity.class,
                new AABB(center, center).inflate(3), Entity::isAlive);
        helper.assertTrue(!falling.isEmpty(), "phải có khối rơi ở chỗ khối đá");
        helper.succeed();
    }

    /** Nâng đá 3 m ngay trên đầu zombie, 1 giây rồi thả: zombie mất máu. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void droppedBlockHurts(GameTestHelper helper) {
        BlockPos over = new BlockPos(3, 5, 3);
        helper.setBlock(over, Blocks.STONE);
        ServerPlayer player = caster(helper);
        Mob zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 3));
        zombie.setNoAi(true);
        float healthBefore = zombie.getHealth();
        cast(player, List.of(new SpellUnit.BlockUnit(helper.absolutePos(over))),
                Vec3.atCenterOf(helper.absolutePos(over)), 3, 1);
        helper.runAfterDelay(20 * 6, () -> {
            helper.assertTrue(zombie.getHealth() < healthBefore || !zombie.isAlive(),
                    "khối đá rơi trúng phải làm zombie mất máu: " + healthBefore + " → " + zombie.getHealth());
            helper.succeed();
        });
    }

    /** Rương có đồ và đá nền: cả hai giữ nguyên, rương còn đủ đồ, không trừ mana. */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void cannotLiftChestOrBedrock(GameTestHelper helper) {
        BlockPos chestRel = new BlockPos(4, 2, 0);
        BlockPos bedrockRel = new BlockPos(4, 2, 4);
        helper.setBlock(chestRel, Blocks.CHEST);
        helper.setBlock(bedrockRel, Blocks.BEDROCK);
        ChestBlockEntity chest = helper.getBlockEntity(chestRel);
        chest.setItem(0, new ItemStack(Items.DIAMOND, 5));
        ServerPlayer player = caster(helper);
        double before = ManaManager.get(player);
        CastReport report = cast(player, List.of(new SpellUnit.BlockUnit(helper.absolutePos(chestRel)),
                new SpellUnit.BlockUnit(helper.absolutePos(bedrockRel))),
                Vec3.atCenterOf(helper.absolutePos(chestRel)), 3, 2);
        double spent = before - ManaManager.get(player);
        helper.assertTrue(!report.success(), "không nâng được gì thì phải là thất bại");
        helper.assertTrue(Math.abs(spent) <= 1e-9, "không được trừ mana, trừ " + spent);
        helper.assertBlockPresent(Blocks.CHEST, chestRel);
        helper.assertBlockPresent(Blocks.BEDROCK, bedrockRel);
        ChestBlockEntity after = helper.getBlockEntity(chestRel);
        helper.assertTrue(after.countItem(Items.DIAMOND) == 5, "rương phải còn đủ 5 viên kim cương, còn "
                + after.countItem(Items.DIAMOND));
        helper.succeed();
    }
}
