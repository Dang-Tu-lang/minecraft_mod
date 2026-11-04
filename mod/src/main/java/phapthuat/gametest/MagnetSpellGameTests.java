package phapthuat.gametest;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import phapthuat.PhapThuat;
import phapthuat.mana.ManaManager;
import phapthuat.spell.InscribedSpell;
import phapthuat.spell.Shape;
import phapthuat.spell.SpellCaster;
import phapthuat.spell.SpellUnit;
import phapthuat.spell.impl.MagnetSpell;

/** Ví dụ số của spec 8.5: năng lượng trường, hút sắt theo khối lượng riêng của sắt trong thực thể. */
@GameTestHolder(PhapThuat.MODID)
@PrefixGameTestTemplate(false)
public class MagnetSpellGameTests {
    private static final String TEMPLATE = ThermalGameTests.EMPTY;

    private static ServerPlayer caster(GameTestHelper helper) {
        return TargetingGameTests.playerAt(helper, 0.5, 2, 0.5, -90, 0);
    }

    private static void cast(GameTestHelper helper, ServerPlayer player, List<SpellUnit> units, Vec3 center,
            double teslas, double radius, double seconds) {
        SpellCaster.castOnUnits(player, new InscribedSpell(MagnetSpell.ID, Shape.CAU,
                Map.of(MagnetSpell.FIELD.key(), teslas, MagnetSpell.RADIUS.key(), radius,
                        MagnetSpell.DURATION.key(), seconds)), units, center, null);
    }

    /** Thỏi sắt rơi cách tâm 1,5 khối, B 0,5 T, r 4 m, 2 giây → sau 20 tick nó gần tâm hơn lúc đầu. */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void pullsIronIngot(GameTestHelper helper) {
        ServerPlayer player = caster(helper);
        Vec3 center = helper.absoluteVec(new Vec3(3.5, 3, 0.5));
        Vec3 start = center.add(1.5, 0, 0);
        ItemEntity ingot = new ItemEntity(helper.getLevel(), start.x, start.y, start.z,
                new ItemStack(Items.IRON_INGOT));
        ingot.setPickUpDelay(Integer.MAX_VALUE);
        ingot.setNoGravity(true);
        ingot.setDeltaMovement(Vec3.ZERO);
        helper.getLevel().addFreshEntity(ingot);
        double before = ingot.getBoundingBox().getCenter().distanceTo(center);
        cast(helper, player, List.of(new SpellUnit.EntityUnit(ingot)), center, 0.5, 4, 2);
        helper.runAfterDelay(20, () -> {
            double after = ingot.getBoundingBox().getCenter().distanceTo(center);
            helper.assertTrue(after < before, "thỏi sắt phải bị hút về tâm: " + before + " → " + after);
            helper.succeed();
        });
    }

    /** Zombie đủ bộ giáp sắt bị hút; zombie không giáp đứng nguyên tại chỗ. */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void armoredZombiePulledPlainNot(GameTestHelper helper) {
        ServerPlayer player = caster(helper);
        Vec3 center = helper.absoluteVec(new Vec3(3.5, 3, 0.5));
        Mob armored = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 0));
        Mob plain = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 2));
        armored.setNoAi(true);
        plain.setNoAi(true);
        armored.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        armored.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        armored.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        armored.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
        double armoredBefore = armored.getBoundingBox().getCenter().distanceTo(center);
        double plainBefore = plain.getBoundingBox().getCenter().distanceTo(center);
        cast(helper, player, List.of(new SpellUnit.EntityUnit(armored), new SpellUnit.EntityUnit(plain)), center,
                0.5, 4, 2);
        helper.runAfterDelay(20, () -> {
            double armoredAfter = armored.getBoundingBox().getCenter().distanceTo(center);
            double plainAfter = plain.getBoundingBox().getCenter().distanceTo(center);
            helper.assertTrue(armoredAfter < armoredBefore - 0.01,
                    "zombie mặc giáp phải bị hút: " + armoredBefore + " → " + armoredAfter);
            helper.assertTrue(Math.abs(plainAfter - plainBefore) < 0.01,
                    "zombie không giáp phải đứng yên: " + plainBefore + " → " + plainAfter);
            helper.succeed();
        });
    }

    /** Người chơi mặc giáp sắt đứng trong cầu cũng bị hút (phản tác dụng, spec 8.5). */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void casterWithIronArmorPulled(GameTestHelper helper) {
        ServerPlayer player = caster(helper);
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        Vec3 center = player.getBoundingBox().getCenter().add(1.5, 0, 0);
        double before = player.getBoundingBox().getCenter().distanceTo(center);
        cast(helper, player, List.of(new SpellUnit.EntityUnit(player)), center, 0.5, 4, 2);
        helper.runAfterDelay(20, () -> {
            double after = player.getBoundingBox().getCenter().distanceTo(center);
            helper.assertTrue(after < before - 0.01, "người chơi mặc giáp phải bị hút: " + before + " → " + after);
            helper.succeed();
        });
    }

    /** B 0,5 T, r 1 m → u·V ≈ 416,7 kJ → 41,7 mana. */
    @GameTest(template = TEMPLATE)
    public static void costMatchesFieldEnergy(GameTestHelper helper) {
        ServerPlayer player = caster(helper);
        double before = ManaManager.get(player);
        cast(helper, player, List.of(new SpellUnit.EntityUnit(player)), player.position(), 0.5, 1, 1);
        double spent = before - ManaManager.get(player);
        helper.assertTrue(Math.abs(spent - 41.7) <= 0.1, "phải tốn ≈ 41,7 mana, tốn " + spent);
        helper.succeed();
    }
}
