package phapthuat.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import phapthuat.Config;
import phapthuat.entity.SpellProjectile;
import phapthuat.physics.Element;
import phapthuat.physics.EntityMass;
import phapthuat.physics.Material;
import phapthuat.physics.Materials;
import phapthuat.physics.PhysicsConstants;
import phapthuat.physics.ShapeMath;
import phapthuat.spell.Fmt;
import phapthuat.spell.Shape;
import phapthuat.spell.Spell;
import phapthuat.spell.SpellParam;

/**
 * Nội dung các trang Sách Công Thức. Mọi con số đọc từ chính chỗ code dùng ({@link PhysicsConstants},
 * {@link Materials}, {@link EntityMass}, {@link Config}), chữ lấy từ file ngôn ngữ. Không in đáp số ví dụ nào.
 */
final class GrimoireText {
    private static final Locale VI = Locale.forLanguageTag("vi-VN");

    private GrimoireText() {}

    static List<Component> intro() {
        String elements = Arrays.stream(Element.values()).map(e -> I18n.get(e.langKey()))
                .collect(Collectors.joining(", "));
        StringBuilder spheres = new StringBuilder();
        for (int r = 1; r <= 5; r++) {
            if (r > 1) spheres.append(", ");
            spheres.append("r = ").append(r).append(" → ").append(ShapeMath.sphere(r).size());
        }
        List<Component> out = paragraphs(I18n.get("phapthuat.book.intro.body", grouped(Config.joulesPerMana()),
                Fmt.num(Config.MAX_MANA.get(), 0), Fmt.num(Config.REGEN_PER_SECOND.get(), 1), elements));
        out.add(heading("phapthuat.book.shapes.title"));
        for (Shape s : Shape.values()) {
            out.add(Component.literal("• ").append(Component.translatable(s.langKey()).withStyle(ChatFormatting.AQUA))
                    .append(": ").append(Component.translatable("phapthuat.book.shape." + s.serializedName())));
        }
        out.addAll(paragraphs(I18n.get("phapthuat.book.intro.rules", spheres, Fmt.num(SpellProjectile.MASS_KG, 1),
                Fmt.num(Config.HALF_LIFE_SECONDS.get(), 0), Fmt.num(Config.HOT_SOURCE_HALF_LIFE_SECONDS.get(), 0),
                Fmt.num(Config.DOSE_HALF_LIFE_SECONDS.get(), 0), Fmt.num(PhysicsConstants.KELVIN_OFFSET, 2))));
        return out;
    }

    static List<Component> data() {
        List<Component> out = new ArrayList<>();
        out.add(heading("phapthuat.book.data.constants"));
        out.add(line("phapthuat.book.const.g", Fmt.num(PhysicsConstants.G, 2)));
        out.add(line("phapthuat.book.const.r", Fmt.num(PhysicsConstants.R, 3)));
        out.add(line("phapthuat.book.const.p0", grouped(PhysicsConstants.P0)));
        out.add(line("phapthuat.book.const.air", Fmt.num(PhysicsConstants.AIR_DENSITY, 1),
                Fmt.num(PhysicsConstants.AIR_CV, 0)));
        out.add(line("phapthuat.book.const.water_mol", Fmt.num(PhysicsConstants.WATER_MOL_PER_KG, 2)));
        out.add(line("phapthuat.book.const.steam_cp", Fmt.num(PhysicsConstants.STEAM_CP, 0)));
        out.add(line("phapthuat.book.const.glass_pa", Fmt.num(PhysicsConstants.GLASS_BREAK_PA / 1000, 0)));
        out.add(line("phapthuat.book.const.blast_pa", Fmt.num(PhysicsConstants.BLAST_DAMAGE_PA / 1000, 0)));
        out.add(line("phapthuat.book.const.hot_air", Fmt.num(PhysicsConstants.HOT_AIR_IGNITE_C, 0)));
        out.add(line("phapthuat.book.const.tnt", grouped(PhysicsConstants.TNT_REFERENCE_WORK_J / 1000),
                Fmt.num(PhysicsConstants.MAX_EXPLOSION_POWER, 0)));
        out.add(line("phapthuat.book.const.core", Fmt.num(PhysicsConstants.CORE_VOLUME_M3, 3)));

        out.add(heading("phapthuat.book.data.materials"));
        for (Material m : Materials.all()) out.add(material(m));

        out.add(heading("phapthuat.book.data.entities"));
        EntityMass.table().entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEach(e -> out.add(Component.literal("• ").append(entityName(e.getKey()))
                        .append(": " + Fmt.num(e.getValue(), 0) + " kg")));
        out.add(line("phapthuat.book.data.entity_fallback", Fmt.num(EntityMass.FALLBACK_KG_PER_M3, 0)));
        return out;
    }

