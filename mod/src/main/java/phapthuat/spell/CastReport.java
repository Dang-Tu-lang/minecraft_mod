package phapthuat.spell;

import net.minecraft.network.chat.Component;

/** Kết quả một lần thi triển: dòng báo có số liệu, hiện sau khi thi triển (spec mục 6.7). */
public record CastReport(boolean success, Component message) {}
