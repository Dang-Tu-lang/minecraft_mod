package phapthuat.spell;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
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
        Vec3 center, @Nullable Direction face) {

    /**
     * Ô nằm ngay ngoài mặt khối đang nhìn — ô không khí trước mặt người chơi. Với Điểm, {@code center} là điểm chạm trên
     * mặt khối nên {@code BlockPos.containing(center)} trả về chính khối rắn đó (mặt tây/bắc/dưới) hoặc ô bên trong
     * khối; đây mới là ô mà phép đặt quả cầu sáng hay đếm oxy.
     */
    public BlockPos airCell() {
        BlockPos cell = BlockPos.containing(center);
        if (face == null) return cell;
        BlockPos outside = cell.relative(face);
        return level.getBlockState(cell).isAir() ? cell : outside;
    }
}
