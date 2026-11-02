package phapthuat.thermal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Dọn bảng nhiệt và trạng thái sinh vật đã về bình thường. Hiệu ứng theo bậc (lạnh, phóng xạ) cũng chạy ở đây. */
public final class ThermalTicker {
    private ThermalTicker() {}

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % 200 == 0) {
            ThermalStore.get(level).sweep(level.getGameTime());
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity) || entity.level().isClientSide()) return;
        if (entity.tickCount % 20 != 0 || !entity.hasData(ThermalAttachments.ENTITY_THERMAL)) return;
        EntityThermal now = WorldTemps.currentEntity(entity);
        if (now.settled(WorldTemps.naturalEntityC(entity))) {
            entity.removeData(ThermalAttachments.ENTITY_THERMAL);
        } else {
            entity.setData(ThermalAttachments.ENTITY_THERMAL, now);
        }
    }
}
