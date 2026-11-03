package phapthuat.network;

import java.util.HashMap;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import phapthuat.PhapThuat;
import phapthuat.spell.InscribedSpell;
import phapthuat.spell.SpellCodecs;

/** Client → server: khắc phép vào ô {@code slot} (0..4) của Pháp Trượng ở tay phụ. Server kiểm tra lại mọi thứ. */
public record InscribePayload(int slot, InscribedSpell spell) implements CustomPacketPayload {
    public static final Type<InscribePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PhapThuat.MODID, "inscribe"));
    /** Giới hạn kích thước để gói tin giả mạo không đẩy được dữ liệu lớn lên server. */
    private static final int MAX_ID_LENGTH = 64;
    private static final int MAX_PARAMS = 16;
    private static final StreamCodec<ByteBuf, InscribedSpell> SPELL = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_ID_LENGTH), InscribedSpell::spellId,
            SpellCodecs.SHAPE_STREAM, InscribedSpell::shape,
            ByteBufCodecs.<ByteBuf, String, Double, Map<String, Double>>map(HashMap::new,
                    ByteBufCodecs.stringUtf8(MAX_ID_LENGTH), ByteBufCodecs.DOUBLE, MAX_PARAMS), InscribedSpell::params,
            InscribedSpell::new);
    public static final StreamCodec<ByteBuf, InscribePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, InscribePayload::slot,
            SPELL, InscribePayload::spell,
            InscribePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
