package phapthuat.spell;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

/** Một đơn vị chịu tác động của phép: một khối, một ô không khí (1 m³) hoặc một thực thể (spec mục 6.5). */
public sealed interface SpellUnit {
    record BlockUnit(BlockPos pos) implements SpellUnit {}

    record AirUnit(BlockPos pos) implements SpellUnit {}

    record EntityUnit(Entity entity) implements SpellUnit {}
}
