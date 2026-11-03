package phapthuat.mana;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import phapthuat.Config;
import phapthuat.network.ManaSyncPayload;

/** Đọc, trừ, cộng và hồi mana của người chơi phía server; đồng bộ xuống client khi số hiển thị thay đổi. */
public final class ManaManager {
    /** Giá trị đã gửi gần nhất (làm tròn 0,1) của từng người chơi, để không gửi gói thừa mỗi tick. */
    private static final Map<UUID, Long> LAST_SENT = new HashMap<>();

    private ManaManager() {}

    public static double get(Player player) {
        return player.getData(ManaAttachments.MANA);
    }

    public static boolean trySpend(ServerPlayer player, double mana) {
        ManaPool pool = pool(player);
        if (!pool.trySpend(mana)) return false;
        store(player, pool);
        return true;
    }

    public static void add(ServerPlayer player, double mana) {
        ManaPool pool = pool(player);
        pool.add(mana);
        store(player, pool);
    }

    public static void set(ServerPlayer player, double mana) {
        store(player, new ManaPool(Config.MAX_MANA.get(), mana));
    }

    private static ManaPool pool(Player player) {
        return new ManaPool(Config.MAX_MANA.get(), get(player));
    }

    private static void store(ServerPlayer player, ManaPool pool) {
        player.setData(ManaAttachments.MANA, pool.current());
        sync(player, false);
    }

    public static void sync(ServerPlayer player, boolean force) {
        // Người chơi giả của GameTest không có kênh mạng của mod.
        if (player.connection == null || !player.connection.hasChannel(ManaSyncPayload.TYPE)) return;
        long shown = Math.round(get(player) * 10);
        Long last = LAST_SENT.get(player.getUUID());
        if (!force && last != null && last == shown) return;
        LAST_SENT.put(player.getUUID(), shown);
        PacketDistributor.sendToPlayer(player, new ManaSyncPayload(get(player), Config.MAX_MANA.get()));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ManaPool pool = pool(player);
        if (pool.current() >= pool.max()) return;
        pool.regen(1.0 / 20, Config.REGEN_PER_SECOND.get());
        store(player, pool);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player, true);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player, true);
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player, true);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SENT.remove(event.getEntity().getUUID());
    }
}
