package phapthuat.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
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
}
