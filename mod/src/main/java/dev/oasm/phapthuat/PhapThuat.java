package dev.oasm.phapthuat;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(PhapThuat.MODID)
public class PhapThuat {
    public static final String MODID = "phapthuat";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PhapThuat(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
