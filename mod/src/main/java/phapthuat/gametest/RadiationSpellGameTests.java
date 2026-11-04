package phapthuat.gametest;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import phapthuat.PhapThuat;
import phapthuat.spell.InscribedSpell;
import phapthuat.spell.Shape;
import phapthuat.spell.SpellCaster;
import phapthuat.spell.SpellUnit;
import phapthuat.spell.impl.RadiationSpell;
import phapthuat.thermal.WorldTemps;

/** Ví dụ số của spec 8.8: liều Gy = năng lượng hấp thụ / khối lượng, giảm theo vật chắn, cộng dồn, tán xạ. */
@GameTestHolder(PhapThuat.MODID)
@PrefixGameTestTemplate(false)
public class RadiationSpellGameTests {
    private static final String TEMPLATE = ThermalGameTests.EMPTY;

    private static ServerPlayer caster(GameTestHelper helper) {
        return TargetingGameTests.playerAt(helper, 0.5, 2, 0.5, -90, 0);
    }

    /** Đặt sinh vật sao cho tâm hộp cách mắt người chơi đúng {@code distance} theo +X (tia nằm ngang). */
    private static <T extends Entity> T atDistance(GameTestHelper helper, EntityType<T> type, ServerPlayer player,
            double distance) {
        T entity = helper.spawn(type, new BlockPos(3, 2, 3));
        Vec3 eye = player.getEyePosition();
        entity.moveTo(eye.x + distance, eye.y - entity.getBbHeight() / 2, eye.z);
        if (entity instanceof Mob mob) mob.setNoAi(true);
        return entity;
    }

    private static void cast(ServerPlayer player, double mana, List<SpellUnit> units, Vec3 center) {
        SpellCaster.castOnUnits(player, new InscribedSpell(RadiationSpell.ID, Shape.DIEM,
                Map.of(RadiationSpell.ENERGY.key(), mana)), units, center, null);
    }

    private static double dose(LivingEntity entity) {
        return WorldTemps.currentEntity(entity).doseGy();
    }

    /** Lợn 100 kg cách 5 khối, 300 mana → hấp thụ 600 J → 6,0 Gy → khô héo sau mốc 1 giây. */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void pigWitherAt6Gy(GameTestHelper helper) {
        ServerPlayer player = caster(helper);
        Mob pig = atDistance(helper, EntityType.PIG, player, 5);
        cast(player, 300, List.of(new SpellUnit.EntityUnit(pig)), pig.getBoundingBox().getCenter());
        double gy = dose(pig);
        helper.assertTrue(Math.abs(gy - 6.0) <= 0.01, "liều phải ≈ 6,0 Gy, có " + gy);
        helper.runAfterDelay(21, () -> {
            helper.assertTrue(pig.hasEffect(MobEffects.WITHER), "từ 6 Gy phải bị khô héo");
            helper.succeed();
        });
    }

    /** Một khối nước chắn giữa người chơi và lợn: × 0,1 → 0,6 Gy, chỉ buồn nôn. */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void waterShield(GameTestHelper helper) {
        ServerPlayer player = caster(helper);
        Mob pig = atDistance(helper, EntityType.PIG, player, 5);
        // Tia mắt → tâm lợn nằm ngang ở y ≈ 3,62, z = 0,5: chỉ đi qua đúng ô (3, 3, 0).
        helper.setBlock(new BlockPos(3, 3, 0), Blocks.WATER);
        cast(player, 300, List.of(new SpellUnit.EntityUnit(pig)), pig.getBoundingBox().getCenter());
        double gy = dose(pig);
        helper.assertTrue(Math.abs(gy - 0.6) <= 0.02, "qua 1 khối nước liều phải ≈ 0,6 Gy, có " + gy);
        helper.runAfterDelay(21, () -> {
            helper.assertTrue(pig.hasEffect(MobEffects.CONFUSION), "0,6 Gy phải gây buồn nôn");
            helper.assertFalse(pig.hasEffect(MobEffects.WITHER), "0,6 Gy chưa đủ gây khô héo");
            helper.succeed();
        });
    }

    /** Chiếu hai lần 150 mana: liều cộng dồn ≈ 6,0 Gy. */
    @GameTest(template = TEMPLATE)
    public static void doseAccumulates(GameTestHelper helper) {
        ServerPlayer player = caster(helper);
        Mob pig = atDistance(helper, EntityType.PIG, player, 5);
        Vec3 center = pig.getBoundingBox().getCenter();
        cast(player, 150, List.of(new SpellUnit.EntityUnit(pig)), center);
        cast(player, 150, List.of(new SpellUnit.EntityUnit(pig)), center);
        double gy = dose(pig);
        helper.assertTrue(Math.abs(gy - 6.0) <= 0.05, "hai lần 150 mana phải cộng thành ≈ 6,0 Gy, có " + gy);
        helper.succeed();
    }

    /** Lợn cách 2 khối: năng lượng tán xạ sang người chơi đứng gần. */
    @GameTest(template = TEMPLATE)
    public static void scatterHitsCasterNearby(GameTestHelper helper) {
        ServerPlayer player = caster(helper);
        Mob pig = atDistance(helper, EntityType.PIG, player, 2);
        cast(player, 300, List.of(new SpellUnit.EntityUnit(pig)), pig.getBoundingBox().getCenter());
        double selfDose = dose(player);
        helper.assertTrue(selfDose > 0, "người chơi đứng gần phải nhận liều tán xạ, có " + selfDose);
        helper.succeed();
    }

    /** Đặt liều 6,0 Gy, sau 60 giây (chu kỳ bán rã 60 giây) còn ≈ 3,0 Gy. */
    @GameTest(template = TEMPLATE, timeoutTicks = 1300)
    public static void doseDecays(GameTestHelper helper) {
        Mob golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(3, 2, 3));
        golem.setNoAi(true);
        WorldTemps.saveEntity(golem, WorldTemps.naturalEntityC(golem), 6.0);
        helper.runAfterDelay(1200, () -> {
            double gy = dose(golem);
            helper.assertTrue(Math.abs(gy - 3.0) <= 0.25, "sau 60 giây liều phải ≈ 3,0 Gy, có " + gy);
            helper.succeed();
        });
    }
}
