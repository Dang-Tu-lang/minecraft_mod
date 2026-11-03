package phapthuat.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import phapthuat.physics.EntityMass;
import phapthuat.physics.Material;
import phapthuat.physics.PhysicsConstants;
import phapthuat.spell.Fmt;
import phapthuat.thermal.BlockThermal;
import phapthuat.thermal.EntityThermal;
import phapthuat.thermal.MaterialLookup;
import phapthuat.thermal.WorldTemps;
import phapthuat.util.Raycast;

/** Số liệu Máy Đo hiển thị (spec mục 6.4). Chỉ đo, không bao giờ tính hộ chi phí của phép. */
public final class MeterReadout {
    public static final double RANGE = 16;

    private MeterReadout() {}

    public static void send(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        double ambient = WorldTemps.ambientC(level, player.blockPosition());
        say(player, Component.translatable("phapthuat.meter.ambient", Fmt.tempC(ambient),
                Fmt.num(PhysicsConstants.toKelvin(ambient), 1)));

        HitResult hit = Raycast.look(player, RANGE);
        boolean inWater = false;
        if (hit instanceof EntityHitResult eh) {
            Entity e = eh.getEntity();
            inWater = e.isInWater();
            if (e instanceof LivingEntity living) {
                EntityThermal t = WorldTemps.currentEntity(living);
                say(player, Component.translatable("phapthuat.meter.entity", e.getDisplayName(), Fmt.tempC(t.coreTempC())));
                say(player, Component.translatable("phapthuat.meter.mass", Fmt.num(massKg(e), 0)));
                say(player, Component.translatable("phapthuat.meter.dose", Fmt.num(t.doseGy(), 2)));
            } else {
                say(player, Component.translatable("phapthuat.meter.entity_plain", e.getDisplayName()));
            }
            say(player, distance(player, eh.getLocation().distanceTo(player.getEyePosition())));
        } else if (hit instanceof BlockHitResult bh && hit.getType() != HitResult.Type.MISS) {
            BlockPos pos = bh.getBlockPos();
            BlockState state = level.getBlockState(pos);
            inWater = WorldTemps.inWater(level, pos);
            var material = MaterialLookup.of(state);
            if (material.isPresent()) {
                Material m = material.get();
                BlockThermal t = WorldTemps.currentBlock(level, pos, m);
                say(player, Component.translatable("phapthuat.meter.block",
                        Component.translatable("phapthuat.material." + m.id()), Fmt.tempC(t.tempC())));
                double latent = m.latentAt(t.tempC()) * m.coreMassKg();
                if (latent > 0 && t.phaseProgressJ() >= 1) {
                    say(player, Component.translatable("phapthuat.meter.phase",
                            Fmt.num(100 * Math.min(1, t.phaseProgressJ() / latent), 0)));
                }
            } else {
                say(player, Component.translatable("phapthuat.meter.block_unknown", state.getBlock().getName()));
            }
            say(player, distance(player, bh.getLocation().distanceTo(player.getEyePosition())));
        } else {
            say(player, Component.translatable("phapthuat.meter.nothing"));
        }

        say(player, Component.translatable("phapthuat.meter.rain", yesNo(level.isRainingAt(player.blockPosition()))));
        say(player, Component.translatable("phapthuat.meter.in_water", yesNo(inWater)));
    }

    public static double massKg(Entity e) {
        var bb = e.getBoundingBox();
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString();
        return EntityMass.kg(id, bb.getXsize() * bb.getYsize() * bb.getZsize());
    }

    private static Component distance(ServerPlayer player, double meters) {
        return Component.translatable("phapthuat.meter.distance", Fmt.num(meters, 1));
    }

    private static Component yesNo(boolean b) {
        return Component.translatable(b ? "phapthuat.yes" : "phapthuat.no");
    }

    private static void say(ServerPlayer player, Component c) {
        player.sendSystemMessage(c);
    }
}
