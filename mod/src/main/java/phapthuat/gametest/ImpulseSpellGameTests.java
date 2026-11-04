package phapthuat.gametest;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
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
import phapthuat.spell.impl.ImpulseSpell;

/**
 * Ví dụ số của spec 8.3 và luật phản tác dụng định luật 3 Newton (spec 8.3, Review Focus 2). Người chơi giả đứng
 * tại (0.5, 2, 0.5) — tầng y = 1 là nền đá của khuôn thử — nhìn về +X (yaw −90°).
 */
@GameTestHolder(PhapThuat.MODID)
@PrefixGameTestTemplate(false)
public class ImpulseSpellGameTests {
    private static final String TEMPLATE = ThermalGameTests.EMPTY;
    private static final Vec3 CASTER = new Vec3(0.5, 2, 0.5);
    private static final float FACING_PLUS_X = -90;

    private static ServerPlayer caster(GameTestHelper helper) {
        return TargetingGameTests.playerAt(helper, CASTER.x, CASTER.y, CASTER.z, FACING_PLUS_X, 0);
    }

    private static void cast(GameTestHelper helper, ServerPlayer player, Shape shape, double mana, int direction,
            SpellUnit unit) {
        SpellCaster.castOnUnits(player, new InscribedSpell(ImpulseSpell.ID, shape,
                Map.of(ImpulseSpell.ENERGY.key(), mana, ImpulseSpell.DIRECTION.key(), (double) direction)),
                List.of(unit), player.getEyePosition(), null);
    }

    /** Zombie 70 kg, 1 mana → √(2·10 000/70) ≈ 16,9 m/s = 0,845 khối/tick, ra xa người chơi. */
    @GameTest(template = TEMPLATE)
    public static void pushZombie(GameTestHelper helper) {
        ServerPlayer player = caster(helper);
        Mob zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 0));
        zombie.setNoAi(true);
        cast(helper, player, Shape.DIEM, 1, ImpulseSpell.PUSH, new SpellUnit.EntityUnit(zombie));
        Vec3 v = zombie.getDeltaMovement();
        double speed = v.length();
        helper.assertTrue(Math.abs(speed - 0.845) <= 0.05, "vận tốc phải ≈ 0,845 khối/tick, có " + speed);
        helper.assertTrue(v.x > 0.5, "zombie phải bay ra xa người chơi theo +X, có " + v);
        helper.succeed();
    }

    /** Kéo: vận tốc hướng về phía người chơi. Khối lượng vật phẩm rơi lấy theo thể tích hitbox (spec 7.3). */
    @GameTest(template = TEMPLATE)
    public static void pullItem(GameTestHelper helper) {
        ServerPlayer player = caster(helper);
        ItemEntity item = new ItemEntity(helper.getLevel(),
                helper.absoluteVec(new Vec3(4.5, 2, 0.5)).x,
                helper.absoluteVec(new Vec3(4.5, 2, 0.5)).y,
                helper.absoluteVec(new Vec3(4.5, 2, 0.5)).z,
                new ItemStack(Items.IRON_INGOT));
        item.setDeltaMovement(Vec3.ZERO);
        helper.getLevel().addFreshEntity(item);
        cast(helper, player, Shape.DIEM, 1, ImpulseSpell.PULL, new SpellUnit.EntityUnit(item));
        Vec3 v = item.getDeltaMovement();
        helper.assertTrue(v.length() > 0.1, "vật phẩm phải bị kéo, vận tốc " + v);
        helper.assertTrue(v.x < 0, "vận tốc phải hướng về người chơi ở phía −X, có " + v);
        helper.succeed();
    }

    /** Đứng trên đá: mặt đất hấp thụ xung lượng ngược, người chơi không bị giật. */
    @GameTest(template = TEMPLATE)
    public static void noRecoilOnGround(GameTestHelper helper) {
        ServerPlayer player = caster(helper);
        Mob golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(3, 2, 0));
        golem.setNoAi(true);
        player.setDeltaMovement(Vec3.ZERO);
        cast(helper, player, Shape.DIEM, 1, ImpulseSpell.PUSH, new SpellUnit.EntityUnit(golem));
        double speed = player.getDeltaMovement().length();
        helper.assertTrue(speed < 0.01, "người chơi đứng trên đá không được giật lùi, có " + speed);
        helper.succeed();
    }

    /** Lơ lửng: golem 500 kg, 1 mana → J = 3162 N·s, 3162/70 ≈ 45 m/s, kẹp còn 40 m/s = 2,0 khối/tick. */
    @GameTest(template = TEMPLATE)
    public static void recoilWhenAirborneIsCapped(GameTestHelper helper) {
        ServerPlayer player = TargetingGameTests.playerAt(helper, CASTER.x, 5, CASTER.z, FACING_PLUS_X, 0);
        Mob golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(3, 5, 0));
        golem.setNoAi(true);
        player.setDeltaMovement(Vec3.ZERO);
        cast(helper, player, Shape.DIEM, 1, ImpulseSpell.PUSH, new SpellUnit.EntityUnit(golem));
        Vec3 v = player.getDeltaMovement();
        helper.assertTrue(Math.abs(v.length() - 2.0) <= 0.05, "giật lùi phải bị kẹp ở 2,0 khối/tick, có " + v.length());
        helper.assertTrue(v.x < 0, "phải giật ngược hướng đẩy, có " + v);
        helper.succeed();
    }

    /** Đứng trên băng: ma sát gần như không có, người chơi vẫn bị giật ngược. */
    @GameTest(template = TEMPLATE)
    public static void recoilOnIce(GameTestHelper helper) {
        helper.setBlock(new BlockPos(0, 1, 0), Blocks.ICE);
        ServerPlayer player = caster(helper);
        Mob golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(3, 2, 0));
        golem.setNoAi(true);
        player.setDeltaMovement(Vec3.ZERO);
        cast(helper, player, Shape.DIEM, 1, ImpulseSpell.PUSH, new SpellUnit.EntityUnit(golem));
        Vec3 v = player.getDeltaMovement();
        helper.assertTrue(v.x < -0.5, "đứng trên băng phải bị giật ngược, có " + v);
        helper.succeed();
    }
}
