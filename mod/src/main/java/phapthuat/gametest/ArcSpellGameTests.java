package phapthuat.gametest;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.block.Blocks;
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
import phapthuat.spell.impl.ArcSpell;

/**
 * Ví dụ số của spec 8.4. Người chơi giả ở chế độ sáng tạo nên không mất máu: các ca phóng ngược kiểm bằng khóa dịch
 * của dòng báo. Mục tiêu được đặt sao cho tâm hộp chạm đúng khoảng cách cần đo (mắt người chơi → tâm hộp).
 */
@GameTestHolder(PhapThuat.MODID)
@PrefixGameTestTemplate(false)
public class ArcSpellGameTests {
    private static final String TEMPLATE = ThermalGameTests.EMPTY;
    private static final Vec3 CASTER = new Vec3(0.5, 2, 0.5);
    private static final float FACING_PLUS_X = -90;

    private static ServerPlayer caster(GameTestHelper helper) {
        return TargetingGameTests.playerAt(helper, CASTER.x, CASTER.y, CASTER.z, FACING_PLUS_X, 0);
    }

    /** Đặt thực thể sao cho tâm hộp cách mắt người chơi đúng {@code distance} theo +X. */
    private static <T extends Entity> T atDistance(GameTestHelper helper, EntityType<T> type, ServerPlayer player,
            double distance) {
        T entity = helper.spawn(type, new BlockPos(3, 2, 3));
        Vec3 eye = player.getEyePosition();
        entity.moveTo(eye.x + distance, eye.y - entity.getBbHeight() / 2, eye.z);
        if (entity instanceof Mob mob) mob.setNoAi(true);
        return entity;
    }

    private static String key(CastReport report) {
        return report.message().getContents() instanceof TranslatableContents t ? t.getKey() : "";
    }

    private static CastReport cast(ServerPlayer player, double kv, double mana, List<SpellUnit> units, Vec3 center) {
        return SpellCaster.castOnUnits(player, new InscribedSpell(ArcSpell.ID, Shape.DIEM,
                Map.of(ArcSpell.VOLTAGE.key(), kv, ArcSpell.ENERGY.key(), mana)), units, center, null);
    }

    /** Zombie đúng 5,0 m: 15 000 kV = 3 MV/m, 20 mana = 200 kJ = 10 máu. Phí tích điện thêm 1,125 mana. */
    @GameTest(template = TEMPLATE)
    public static void arcHitsAt5m(GameTestHelper helper) {
        ServerPlayer player = caster(helper);
        Mob zombie = atDistance(helper, EntityType.ZOMBIE, player, 5);
        double before = ManaManager.get(player);
        CastReport report = cast(player, 15_000, 20, List.of(new SpellUnit.EntityUnit(zombie)),
                zombie.getBoundingBox().getCenter());
        helper.assertTrue(report.success(), "hồ quang phải trúng, dòng báo: " + key(report) + " "
                + report.message().getString());
        helper.assertTrue(Math.abs(zombie.getHealth() - 10.0) <= 0.01, "zombie phải còn 10 máu, còn "
                + zombie.getHealth());
        double spent = before - ManaManager.get(player);
        helper.assertTrue(Math.abs(spent - 21.125) <= 0.01, "phải tốn 20 + 1,125 mana, tốn " + spent);
        helper.succeed();
    }

    /** 14 000 kV: 2,8 MV/m < 3 nhưng 28 MV/m ở 0,5 m → điện phóng ngược vào người thi triển. */
    @GameTest(template = TEMPLATE)
    public static void arcBackfiresOnCaster(GameTestHelper helper) {
        ServerPlayer player = caster(helper);
        Mob zombie = atDistance(helper, EntityType.ZOMBIE, player, 5);
        CastReport report = cast(player, 14_000, 20, List.of(new SpellUnit.EntityUnit(zombie)),
                zombie.getBoundingBox().getCenter());
        helper.assertTrue(!report.success(), "phóng ngược phải là thất bại");
        helper.assertTrue(key(report).equals("phapthuat.arc.backfire"), "dòng báo phải là phóng ngược, có "
                + key(report));
        helper.assertTrue(zombie.getHealth() == 20, "zombie phải nguyên vẹn, còn " + zombie.getHealth());
        helper.succeed();
    }

