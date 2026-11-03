package phapthuat.thermal;

import java.util.Map;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import phapthuat.Config;
import phapthuat.PhapThuat;

/** Bảng nhiệt của một thế giới, lưu cùng file save (spec mục 6.8). */
public final class ThermalStore extends SavedData {
    private static final String NAME = PhapThuat.MODID + "_thermal";

    private final ThermalTable table = new ThermalTable(Config.MAX_TRACKED_BLOCKS.get());

    public static ThermalStore get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ThermalStore::new, ThermalStore::load, null), NAME);
    }

    public Optional<BlockThermal> get(BlockPos pos, long nowTick) {
        return table.get(pos.asLong(), nowTick);
    }

    public void put(BlockPos pos, BlockThermal value, long nowTick) {
        table.put(pos.asLong(), value, nowTick);
        setDirty();
    }

    public void remove(BlockPos pos) {
        if (table.get(pos.asLong(), 0).isPresent()) {
            table.remove(pos.asLong());
            setDirty();
        }
    }

    public void sweep(long nowTick) {
        int before = table.size();
        table.sweep(nowTick);
        if (table.size() != before) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<Long, ThermalTable.Entry> e : table.entries().entrySet()) {
            BlockThermal v = e.getValue().value();
            CompoundTag c = new CompoundTag();
            c.putLong("p", e.getKey());
            c.putString("m", v.materialId());
            c.putDouble("t", v.tempC());
            c.putDouble("q", v.phaseProgressJ());
            c.putDouble("n", v.naturalC());
            c.putDouble("h", v.halfLifeSeconds());
            c.putLong("u", e.getValue().updatedAtTick());
            list.add(c);
        }
        tag.put("entries", list);
        return tag;
    }

    private static ThermalStore load(CompoundTag tag, HolderLookup.Provider registries) {
        ThermalStore store = new ThermalStore();
        for (Tag t : tag.getList("entries", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            store.table.put(c.getLong("p"), new BlockThermal(c.getString("m"), c.getDouble("t"), c.getDouble("q"),
                    c.getDouble("n"), c.getDouble("h")), c.getLong("u"));
        }
        return store;
    }
}
