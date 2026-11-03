package phapthuat.item;

import java.util.Optional;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import phapthuat.registry.ModDataComponents;
import phapthuat.spell.InscribedSpell;
import phapthuat.spell.SpellCaster;
import phapthuat.spell.WandSlots;

/** Pháp Trượng: 5 ô phép; chuột phải thi triển ô đang chọn, Shift + lăn chuột để đổi ô. */
public class WandItem extends Item {
    public static final int COOLDOWN_TICKS = 10;

    public WandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.success(stack);
        WandSlots slots = slots(stack);
        Optional<InscribedSpell> spell = slots.current();
        if (spell.isEmpty()) {
            serverPlayer.displayClientMessage(
                    Component.translatable("phapthuat.wand.empty_slot", slots.selected() + 1), true);
        } else {
            SpellCaster.cast(serverPlayer, spell.get());
        }
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.success(stack);
    }

    public static WandSlots slots(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.WAND_SLOTS.get(), WandSlots.EMPTY);
    }

    /** Đổi ô đang chọn của trượng ở tay chính. Gói tin từ client chỉ được đổi đúng 1 ô mỗi lần. */
    public static void cycleSelected(ServerPlayer player, int delta) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof WandItem) || delta == 0) return;
        stack.set(ModDataComponents.WAND_SLOTS.get(), slots(stack).cycle(Integer.signum(delta)));
    }
}
