package phapthuat.thermal;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import phapthuat.PhapThuat;

/**
 * Khối sáng tạm của Phát Quang, lưu cùng file save để dù game bị thoát giữa chừng thì khối sáng vẫn được dọn: mỗi
 * 20 tick (và vì thế ngay sau khi tải thế giới) mọi khối đã hết hạn bị xóa.
 */
public final class PlacedLights extends SavedData {
    private static final String NAME = PhapThuat.MODID + "_lights";

    private final LightTable table = new LightTable();

    public static PlacedLights get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(PlacedLights::new, PlacedLights::load, null), NAME);
    }

    public void add(BlockPos pos, long expiresAtGameTime) {
        table.add(pos.asLong(), expiresAtGameTime);
        setDirty();
    }

    public boolean isEmpty() {
        return table.isEmpty();
    }

    /** Xóa các khối sáng đã hết hạn. Khối nằm ở chunk chưa tải thì chờ tới khi chunk được tải. */
    public void sweep(ServerLevel level) {
        for (long key : table.expired(level.getGameTime())) {
            BlockPos pos = BlockPos.of(key);
            if (!level.isLoaded(pos)) continue;
            if (level.getBlockState(pos).is(Blocks.LIGHT)) level.removeBlock(pos, false);
            table.remove(key);
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<Long, Long> e : table.entries().entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putLong("p", e.getKey());
            c.putLong("t", e.getValue());
            list.add(c);
        }
        tag.put("lights", list);
        return tag;
    }

    private static PlacedLights load(CompoundTag tag, HolderLookup.Provider registries) {
        PlacedLights lights = new PlacedLights();
        for (Tag t : tag.getList("lights", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            lights.table.add(c.getLong("p"), c.getLong("t"));
        }
        return lights;
    }
}
