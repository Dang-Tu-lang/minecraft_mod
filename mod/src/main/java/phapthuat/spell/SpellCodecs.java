package phapthuat.spell;

import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Codec lưu/đồng bộ cho Shape, InscribedSpell, WandSlots. Tách riêng để các kiểu đó là Java thuần và chạy được
 * trong unit test (thư viện của Minecraft không có trên classpath test).
 */
public final class SpellCodecs {
    public static final Codec<Shape> SHAPE = Codec.STRING.comapFlatMap(SpellCodecs::parseShape, Shape::serializedName);
    public static final StreamCodec<ByteBuf, Shape> SHAPE_STREAM = ByteBufCodecs.VAR_INT.map(
            i -> i >= 0 && i < Shape.values().length ? Shape.values()[i] : Shape.DIEM, Shape::ordinal);

    public static final Codec<InscribedSpell> INSCRIBED = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("spell").forGetter(InscribedSpell::spellId),
            SHAPE.fieldOf("shape").forGetter(InscribedSpell::shape),
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).fieldOf("params").forGetter(InscribedSpell::params))
            .apply(i, InscribedSpell::new));
    public static final StreamCodec<ByteBuf, InscribedSpell> INSCRIBED_STREAM = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, InscribedSpell::spellId,
            SHAPE_STREAM, InscribedSpell::shape,
            ByteBufCodecs.<ByteBuf, String, Double, Map<String, Double>>map(HashMap::new, ByteBufCodecs.STRING_UTF8,
                    ByteBufCodecs.DOUBLE), InscribedSpell::params,
            InscribedSpell::new);

    public static final Codec<WandSlots> WAND_SLOTS = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, INSCRIBED).fieldOf("slots").forGetter(WandSlots::asMap),
            Codec.INT.fieldOf("selected").forGetter(WandSlots::selected)).apply(i, WandSlots::fromMap));
    public static final StreamCodec<ByteBuf, WandSlots> WAND_SLOTS_STREAM = StreamCodec.composite(
            ByteBufCodecs.optional(INSCRIBED_STREAM).apply(ByteBufCodecs.list(WandSlots.SIZE)), WandSlots::slots,
            ByteBufCodecs.VAR_INT, WandSlots::selected,
            WandSlots::new);

    private SpellCodecs() {}

    private static DataResult<Shape> parseShape(String name) {
        for (Shape s : Shape.values()) {
            if (s.serializedName().equals(name)) return DataResult.success(s);
        }
        return DataResult.error(() -> "Thao tác lạ: " + name);
    }
}
