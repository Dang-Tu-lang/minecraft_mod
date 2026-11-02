package phapthuat.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import phapthuat.PhapThuat;

/** Server → client: mana hiện tại và mana tối đa của người chơi. */
public record ManaSyncPayload(double mana, double max) implements CustomPacketPayload {
    public static final Type<ManaSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PhapThuat.MODID, "mana_sync"));
    public static final StreamCodec<ByteBuf, ManaSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, ManaSyncPayload::mana,
            ByteBufCodecs.DOUBLE, ManaSyncPayload::max,
            ManaSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
