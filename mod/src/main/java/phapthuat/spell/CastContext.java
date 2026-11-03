package phapthuat.spell;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Bối cảnh một lần thi triển.
 *
 * @param face mặt khối mà người chơi đang nhìn (với Điểm và đạn chạm khối); null nếu không có
 */
public record CastContext(ServerLevel level, ServerPlayer caster, InscribedSpell spell, double joulesPerMana,
        Vec3 center, @Nullable Direction face) {}
