package phapthuat.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import phapthuat.mana.ClientManaCache;

/** Đăng ký mọi gói tin của mod. Các task sau thêm gói tin vào đây. */
public final class ModNetwork {
    private ModNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(ManaSyncPayload.TYPE, ManaSyncPayload.STREAM_CODEC,
                (payload, context) -> ClientManaCache.set(payload.mana(), payload.max()));
    }
}
