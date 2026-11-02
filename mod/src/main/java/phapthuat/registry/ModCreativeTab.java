package phapthuat.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import phapthuat.PhapThuat;

public final class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PhapThuat.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.phapthuat"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.WAND.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(ModItems.GRIMOIRE.get());
                        output.accept(ModItems.WAND.get());
                        output.accept(ModItems.METER.get());
                    })
                    .build());

    private ModCreativeTab() {}
}
