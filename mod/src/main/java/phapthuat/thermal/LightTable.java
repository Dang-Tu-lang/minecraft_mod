package phapthuat.thermal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Các khối sáng tạm của Phát Quang và thời điểm hết hạn (game time). Java thuần. */
public final class LightTable {
    private final Map<Long, Long> expiresAt = new LinkedHashMap<>();

    public void add(long pos, long expiresAtGameTime) {
        expiresAt.put(pos, expiresAtGameTime);
    }

    /** Các vị trí đã hết hạn tại thời điểm {@code nowGameTime} (chưa xóa khỏi bảng). */
    public List<Long> expired(long nowGameTime) {
        List<Long> out = new ArrayList<>();
        for (Map.Entry<Long, Long> e : expiresAt.entrySet()) {
            if (e.getValue() <= nowGameTime) out.add(e.getKey());
        }
        return out;
    }

    public void remove(long pos) {
        expiresAt.remove(pos);
    }

    public boolean isEmpty() {
        return expiresAt.isEmpty();
    }

    public Map<Long, Long> entries() {
        return expiresAt;
    }
}
