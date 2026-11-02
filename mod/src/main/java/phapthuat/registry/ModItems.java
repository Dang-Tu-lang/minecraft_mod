package phapthuat.registry;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import phapthuat.PhapThuat;
import phapthuat.item.GrimoireItem;
import phapthuat.item.MeterItem;
import phapthuat.item.WandItem;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PhapThuat.MODID);

    public static final DeferredItem<GrimoireItem> GRIMOIRE =
            ITEMS.register("grimoire", () -> new GrimoireItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<WandItem> WAND =
            ITEMS.register("wand", () -> new WandItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<MeterItem> METER =
            ITEMS.register("meter", () -> new MeterItem(new Item.Properties().stacksTo(1)));

    private ModItems() {}
}
