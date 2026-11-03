package phapthuat.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import phapthuat.item.WandItem;
import phapthuat.network.InscribePayload;
import phapthuat.physics.Element;
import phapthuat.spell.Fmt;
import phapthuat.spell.InscribedSpell;
import phapthuat.spell.ParamParser;
import phapthuat.spell.Shape;
import phapthuat.spell.Spell;
import phapthuat.spell.SpellParam;
import phapthuat.spell.SpellRegistry;
import phapthuat.spell.WandSlots;

/**
 * Sách Công Thức: cột trái là mục lục (Mở đầu, Hằng số và vật liệu, các phép theo nguyên tố), bên phải là trang cuộn
 * được. Khi tay phụ cầm Pháp Trượng, trang phép có thêm khu khắc phép. Không bao giờ hiện con số dự đoán nào.
 */
public final class GrimoireScreen extends Screen {
    /** Khóa của hai trang chung; bắt đầu bằng '#' nên không trùng id phép. */
    private static final String INTRO = "#intro";
    private static final String DATA = "#data";
    private static final int MARGIN = 8;
    private static final int LIST_WIDTH = 120;
    private static final int ENTRY_HEIGHT = 14;
    private static final int LINE_HEIGHT = 10;
    private static final int PARAM_WIDTH = 120;
    private static final int SHAPE_WIDTH = 48;
    private static final int TEXT_COLOR = 0xFFE6E6E6;
    private static final int LABEL_COLOR = 0xFFB8B8B8;
    private static final int TITLE_COLOR = 0xFFFFD27F;
    private static final int ERROR_COLOR = 0xFFFF5555;
    private static final int OK_COLOR = 0xFF7FFF7F;
    private static final int PANEL_COLOR = 0xC0101018;

    // Trạng thái giữ qua mỗi lần dựng lại widget.
    private String selected = INTRO;
    private int scroll;
    @Nullable
    private Shape shape;
    private int slot;
    private final Map<String, String> typed = new HashMap<>();
    private final Map<String, Integer> choices = new HashMap<>();
    @Nullable
    private Component status;

    // Dựng lại trong init().
    private record Label(Component text, int x, int y, int color) {}

    private final List<Label> labels = new ArrayList<>();
    private final Map<SpellParam, EditBox> inputs = new LinkedHashMap<>();
    private final List<SpellParam> choiceParams = new ArrayList<>();
    private List<FormattedCharSequence> lines = List.of();
    private int pageX;
    private int pageY;
    private int pageRight;
    private int pageBottom;
    @Nullable
    private Button inscribeButton;

    public GrimoireScreen() {
        super(Component.translatable("item.phapthuat.grimoire"));
        WandSlots slots = offhandWand();
        if (slots != null) slot = slots.selected();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        labels.clear();
        inputs.clear();
        choiceParams.clear();
        inscribeButton = null;

        int y = MARGIN;
        y = entry(INTRO, Component.translatable("phapthuat.book.intro.title"), y);
        y = entry(DATA, Component.translatable("phapthuat.book.data.title"), y);
        List<Spell> spells = new ArrayList<>(SpellRegistry.all());
        spells.sort(Comparator.comparing(Spell::element));
        Element group = null;
        for (Spell s : spells) {
            if (s.element() != group) {
                group = s.element();
                y += 3;
                labels.add(new Label(Component.translatable(group.langKey()), MARGIN, y, LABEL_COLOR));
                y += LINE_HEIGHT;
            }
            y = entry(s.id(), s.displayName(), y);
        }

        pageX = MARGIN * 2 + LIST_WIDTH;
        pageY = MARGIN;
        pageRight = width - MARGIN;
        pageBottom = height - MARGIN;
        Spell spell = SpellRegistry.get(selected).orElse(null);
        if (spell != null && offhandWand() != null) pageBottom = buildInscribe(spell);

        List<Component> content = spell != null ? GrimoireText.spell(spell)
                : selected.equals(DATA) ? GrimoireText.data() : GrimoireText.intro();
        List<FormattedCharSequence> wrapped = new ArrayList<>();
        for (Component c : content) wrapped.addAll(font.split(c, pageRight - pageX - 6));
        lines = wrapped;
        scroll = Mth.clamp(scroll, 0, maxScroll());
    }

