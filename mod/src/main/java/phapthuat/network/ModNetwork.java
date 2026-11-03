package phapthuat.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import phapthuat.item.WandItem;
import phapthuat.mana.ClientManaCache;

/** Đăng ký mọi gói tin của mod. Các task sau thêm gói tin vào đây. */
public final class ModNetwork {
    private ModNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(ManaSyncPayload.TYPE, ManaSyncPayload.STREAM_CODEC,
                (payload, context) -> ClientManaCache.set(payload.mana(), payload.max()));
        registrar.playToServer(SelectSlotPayload.TYPE, SelectSlotPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) WandItem.cycleSelected(player, payload.delta());
        });
    }
}
