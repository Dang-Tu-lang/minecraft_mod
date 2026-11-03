package phapthuat.entity;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import phapthuat.physics.Ballistics;
import phapthuat.registry.ModEntities;
import phapthuat.spell.InscribedSpell;
import phapthuat.spell.SpellCaster;
import phapthuat.spell.SpellUnit;

/**
 * Đạn phép 0,1 kg bay theo chuyển động ném xiên thật (g = 9,81 m/s², bỏ qua lực cản), không dùng trọng lực vanilla.
 * Chạm thì áp phép như thao tác Điểm tại chỗ chạm (spec mục 6.5).
 */
public class SpellProjectile extends Projectile {
    public static final double MASS_KG = 0.1;
    public static final int LIFETIME_TICKS = 200;
    /** Sau ngần này tick bay, đạn có thể chạm chính người bắn. */
    public static final int OWNER_IMMUNE_TICKS = 20;

    @Nullable
    private InscribedSpell spell;

    public SpellProjectile(EntityType<? extends SpellProjectile> type, Level level) {
        super(type, level);
    }

    /** Động năng lúc phóng, ½·m·v² (J). */
    public static double launchEnergyJ(double speed) {
        return 0.5 * MASS_KG * speed * speed;
    }

    /** Bắn từ mắt người chơi theo hướng nhìn với tốc độ {@code speed} m/s. */
    public static SpellProjectile launch(ServerPlayer owner, InscribedSpell spell, double speed) {
        SpellProjectile p = new SpellProjectile(ModEntities.SPELL_PROJECTILE.get(), owner.level());
        p.spell = spell;
        p.setOwner(owner);
        Vec3 eye = owner.getEyePosition();
        p.setPos(eye.x, eye.y - 0.1, eye.z);
        p.setDeltaMovement(owner.getViewVector(1.0f).scale(speed / Ballistics.TICKS_PER_SECOND));
        owner.level().addFreshEntity(p);
        return p;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public void tick() {
        super.tick();
        Vec3 v = getDeltaMovement();
        if (level().isClientSide()) {
            level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), 0, 0, 0);
        } else {
            if (tickCount > LIFETIME_TICKS || spell == null) {
                discard();
                return;
            }
            if (isInWater()) {
                ((ServerLevel) level()).sendParticles(ParticleTypes.CLOUD, getX(), getY(), getZ(), 8, 0.2, 0.2, 0.2, 0.02);
                discard();
                return;
            }
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS) {
                onHit(hit);
                return;
            }
        }
        setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
        double vyPerSecond = v.y * Ballistics.TICKS_PER_SECOND;
        setDeltaMovement(v.x, Ballistics.vyAfterTick(vyPerSecond) / Ballistics.TICKS_PER_SECOND, v.z);
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (target == getOwner()) return tickCount >= OWNER_IMMUNE_TICKS;
        return super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        impact(List.of(new SpellUnit.EntityUnit(result.getEntity())), result.getLocation(), null);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        impact(List.of(new SpellUnit.BlockUnit(result.getBlockPos().immutable())), result.getLocation(), result.getDirection());
    }

    private void impact(List<SpellUnit> units, Vec3 at, @Nullable net.minecraft.core.Direction face) {
        if (level().isClientSide()) return;
        if (getOwner() instanceof ServerPlayer owner && spell != null) {
            SpellCaster.applyOnUnits(owner, spell, units, at, face);
        }
        discard();
    }
}