    private int entry(String key, Component title, int y) {
        Button b = addRenderableWidget(Button.builder(title, btn -> select(key))
                .bounds(MARGIN, y, LIST_WIDTH, ENTRY_HEIGHT).build());
        b.active = !key.equals(selected);
        return y + ENTRY_HEIGHT + 2;
    }

    private void select(String key) {
        selected = key;
        scroll = 0;
        shape = null;
        status = null;
        rebuildWidgets();
    }

    /** Dựng khu khắc phép ở đáy trang; trả về đáy mới của vùng chữ. */
    private int buildInscribe(Spell spell) {
        List<Shape> shapes = spell.shapes().stream().sorted().toList();
        if (shape == null || !shapes.contains(shape)) shape = shapes.getFirst();
        List<SpellParam> params = spell.allParams(shape);

        int pageWidth = pageRight - pageX;
        int shapeCols = Math.max(1, pageWidth / (SHAPE_WIDTH + 2));
        int shapeRows = (shapes.size() + shapeCols - 1) / shapeCols;
        int paramCols = Math.max(1, pageWidth / (PARAM_WIDTH + 6));
        int paramRows = (params.size() + paramCols - 1) / paramCols;
        int top = height - MARGIN - (12 + shapeRows * 20 + 2 + paramRows * 32 + 20 + 12);

        int y = top;
        labels.add(new Label(Component.translatable("phapthuat.book.inscribe.title"), pageX, y, TITLE_COLOR));
        y += 12;
        for (int i = 0; i < shapes.size(); i++) {
            Shape s = shapes.get(i);
            Button b = addRenderableWidget(Button.builder(Component.translatable(s.langKey()), btn -> {
                shape = s;
                rebuildWidgets();
            }).bounds(pageX + (i % shapeCols) * (SHAPE_WIDTH + 2), y + (i / shapeCols) * 20, SHAPE_WIDTH, 18).build());
            b.active = s != shape;
        }
        y += shapeRows * 20 + 2;

        for (int i = 0; i < params.size(); i++) {
            SpellParam p = params.get(i);
            int px = pageX + (i % paramCols) * (PARAM_WIDTH + 6);
            int py = y + (i / paramCols) * 32;
            Component label = GrimoireText.paramLabel(p);
            labels.add(new Label(label, px, py, LABEL_COLOR));
            if (p.isChoice()) {
                // Thông số lựa chọn: một dãy nút, nút đang chọn bị khóa.
                int n = p.choiceLangKeys().size();
                int bw = (PARAM_WIDTH - 2 * (n - 1)) / n;
                int chosen = chosenIndex(p);
                for (int c = 0; c < n; c++) {
                    int index = c;
                    Button b = addRenderableWidget(Button.builder(Component.translatable(p.choiceLangKeys().get(c)),
                            btn -> {
                                choices.put(p.key(), index);
                                rebuildWidgets();
                            }).bounds(px + c * (bw + 2), py + 11, bw, 16).build());
                    b.active = c != chosen;
                }
                choiceParams.add(p);
                continue;
            }
            EditBox box = new EditBox(font, px, py + 11, PARAM_WIDTH, 16, label);
            box.setMaxLength(16);
            box.setValue(typed.getOrDefault(p.key(), plain(p.defaultValue())));
            box.setResponder(text -> {
                typed.put(p.key(), text);
                refreshValidity();
            });
            inputs.put(p, addRenderableWidget(box));
        }
        y += paramRows * 32;

        Component slotLabel = Component.translatable("phapthuat.book.inscribe.slot");
        labels.add(new Label(slotLabel, pageX, y + 5, LABEL_COLOR));
        int sx = pageX + font.width(slotLabel) + 4;
        for (int i = 0; i < WandSlots.SIZE; i++) {
            int index = i;
            Button b = addRenderableWidget(Button.builder(Component.literal(String.valueOf(i + 1)), btn -> {
                slot = index;
                rebuildWidgets();
            }).bounds(sx + i * 22, y, 20, 18).build());
            b.active = i != slot;
        }
        inscribeButton = addRenderableWidget(Button.builder(Component.translatable("phapthuat.book.inscribe.button"),
                btn -> inscribe(spell)).bounds(sx + WandSlots.SIZE * 22 + 6, y, 80, 18).build());
        refreshValidity();
        return top - 6;
    }

