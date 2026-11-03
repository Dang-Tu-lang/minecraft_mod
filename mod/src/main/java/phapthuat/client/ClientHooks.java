package phapthuat.client;

import net.minecraft.client.Minecraft;

/** Lời gọi từ code chung sang code chỉ có ở client. Lớp này chỉ được nạp khi đang chạy phía client. */
public final class ClientHooks {
    private ClientHooks() {}

    public static void openGrimoire() {
        Minecraft.getInstance().setScreen(new GrimoireScreen());
    }
}
