package phapthuat;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// Lớp này không được nạp trên server riêng, nên dùng code client ở đây là an toàn.
@Mod(value = PhapThuat.MODID, dist = Dist.CLIENT)
public class PhapThuatClient {
    public PhapThuatClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
