package phapthuat.thermal;

import phapthuat.physics.Cooling;

/**
 * Trạng thái nhiệt đã lưu của một khối (spec mục 6.8). Java thuần.
 *
 * @param materialId vật liệu lúc lưu; khối ở vị trí đó đổi vật liệu thì mục này bị bỏ
 * @param phaseProgressJ nhiệt chuyển pha đã nhận (hoặc đã nhả, với dung nham) ở bình nguyên hiện tại
 */
public record BlockThermal(String materialId, double tempC, double phaseProgressJ, double naturalC,
        double halfLifeSeconds) {

    public BlockThermal decayed(double seconds) {
        return new BlockThermal(materialId, Cooling.relax(tempC, naturalC, seconds, halfLifeSeconds),
                Cooling.relax(phaseProgressJ, 0, seconds, halfLifeSeconds), naturalC, halfLifeSeconds);
    }

    public boolean settled() {
        return Math.abs(tempC - naturalC) < 1 && phaseProgressJ < 1;
    }
}
