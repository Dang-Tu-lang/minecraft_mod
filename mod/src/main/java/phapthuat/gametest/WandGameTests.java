package phapthuat.gametest;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import phapthuat.PhapThuat;
import phapthuat.entity.SpellProjectile;
import phapthuat.item.WandItem;
import phapthuat.mana.ManaManager;
import phapthuat.registry.ModDataComponents;
import phapthuat.registry.ModItems;
import phapthuat.spell.InscribedSpell;
import phapthuat.spell.Shape;
import phapthuat.spell.SpellCaster;
import phapthuat.spell.WandSlots;
import phapthuat.spell.impl.HeatSpell;

/** Dùng Pháp Trượng phía server: thi triển ô đang chọn, đổi ô, phí phóng đạn. */
@GameTestHolder(PhapThuat.MODID)
@PrefixGameTestTemplate(false)
public class WandGameTests {
    private static ServerPlayer holding(GameTestHelper helper, WandSlots slots, double x, double y, double z,
            float yaw, float pitch) {
        ServerPlayer player = TargetingGameTests.playerAt(helper, x, y, z, yaw, pitch);
        ItemStack wand = new ItemStack(ModItems.WAND.get());
        wand.set(ModDataComponents.WAND_SLOTS.get(), slots);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        return player;
    }

    @GameTest(template = ThermalGameTests.EMPTY)
    public static void wandCastsSelectedSlot(GameTestHelper helper) {
        BlockPos wood = new BlockPos(3, 3, 3);
        helper.setBlock(wood, Blocks.OAK_PLANKS);
        WandSlots slots = WandSlots.EMPTY.with(0, new InscribedSpell(HeatSpell.ID, Shape.DIEM, Map.of("energy", 33.4)));
        // Đứng trên nền (y = 2), nhìn về hướng +X, mắt ngang khối gỗ.
        ServerPlayer player = holding(helper, slots, 0.5, 2, 3.5, -90, 0);
        player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(helper.getBlockState(wood.west()).is(BlockTags.FIRE), "mặt gỗ đang nhìn phải bốc cháy");
        helper.assertTrue(Math.abs(ManaManager.get(player) - (1000 - 33.4)) < 1e-6,
                "phải trừ 33,4 mana, còn " + ManaManager.get(player));
        helper.assertTrue(player.getCooldowns().isOnCooldown(ModItems.WAND.get()), "trượng phải hồi chiêu");
        helper.succeed();
    }

    @GameTest(template = ThermalGameTests.EMPTY)
    public static void emptySlotCostsNothing(GameTestHelper helper) {
        ServerPlayer player = holding(helper, WandSlots.EMPTY, 0.5, 2, 3.5, -90, 0);
        player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(ManaManager.get(player) == 1000, "ô trống không được tốn mana");
        helper.succeed();
    }

    @GameTest(template = ThermalGameTests.EMPTY)
    public static void selectSlotCyclesAndClampsDelta(GameTestHelper helper) {
        ServerPlayer player = holding(helper, WandSlots.EMPTY, 0.5, 2, 3.5, 0, 0);
        WandItem.cycleSelected(player, -1);
        helper.assertTrue(selected(player) == 4, "lăn lùi từ ô 1 phải về ô 5");
        // Gói tin giả mạo với delta lớn chỉ được đổi 1 ô.
        WandItem.cycleSelected(player, 1_000_000);
        helper.assertTrue(selected(player) == 0, "delta lớn chỉ được tính là +1, đang ở ô " + (selected(player) + 1));
        helper.succeed();
    }

    private static int selected(ServerPlayer player) {
        return player.getMainHandItem().getOrDefault(ModDataComponents.WAND_SLOTS.get(), WandSlots.EMPTY).selected();
    }

    @GameTest(template = ThermalGameTests.EMPTY)
    public static void projectileChargesKineticEnergy(GameTestHelper helper) {
        TargetingGameTests.clearSkyAbove(helper, 3, 3);
        ServerPlayer player = TargetingGameTests.playerAt(helper, 3.5, 2, 3.5, 0, -90);
        SpellCaster.cast(player, new InscribedSpell(HeatSpell.ID, Shape.DAN, Map.of("energy", 10.0, "speed", 100.0)));
        // ½ · 0,1 kg · (100 m/s)² = 500 J = 0,05 mana.
        double expected = 1000 - 10 - 0.05;
        helper.assertTrue(Math.abs(ManaManager.get(player) - expected) < 1e-6,
                "mana phải còn " + expected + ", còn " + ManaManager.get(player));
        helper.getLevel().getEntitiesOfClass(SpellProjectile.class, new AABB(player.blockPosition()).inflate(40))
                .forEach(SpellProjectile::discard);
        helper.succeed();
    }
}
