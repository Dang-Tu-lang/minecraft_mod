package phapthuat.mana;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import phapthuat.Config;
import phapthuat.PhapThuat;

public final class ManaAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, PhapThuat.MODID);

    /** Mana hiện tại. Không sao chép khi chết, nên người chơi hồi sinh với mana đầy (giá trị mặc định). */
    public static final Supplier<AttachmentType<Double>> MANA = ATTACHMENT_TYPES.register("mana",
            () -> AttachmentType.builder(() -> Config.MAX_MANA.get()).serialize(Codec.DOUBLE).build());

    private ManaAttachments() {}
}
