package phapthuat.gametest;

import java.util.Map;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import phapthuat.PhapThuat;
import phapthuat.item.WandItem;
import phapthuat.registry.ModItems;
import phapthuat.spell.InscribeValidator;
import phapthuat.spell.InscribedSpell;
import phapthuat.spell.Shape;
import phapthuat.spell.WandSlots;
import phapthuat.spell.impl.HeatSpell;
import phapthuat.spell.impl.SiphonSpell;

/** Khắc phép từ sách vào Pháp Trượng: dữ liệu từ client không bao giờ được ghi giá trị lạ vào trượng. */
@GameTestHolder(PhapThuat.MODID)
@PrefixGameTestTemplate(false)
public class InscribeGameTests {
    private static ServerPlayer withOffhandWand(GameTestHelper helper) {
        ServerPlayer player = TargetingGameTests.playerAt(helper, 0.5, 2, 0.5, 0, 0);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.WAND.get()));
        return player;
    }

    private static WandSlots offhand(ServerPlayer player) {
        return WandItem.slots(player.getOffhandItem());
    }

    @GameTest(template = ThermalGameTests.EMPTY)
    public static void inscribeClampsAndWrites(GameTestHelper helper) {
        ServerPlayer player = withOffhandWand(helper);
        boolean ok = InscribeValidator.inscribe(player, 2,
                new InscribedSpell(HeatSpell.ID, Shape.DIEM, Map.of("energy", 1e9, "bogus", 5.0)));
        helper.assertTrue(ok, "khắc hợp lệ phải thành công");
        InscribedSpell written = offhand(player).get(2).orElseThrow();
        helper.assertTrue(written.param("energy") == 1000, "năng lượng phải kẹp về 1000, có " + written.param("energy"));
        helper.assertFalse(written.params().containsKey("bogus"), "khóa lạ phải bị bỏ");
        helper.succeed();
    }

    @GameTest(template = ThermalGameTests.EMPTY)
    public static void inscribeFillsMissingShapeParams(GameTestHelper helper) {
        ServerPlayer player = withOffhandWand(helper);
        InscribeValidator.inscribe(player, 0, new InscribedSpell(HeatSpell.ID, Shape.CAU, Map.of("energy", 50.0)));
        InscribedSpell written = offhand(player).get(0).orElseThrow();
        helper.assertTrue(written.param("radius") == 2, "bán kính thiếu phải lấy mặc định 2, có " + written.param("radius"));
        helper.succeed();
    }

    @GameTest(template = ThermalGameTests.EMPTY)
    public static void inscribeRejectsWrongShape(GameTestHelper helper) {
        ServerPlayer player = withOffhandWand(helper);
        boolean ok = InscribeValidator.inscribe(player, 0,
                new InscribedSpell(SiphonSpell.ID, Shape.TIA, Map.of("heat", 10.0, "length", 8.0)));
        helper.assertFalse(ok, "Thu Nhiệt không có thao tác Tia");
        helper.assertTrue(offhand(player).equals(WandSlots.EMPTY), "trượng không được đổi");
        helper.succeed();
    }

    @GameTest(template = ThermalGameTests.EMPTY)
    public static void inscribeRejectsNaNUnknownSpellAndBadSlot(GameTestHelper helper) {
        ServerPlayer player = withOffhandWand(helper);
        InscribedSpell good = new InscribedSpell(HeatSpell.ID, Shape.DIEM, Map.of("energy", 10.0));
        helper.assertFalse(InscribeValidator.inscribe(player, 0,
                new InscribedSpell(HeatSpell.ID, Shape.DIEM, Map.of("energy", Double.NaN))), "NaN phải bị từ chối");
        helper.assertFalse(InscribeValidator.inscribe(player, 0,
                new InscribedSpell("khong_co", Shape.DIEM, Map.of())), "phép không tồn tại phải bị từ chối");
        helper.assertFalse(InscribeValidator.inscribe(player, 5, good), "ô 6 không tồn tại");
        helper.assertFalse(InscribeValidator.inscribe(player, -1, good), "ô âm không tồn tại");
        helper.assertTrue(offhand(player).equals(WandSlots.EMPTY), "trượng không được đổi");
        helper.succeed();
    }

    @GameTest(template = ThermalGameTests.EMPTY)
    public static void inscribeRequiresWandInOffhand(GameTestHelper helper) {
        ServerPlayer player = TargetingGameTests.playerAt(helper, 0.5, 2, 0.5, 0, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.WAND.get()));
        boolean ok = InscribeValidator.inscribe(player, 0,
                new InscribedSpell(HeatSpell.ID, Shape.DIEM, Map.of("energy", 10.0)));
        helper.assertFalse(ok, "tay phụ trống thì không khắc được");
        helper.assertTrue(WandItem.slots(player.getMainHandItem()).equals(WandSlots.EMPTY),
                "trượng ở tay chính không được bị ghi");
        helper.succeed();
    }
}