    /** 1000 kV: cả 0,5 m cũng không đủ (cần 1,5 MV) → chỉ lách tách, không ai bị thương. */
    @GameTest(template = TEMPLATE)
    public static void arcFizzles(GameTestHelper helper) {
        ServerPlayer player = caster(helper);
        Mob zombie = atDistance(helper, EntityType.ZOMBIE, player, 5);
        CastReport report = cast(player, 1_000, 20, List.of(new SpellUnit.EntityUnit(zombie)),
                zombie.getBoundingBox().getCenter());
        helper.assertTrue(key(report).equals("phapthuat.arc.fizzle"), "dòng báo phải là lách tách, có "
                + key(report));
        helper.assertTrue(zombie.getHealth() == 20, "zombie phải nguyên vẹn, còn " + zombie.getHealth());
        helper.succeed();
    }

    /** Không có mục tiêu: coi như tới trời 16 m. 40 MV / 16 m = 2,5 MV/m < 3 → phóng ngược, không crash. */
    @GameTest(template = TEMPLATE)
    public static void arcAtSkyUsesSixteenMeters(GameTestHelper helper) {
        ServerPlayer player = caster(helper);
        Vec3 eye = player.getEyePosition();
        Vec3 sky = new Vec3(eye.x, eye.y + ArcSpell.skyDistance(), eye.z);
        CastReport report = cast(player, 40_000, 20,
                List.of(new SpellUnit.AirUnit(BlockPos.containing(sky))), sky);
        helper.assertTrue(key(report).equals("phapthuat.arc.backfire"),
                "40 MV tới trời phải phóng ngược, có " + key(report));
        helper.succeed();
    }

    /** Từ 50 mana: hiệu ứng sét vanilla. Creeper đúng 3,0 m, 10 000 kV = 3,33 MV/m → creeper tích điện. */
    @GameTest(template = TEMPLATE)
    public static void chargedCreeper(GameTestHelper helper) {
        ServerPlayer player = caster(helper);
        Creeper creeper = atDistance(helper, EntityType.CREEPER, player, 3);
        CastReport report = cast(player, 10_000, 50, List.of(new SpellUnit.EntityUnit(creeper)),
                creeper.getBoundingBox().getCenter());
        helper.assertTrue(report.success(), "hồ quang phải trúng creeper, dòng báo: " + report.message().getString());
        helper.assertTrue(creeper.isPowered(), "creeper phải tích điện");
        helper.succeed();
    }

    /** Điện lan trong nước: hai zombie và người chơi cùng ở trong nước trong 4 khối đều mất máu (chia đều). */
    @GameTest(template = TEMPLATE)
    public static void waterSpreads(GameTestHelper helper) {
        helper.setBlock(new BlockPos(0, 2, 0), Blocks.WATER);
        helper.setBlock(new BlockPos(3, 2, 0), Blocks.WATER);
        helper.setBlock(new BlockPos(3, 2, 1), Blocks.WATER);
        ServerPlayer player = caster(helper);
        Mob first = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 0));
        Mob second = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 1));
        first.setNoAi(true);
        second.setNoAi(true);
        helper.assertTrue(player.isInWater(), "người chơi giả phải ở trong nước");
        CastReport report = cast(player, 15_000, 20, List.of(new SpellUnit.EntityUnit(first)),
                first.getBoundingBox().getCenter());
        helper.assertTrue(key(report).equals("phapthuat.arc.water"), "dòng báo phải là lan trong nước, có "
                + key(report));
        helper.assertTrue(first.getHealth() < 20 && second.getHealth() < 20,
                "cả hai zombie phải mất máu: " + first.getHealth() + " / " + second.getHealth());
        helper.succeed();
    }
}
