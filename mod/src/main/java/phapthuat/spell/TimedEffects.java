package phapthuat.spell;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Chạy các {@link TimedEffect} mỗi tick. Không lưu qua lần thoát game vì hiệu ứng nào cũng ngắn; khối đặt tạm vào
 * thế giới phải có cơ chế dọn riêng.
 */
public final class TimedEffects {
    private static final class Running {
        final TimedEffect effect;
        int age;

        Running(TimedEffect effect) {
            this.effect = effect;
        }
    }

    private static final Map<ServerLevel, List<Running>> RUNNING = new WeakHashMap<>();

    private TimedEffects() {}

    public static void add(ServerLevel level, TimedEffect effect) {
        RUNNING.computeIfAbsent(level, l -> new ArrayList<>()).add(new Running(effect));
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        List<Running> list = RUNNING.get(level);
        if (list == null || list.isEmpty()) return;
        // Sao chép trước khi chạy: một hiệu ứng có thể thêm hiệu ứng mới.
        List<Running> snapshot = new ArrayList<>(list);
        List<Running> finished = new ArrayList<>();
        for (Running r : snapshot) {
            if (!r.effect.tick(level, r.age++)) finished.add(r);
        }
        Iterator<Running> it = list.iterator();
        while (it.hasNext()) {
            if (finished.contains(it.next())) it.remove();
        }
    }
}
