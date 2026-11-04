package phapthuat.spell.impl;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Bảng nhiên liệu của Đốt Nhiên Liệu (spec 8.7): chỉ các nhiên liệu đốt cháy được kể trên, trừ xô dung nham. Java
 * thuần (chỉ dùng tag và id của Minecraft, không đụng thế giới).
 */
public final class FuelChoices {
    /** Thứ tự lựa chọn khớp với khóa dịch {@code phapthuat.fuel.<id>}. */
    public enum Kind {
        COAL("coal"), CHARCOAL("charcoal"), COAL_BLOCK("coal_block"), PLANKS("planks"), LOGS("logs"), STICK("stick"),
        BLAZE_ROD("blaze_rod");

        private final String id;

        Kind(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    private FuelChoices() {}

    public static List<Kind> all() {
        return List.of(Kind.values());
    }

    public static Kind byIndex(int index) {
        Kind[] all = Kind.values();
        return all[Math.max(0, Math.min(all.length - 1, index))];
    }

    public static int indexOf(Kind kind) {
        return kind.ordinal();
    }

    public static String langKey(int index) {
        return "phapthuat.fuel." + byIndex(index).id();
    }

    public static boolean matches(int index, ItemStack stack) {
        if (stack.isEmpty()) return false;
        Kind kind = byIndex(index);
        return switch (kind) {
            case COAL -> stack.is(Items.COAL);
            case CHARCOAL -> stack.is(Items.CHARCOAL);
            case COAL_BLOCK -> stack.is(Items.COAL_BLOCK);
            case PLANKS -> stack.is(ItemTags.PLANKS);
            case LOGS -> stack.is(ItemTags.LOGS_THAT_BURN);
            case STICK -> stack.is(Items.STICK);
            case BLAZE_ROD -> stack.is(Items.BLAZE_ROD);
        };
    }

    /** Thời gian cháy trong lò của vanilla, tick; 0 nếu vật phẩm không cháy được. */
    public static int burnTicks(ItemStack stack) {
        return stack.getBurnTime(RecipeType.SMELTING);
    }

    public static String itemId(ItemStack stack) {
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key == null ? "minecraft:air" : key.toString();
    }
}
