package phapthuat;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import phapthuat.mana.ManaAttachments;
import phapthuat.mana.ManaManager;
import phapthuat.network.ModNetwork;
import phapthuat.registry.ModCreativeTab;
import phapthuat.registry.ModDataComponents;
import phapthuat.registry.ModEntities;
import phapthuat.registry.ModItems;
import phapthuat.spell.SpellRegistry;
import phapthuat.spell.TimedEffects;
import phapthuat.spell.impl.HeatSpell;
import phapthuat.spell.impl.ImpulseSpell;
import phapthuat.spell.impl.SiphonSpell;
import phapthuat.thermal.ThermalAttachments;
import phapthuat.thermal.ThermalTicker;

@Mod(PhapThuat.MODID)
public class PhapThuat {
    public static final String MODID = "phapthuat";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PhapThuat(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        ModItems.ITEMS.register(modBus);
        ModDataComponents.COMPONENTS.register(modBus);
        ModCreativeTab.TABS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ManaAttachments.ATTACHMENT_TYPES.register(modBus);
        ThermalAttachments.ATTACHMENT_TYPES.register(modBus);
        modBus.addListener(ModNetwork::register);

        NeoForge.EVENT_BUS.register(ManaManager.class);
        NeoForge.EVENT_BUS.register(ThermalTicker.class);
        NeoForge.EVENT_BUS.register(TimedEffects.class);

        SpellRegistry.register(new HeatSpell());
        SpellRegistry.register(new SiphonSpell());
        SpellRegistry.register(new ImpulseSpell());
    }
}
