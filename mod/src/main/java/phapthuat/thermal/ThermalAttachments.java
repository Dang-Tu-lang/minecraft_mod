package phapthuat.thermal;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import phapthuat.PhapThuat;

public final class ThermalAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, PhapThuat.MODID);

    public static final Codec<EntityThermal> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.fieldOf("core").forGetter(EntityThermal::coreTempC),
            Codec.DOUBLE.fieldOf("dose").forGetter(EntityThermal::doseGy),
            Codec.LONG.fieldOf("t").forGetter(EntityThermal::updatedAtTick)).apply(i, EntityThermal::new));

    /** Lõi nhiệt và liều phóng xạ của sinh vật. Mặc định NaN = đang ở nhiệt độ tự nhiên. */
    public static final Supplier<AttachmentType<EntityThermal>> ENTITY_THERMAL = ATTACHMENT_TYPES.register(
            "entity_thermal",
            () -> AttachmentType.builder(() -> new EntityThermal(Double.NaN, 0, 0)).serialize(CODEC).build());

    private ThermalAttachments() {}
}
