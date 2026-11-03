package phapthuat.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Tìm thứ người chơi đang nhìn: sinh vật gần nhất, hoặc khối (kể cả chất lỏng nguồn). */
public final class Raycast {
    private Raycast() {}

    public static HitResult look(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 dir = player.getViewVector(1.0f);
        Vec3 end = eye.add(dir.scale(range));
        BlockHitResult block = player.level().clip(
                new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, player));
        Vec3 entityEnd = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        AABB box = player.getBoundingBox().expandTowards(dir.scale(range)).inflate(1.0);
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(player, eye, entityEnd, box,
                e -> !e.isSpectator() && e.isPickable(), eye.distanceToSqr(entityEnd));
        return entity != null ? entity : block;
    }
}
