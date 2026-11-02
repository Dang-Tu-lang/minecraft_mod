import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

/**
 * Writes the GameTest template data/phapthuat/structure/empty_7x7x7.nbt: a 7x7x7 box of air on a stone floor.
 * NeoForge 21.1.265 has no built-in empty template, so the file is generated here (vanilla structure NBT, gzip).
 * Run: tools\jdk-21\bin\java.exe mod\scripts\GenEmptyStructure.java mod\src\main\resources\data\phapthuat\structure\empty_7x7x7.nbt
 */
public class GenEmptyStructure {
    private static final int SIZE = 7;
    private static final int DATA_VERSION_1_21_1 = 3955;

    public static void main(String[] args) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(args[0])))) {
            out.writeByte(10);
            name(out, "");

            out.writeByte(3);
            name(out, "DataVersion");
            out.writeInt(DATA_VERSION_1_21_1);

            intList(out, "size", SIZE, SIZE, SIZE);

            out.writeByte(9);
            name(out, "palette");
            out.writeByte(10);
            out.writeInt(2);
            paletteEntry(out, "minecraft:stone");
            paletteEntry(out, "minecraft:air");

            out.writeByte(9);
            name(out, "blocks");
            out.writeByte(10);
            out.writeInt(SIZE * SIZE * SIZE);
            for (int x = 0; x < SIZE; x++)
                for (int y = 0; y < SIZE; y++)
                    for (int z = 0; z < SIZE; z++) {
                        intList(out, "pos", x, y, z);
                        out.writeByte(3);
                        name(out, "state");
                        out.writeInt(y == 0 ? 0 : 1);
                        out.writeByte(0);
                    }

            out.writeByte(9);
            name(out, "entities");
            out.writeByte(0);
            out.writeInt(0);

            out.writeByte(0);
        }
    }

    private static void paletteEntry(DataOutputStream out, String block) throws IOException {
        out.writeByte(8);
        name(out, "Name");
        name(out, block);
        out.writeByte(0);
    }

    private static void intList(DataOutputStream out, String key, int... values) throws IOException {
        out.writeByte(9);
        name(out, key);
        out.writeByte(3);
        out.writeInt(values.length);
        for (int v : values) out.writeInt(v);
    }

    private static void name(DataOutputStream out, String s) throws IOException {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        out.writeShort(b.length);
        out.write(b);
    }
}
