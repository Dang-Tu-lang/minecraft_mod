package phapthuat.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import phapthuat.client.ClientHooks;

/** Sách Công Thức: chuột phải để mở giao diện đọc và khắc phép. */
public class GrimoireItem extends Item {
    public GrimoireItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Nhánh này chỉ chạy ở client, nên lớp ClientHooks không bao giờ bị nạp trên server riêng.
        if (level.isClientSide()) ClientHooks.openGrimoire();
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
}
