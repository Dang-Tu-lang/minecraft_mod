package phapthuat.registry;

import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import phapthuat.PhapThuat;

/** Data component của vật phẩm. Ô phép của Pháp Trượng được đăng ký ở đây cùng kiểu WandSlots (Task 9). */
public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, PhapThuat.MODID);

    private ModDataComponents() {}
}
