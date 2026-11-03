package phapthuat;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import phapthuat.client.ManaHudLayer;
import phapthuat.client.WandHudLayer;
import phapthuat.client.WandInput;
import phapthuat.registry.ModEntities;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

// Lớp này không được nạp trên server riêng, nên dùng code client ở đây là an toàn.
@Mod(value = PhapThuat.MODID, dist = Dist.CLIENT)
public class PhapThuatClient {
    public PhapThuatClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener(PhapThuatClient::registerGuiLayers);
        modBus.addListener(PhapThuatClient::registerRenderers);
        NeoForge.EVENT_BUS.register(WandInput.class);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Đạn phép chỉ hiện bằng hạt lửa, không có mô hình.
        event.registerEntityRenderer(ModEntities.SPELL_PROJECTILE.get(), NoopRenderer::new);
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.PLAYER_HEALTH,
                ResourceLocation.fromNamespaceAndPath(PhapThuat.MODID, "mana"), new ManaHudLayer());
        event.registerAbove(VanillaGuiLayers.SELECTED_ITEM_NAME,
                ResourceLocation.fromNamespaceAndPath(PhapThuat.MODID, "wand"), new WandHudLayer());
    }
}
