package phapthuat.client;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import phapthuat.item.WandItem;
import phapthuat.network.SelectSlotPayload;

/** Shift + lăn chuột khi cầm Pháp Trượng ở tay chính: đổi ô phép thay vì đổi ô thanh vật phẩm. */
public final class WandInput {
    private WandInput() {}

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || event.getScrollDeltaY() == 0) return;
        if (!(mc.player.getMainHandItem().getItem() instanceof WandItem) || !mc.options.keyShift.isDown()) return;
        // Lăn lên lùi về ô trước, giống thanh vật phẩm vanilla.
        PacketDistributor.sendToServer(new SelectSlotPayload(event.getScrollDeltaY() > 0 ? -1 : 1));
        event.setCanceled(true);
    }
}
