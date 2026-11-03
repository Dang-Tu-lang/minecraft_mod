package phapthuat.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import phapthuat.PhapThuat;

/** Client → server: đổi ô đang chọn của Pháp Trượng ở tay chính (+1 hoặc −1). */
public record SelectSlotPayload(int delta) implements CustomPacketPayload {
    public static final Type<SelectSlotPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PhapThuat.MODID, "select_slot"));
    public static final StreamCodec<ByteBuf, SelectSlotPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SelectSlotPayload::delta,
            SelectSlotPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
