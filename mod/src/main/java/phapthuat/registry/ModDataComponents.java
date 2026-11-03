package phapthuat.registry;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import phapthuat.PhapThuat;
import phapthuat.spell.SpellCodecs;
import phapthuat.spell.WandSlots;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, PhapThuat.MODID);

    /** 5 ô phép của Pháp Trượng và ô đang chọn. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WandSlots>> WAND_SLOTS =
            COMPONENTS.registerComponentType("wand_slots",
                    b -> b.persistent(SpellCodecs.WAND_SLOTS).networkSynchronized(SpellCodecs.WAND_SLOTS_STREAM));

    private ModDataComponents() {}
}
