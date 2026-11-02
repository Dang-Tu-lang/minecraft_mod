package phapthuat.spell;

import net.minecraft.server.level.ServerLevel;

/** Hiệu ứng kéo dài nhiều tick (đám hơi, quả cầu sáng...). Trả false để kết thúc. */
public interface TimedEffect {
    boolean tick(ServerLevel level, int ageTicks);
}
