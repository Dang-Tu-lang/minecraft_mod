package phapthuat.thermal;

import phapthuat.physics.Cooling;

/**
 * Trạng thái lưu trên một sinh vật: nhiệt độ lõi 1 lít (dùng chung cho Gia Nhiệt và Thu Nhiệt) và liều phóng xạ
 * cộng dồn (spec mục 6.8). Java thuần.
 */
public record EntityThermal(double coreTempC, double doseGy, long updatedAtTick) {

    public EntityThermal decayedTo(long nowTick, double naturalC, double tempHalfLife, double doseHalfLife) {
        double seconds = (nowTick - updatedAtTick) / ThermalTable.TICKS_PER_SECOND;
        return new EntityThermal(Cooling.relax(coreTempC, naturalC, seconds, tempHalfLife),
                Cooling.relax(doseGy, 0, seconds, doseHalfLife), nowTick);
    }

    public boolean settled(double naturalC) {
        return Math.abs(coreTempC - naturalC) < 1 && doseGy < 0.05;
    }
}