    static List<Component> spell(Spell spell) {
        List<Component> out = new ArrayList<>();
        out.add(Component.empty().append(spell.displayName()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        out.add(line("phapthuat.book.spell.element", Component.translatable(spell.element().langKey())));
        String shapes = spell.shapes().stream().sorted().map(s -> I18n.get(s.langKey()))
                .collect(Collectors.joining(", "));
        out.add(line("phapthuat.book.spell.shapes", shapes));
        for (SpellParam p : spell.params()) out.add(paramLine(p));
        out.addAll(paragraphs(I18n.get("phapthuat.book.spell." + spell.id())));
        return out;
    }

    /** "Năng lượng (mana): 0–1000" — nhãn của một thông số, dùng cả trong trang phép lẫn ô nhập. */
    static Component paramLabel(SpellParam p) {
        if (p.isChoice()) {
            String options = p.choiceLangKeys().stream().map(I18n::get).collect(Collectors.joining(" / "));
            return Component.translatable("phapthuat.book.param_choice", Component.translatable(p.langKey()), options);
        }
        return Component.translatable("phapthuat.book.param", Component.translatable(p.langKey()),
                Component.translatable(p.unitLangKey()), Fmt.num(p.min(), 0), Fmt.num(p.max(), 0));
    }

    private static Component paramLine(SpellParam p) {
        return Component.literal("• ").append(paramLabel(p));
    }

    private static Component material(Material m) {
        MutableComponent c = Component.literal("• ")
                .append(Component.translatable("phapthuat.material." + m.id()).withStyle(ChatFormatting.AQUA));
        if (m.estimated()) c.append(Component.translatable("phapthuat.book.mat.estimated"));
        c.append(": ");
        List<Component> parts = new ArrayList<>();
        if (!Double.isNaN(m.density())) {
            parts.add(Component.translatable("phapthuat.book.mat.thermal", grouped(m.density()), grouped(m.specificHeat())));
        }
        if (m.flammable()) parts.add(Component.translatable("phapthuat.book.mat.ignite", Fmt.tempC(m.ignitionC())));
        if (m.meltable()) {
            parts.add(Component.translatable("phapthuat.book.mat.melt", Fmt.tempC(m.meltC()), grouped(m.latentFusion())));
        }
        if (m.boils()) {
            parts.add(Component.translatable("phapthuat.book.mat.boil", Fmt.tempC(m.boilC()),
                    grouped(m.liquidSpecificHeat()), grouped(m.latentVaporization())));
        }
        if (m.brittle()) {
            parts.add(Component.translatable("phapthuat.book.mat.brittle", Fmt.num(m.youngsModulus() / 1e9, 0),
                    Fmt.num(m.thermalExpansion() * 1e6, 0), Fmt.num(m.tensileStrength() / 1e6, 0)));
        }
        if (!Double.isNaN(m.naturalTempC())) parts.add(Component.translatable("phapthuat.book.mat.hot", Fmt.tempC(m.naturalTempC())));
        if (!Double.isNaN(m.maxExtractableJ())) {
            parts.add(Component.translatable("phapthuat.book.mat.max", grouped(m.maxExtractableJ())));
        }
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) c.append(" · ");
            c.append(parts.get(i));
        }
        return c;
    }

    private static Component entityName(String id) {
        int colon = id.indexOf(':');
        return Component.translatable("entity." + id.substring(0, colon) + "." + id.substring(colon + 1));
    }

    private static Component heading(String key) {
        return Component.translatable(key).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
    }

    private static Component line(String key, Object... args) {
        return Component.translatable(key, args);
    }

    /** Tách văn bản nhiều dòng; dòng bắt đầu bằng "# " là tiêu đề. */
    private static List<Component> paragraphs(String text) {
        List<Component> out = new ArrayList<>();
        for (String raw : text.split("\n")) {
            if (raw.startsWith("# ")) {
                out.add(Component.literal(raw.substring(2)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            } else {
                out.add(Component.literal(raw));
            }
        }
        return out;
    }

    /** Số có dấu chấm ngăn hàng nghìn kiểu Việt (2.257.000); số lẻ như 1,2 giữ một chữ số thập phân. */
    private static String grouped(double v) {
        if (Math.abs(v - Math.rint(v)) > 1e-9) return Fmt.num(v, 1);
        return String.format(VI, "%,.0f", v);
    }
}