    /** Ô nhập hợp lệ khi đọc được số và số nằm trong [min, max]; sai thì chữ đỏ, viền đỏ và nút khắc bị khóa. */
    private static boolean valid(SpellParam p, String text) {
        OptionalDouble v = ParamParser.parse(text);
        return v.isPresent() && v.getAsDouble() >= p.min() && v.getAsDouble() <= p.max();
    }

    private void refreshValidity() {
        boolean all = true;
        for (Map.Entry<SpellParam, EditBox> e : inputs.entrySet()) {
            boolean ok = valid(e.getKey(), e.getValue().getValue());
            e.getValue().setTextColor(ok ? TEXT_COLOR : ERROR_COLOR);
            all &= ok;
        }
        if (inscribeButton != null) inscribeButton.active = all;
    }

    private int chosenIndex(SpellParam p) {
        return choices.getOrDefault(p.key(), (int) p.defaultValue());
    }

    private void inscribe(Spell spell) {
        Map<String, Double> values = new HashMap<>();
        for (SpellParam p : choiceParams) values.put(p.key(), (double) chosenIndex(p));
        for (Map.Entry<SpellParam, EditBox> e : inputs.entrySet()) {
            OptionalDouble v = ParamParser.parse(e.getValue().getValue());
            if (v.isEmpty()) return;
            values.put(e.getKey().key(), v.getAsDouble());
        }
        PacketDistributor.sendToServer(new InscribePayload(slot, new InscribedSpell(spell.id(), shape, values)));
        status = Component.translatable("phapthuat.inscribe.done", spell.displayName(), slot + 1);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.fill(pageX - 4, pageY - 4, pageRight + 4, pageBottom + 2, PANEL_COLOR);
        g.enableScissor(pageX, pageY, pageRight, pageBottom);
        for (int i = 0; i < visibleLines() && scroll + i < lines.size(); i++) {
            g.drawString(font, lines.get(scroll + i), pageX, pageY + i * LINE_HEIGHT, TEXT_COLOR, false);
        }
        g.disableScissor();
        if (maxScroll() > 0) {
            int track = pageBottom - pageY;
            int thumb = Math.max(10, track * visibleLines() / lines.size());
            int thumbY = pageY + (track - thumb) * scroll / maxScroll();
            g.fill(pageRight + 1, thumbY, pageRight + 3, thumbY + thumb, 0xFF808080);
        }
        for (Label l : labels) g.drawString(font, l.text(), l.x(), l.y(), l.color(), false);
        for (Map.Entry<SpellParam, EditBox> e : inputs.entrySet()) {
            EditBox box = e.getValue();
            if (!valid(e.getKey(), box.getValue())) {
                g.renderOutline(box.getX() - 2, box.getY() - 2, box.getWidth() + 4, box.getHeight() + 4, ERROR_COLOR);
            }
        }
        if (status != null && inscribeButton != null) {
            g.drawString(font, status.copy().withStyle(ChatFormatting.GREEN), pageX, inscribeButton.getY() + 21,
                    OK_COLOR, false);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= pageX && mouseX <= pageRight && mouseY >= pageY && mouseY <= pageBottom && scrollY != 0) {
            scroll = Mth.clamp(scroll - (int) Math.signum(scrollY) * 3, 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private int visibleLines() {
        return Math.max(1, (pageBottom - pageY) / LINE_HEIGHT);
    }

    private int maxScroll() {
        return Math.max(0, lines.size() - visibleLines());
    }

    @Nullable
    private static WandSlots offhandWand() {
        var player = Minecraft.getInstance().player;
        if (player == null) return null;
        ItemStack offhand = player.getOffhandItem();
        return offhand.getItem() instanceof WandItem ? WandItem.slots(offhand) : null;
    }

    /** Giá trị mặc định cho ô nhập: số nguyên viết gọn, số lẻ dùng dấu phẩy kiểu Việt. */
    private static String plain(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : Fmt.num(v, 2);
    }
}
