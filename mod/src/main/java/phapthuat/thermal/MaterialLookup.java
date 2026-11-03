package phapthuat.thermal;

import java.util.Optional;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.common.Tags;
import phapthuat.physics.Material;
import phapthuat.physics.Materials;

/** Khối Minecraft → vật liệu trong bảng (spec mục 7.2). Khối không có trong bảng trả về rỗng. */
public final class MaterialLookup {
    private MaterialLookup() {}

    public static Optional<Material> of(BlockState s) {
        return Optional.ofNullable(lookup(s));
    }

    private static Material lookup(BlockState s) {
        if (s.isAir()) return Materials.AIR;
        if (s.hasProperty(BlockStateProperties.WATERLOGGED) && s.getValue(BlockStateProperties.WATERLOGGED)) {
            return Materials.WATER;
        }
        if (s.is(Blocks.WATER)) return s.getFluidState().isSource() ? Materials.WATER : null;
        if (s.is(Blocks.LAVA)) return s.getFluidState().isSource() ? Materials.LAVA : null;
        if (s.is(Blocks.WET_SPONGE) || s.is(Blocks.MUD)) return Materials.WATER;
        if (s.getBlock() instanceof CampfireBlock) {
            return s.getValue(CampfireBlock.LIT) ? Materials.CAMPFIRE : Materials.WOOD;
        }
        if (s.is(BlockTags.LOGS) || s.is(BlockTags.PLANKS) || s.is(BlockTags.WOODEN_SLABS)
                || s.is(BlockTags.WOODEN_STAIRS) || s.is(BlockTags.WOODEN_FENCES)) {
            return Materials.WOOD;
        }
        if (s.is(BlockTags.WOOL)) return Materials.WOOL;
        if (s.is(BlockTags.LEAVES)) return Materials.LEAVES;
        if (s.is(Blocks.HAY_BLOCK)) return Materials.HAY;
        if (s.is(Blocks.COAL_BLOCK)) return Materials.COAL_BLOCK;
        if (s.is(BlockTags.CANDLES)) return Materials.WAX;
        if (s.is(Blocks.TNT)) return Materials.TNT;
        if (s.is(Blocks.ICE) || s.is(Blocks.PACKED_ICE)) return Materials.ICE;
        if (s.is(Blocks.SNOW_BLOCK)) return Materials.SNOW;
        if (s.is(Blocks.SAND) || s.is(Blocks.RED_SAND)) return Materials.SAND;
        if (s.is(Blocks.STONE) || s.is(Blocks.COBBLESTONE) || s.is(Blocks.DEEPSLATE) || s.is(Blocks.COBBLED_DEEPSLATE)
                || s.is(Blocks.GRANITE) || s.is(Blocks.DIORITE) || s.is(Blocks.ANDESITE)) {
            return Materials.STONE;
        }
        if (s.is(Tags.Blocks.GLASS_BLOCKS) || s.is(Tags.Blocks.GLASS_PANES)) return Materials.GLASS;
        if (s.is(Blocks.OBSIDIAN)) return Materials.OBSIDIAN;
        if (s.is(Blocks.MAGMA_BLOCK)) return Materials.MAGMA;
        if (s.is(BlockTags.FIRE)) return Materials.FIRE;
        return null;
    }
}
