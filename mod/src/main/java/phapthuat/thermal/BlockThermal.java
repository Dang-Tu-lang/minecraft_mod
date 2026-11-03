package phapthuat.thermal;

import phapthuat.physics.Cooling;

/**
 * Trạng thái nhiệt đã lưu của một khối (spec mục 6.8). Java thuần.
 *
 * @param materialId vật liệu lúc lưu; khối ở vị trí đó đổi vật liệu thì mục này bị bỏ
 * @param phaseProgressJ nhiệt chuyển pha ở bình nguyên hiện tại: dương là đã hấp thụ khi đun (đang chảy/sôi dở),
 *     âm là đã bị rút khi làm lạnh (đang đông dở)
 */
public record BlockThermal(String materialId, double tempC, double phaseProgressJ, double naturalC,
        double halfLifeSeconds) {

    public BlockThermal decayed(double seconds) {
        return new BlockThermal(materialId, Cooling.relax(tempC, naturalC, seconds, halfLifeSeconds),
                Cooling.relax(phaseProgressJ, 0, seconds, halfLifeSeconds), naturalC, halfLifeSeconds);
    }

    public boolean settled() {
        return Math.abs(tempC - naturalC) < 1 && Math.abs(phaseProgressJ) < 1;
    }
}
