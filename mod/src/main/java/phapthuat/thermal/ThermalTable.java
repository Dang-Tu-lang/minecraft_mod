package phapthuat.thermal;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Bảng nhiệt thưa của một thế giới, khóa là {@code BlockPos.asLong()}. Java thuần.
 * Nguội dần được tính lười theo thời gian đã trôi qua mỗi khi đọc, nên giá trị luôn chính xác tới từng tick.
 * Vượt số mục tối đa thì bỏ mục được ghi lâu nhất.
 */
public final class ThermalTable {
    public static final double TICKS_PER_SECOND = 20;

    public record Entry(BlockThermal value, long updatedAtTick) {}

    private final int maxEntries;
    private final LinkedHashMap<Long, Entry> entries = new LinkedHashMap<>();

    public ThermalTable(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    public Optional<BlockThermal> get(long key, long nowTick) {
        Entry e = entries.get(key);
        if (e == null) return Optional.empty();
        return Optional.of(e.value().decayed((nowTick - e.updatedAtTick()) / TICKS_PER_SECOND));
    }

    public void put(long key, BlockThermal value, long nowTick) {
        entries.remove(key);
        entries.put(key, new Entry(value, nowTick));
        while (entries.size() > maxEntries) {
            Iterator<Long> oldest = entries.keySet().iterator();
            oldest.next();
            oldest.remove();
        }
    }

    public void remove(long key) {
        entries.remove(key);
    }

    /** Bỏ các mục đã về gần trạng thái tự nhiên. */
    public void sweep(long nowTick) {
        entries.entrySet().removeIf(e -> e.getValue().value()
                .decayed((nowTick - e.getValue().updatedAtTick()) / TICKS_PER_SECOND).settled());
    }

    public int size() {
        return entries.size();
    }

    public Map<Long, Entry> entries() {
        return entries;
    }
}
