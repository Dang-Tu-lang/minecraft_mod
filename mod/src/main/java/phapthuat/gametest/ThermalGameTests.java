package phapthuat.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import phapthuat.PhapThuat;
import phapthuat.physics.Materials;
import phapthuat.thermal.BlockThermal;
import phapthuat.thermal.WorldTemps;

@GameTestHolder(PhapThuat.MODID)
@PrefixGameTestTemplate(false)
public class ThermalGameTests {
    public static final String EMPTY = "empty_7x7x7";

    @GameTest(template = EMPTY, timeoutTicks = 500)
    public static void storedTempDecays(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(3, 1, 3));
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.OAK_PLANKS);
        ServerLevel level = helper.getLevel();
        double natural = WorldTemps.naturalC(level, pos, Materials.WOOD);
        WorldTemps.saveBlock(level, pos, new BlockThermal(Materials.WOOD.id(), 300, 0, natural, 20));

        helper.runAfterDelay(400, () -> {
            double temp = WorldTemps.currentBlock(level, pos, Materials.WOOD).tempC();
            double expected = natural + (300 - natural) * 0.5;
            helper.assertTrue(Math.abs(temp - expected) <= 3,
                    "nhiệt độ sau 20 giây là " + temp + ", mong đợi ≈ " + expected);
            helper.succeed();
        });
    }

    /** Review Focus 3: khối mới đặt vào chỗ khối cũ (cùng vật liệu) không được thừa hưởng nhiệt đã lưu. */
    @GameTest(template = EMPTY)
    public static void placingBlockClearsStoredHeat(GameTestHelper helper) {
        BlockPos rel = new BlockPos(3, 2, 3);
        BlockPos pos = helper.absolutePos(rel);
        ServerLevel level = helper.getLevel();
        double natural = WorldTemps.naturalC(level, pos, Materials.WOOD);
        WorldTemps.saveBlock(level, pos, new BlockThermal(Materials.WOOD.id(), 300, 0, natural, 20));
        ServerPlayer player = TargetingGameTests.playerAt(helper, 0.5, 2, 0.5, 0, 0);
        // Vật phẩm đặt lấy từ tay người chơi, không phải từ stack truyền vào.
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS));
        helper.placeAt(player, player.getMainHandItem(), new BlockPos(3, 1, 3), Direction.UP);
        helper.assertBlockPresent(Blocks.OAK_PLANKS, rel);
        double temp = WorldTemps.currentBlock(level, pos, Materials.WOOD).tempC();
        helper.assertTrue(Math.abs(temp - natural) < 1, "ván mới đặt phải ở nhiệt độ tự nhiên, có " + temp);
        helper.succeed();
    }

    /** Máy Đo và phép dùng chung luật oxy WorldTemps.inWater: chỉ nước (không phải dung nham), xét cả 6 mặt. */
    @GameTest(template = EMPTY)
    public static void meterInWaterMatchesOxygenRule(GameTestHelper helper) {
        BlockPos lava = new BlockPos(1, 1, 1);
        BlockPos plank = new BlockPos(4, 2, 4);
        helper.setBlock(lava, Blocks.LAVA);
        helper.setBlock(plank, Blocks.OAK_PLANKS);
        helper.setBlock(plank.north(), Blocks.WATER);
        ServerLevel level = helper.getLevel();
        helper.assertFalse(WorldTemps.inWater(level, helper.absolutePos(lava)),
                "dung nham không phải nước");
        helper.assertTrue(WorldTemps.inWater(level, helper.absolutePos(plank)),
                "ván có nước ở mặt bắc là trong nước, dù nhìn từ trên xuống");
        helper.succeed();
    }
}
