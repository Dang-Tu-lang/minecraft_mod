package phapthuat.physics;

import java.util.ArrayList;
import java.util.List;

/** Hình học của các thao tác theo lưới khối (spec mục 6.5). Khoảng cách tính giữa tâm các ô. */
public final class ShapeMath {
    public record Offset(int dx, int dy, int dz) {}

    public record Column(int x, int z) {}

    private ShapeMath() {}

    /** Mọi ô có dx² + dy² + dz² ≤ r². Bán kính lẻ (ví dụ 2,5) dùng đúng giá trị đó, không làm tròn. */
    public static List<Offset> sphere(double r) {
        List<Offset> out = new ArrayList<>();
        int n = (int) Math.floor(r);
        double r2 = r * r + 1e-9;
        for (int x = -n; x <= n; x++)
            for (int y = -n; y <= n; y++)
                for (int z = -n; z <= n; z++)
                    if (x * x + y * y + z * z <= r2) out.add(new Offset(x, y, z));
        return out;
    }

    /**
     * Mặt phẳng đứng vuông góc với hướng nhìn nằm ngang. Nhìn dọc trục X thì bề rộng nằm trên trục Z, và ngược lại.
     * Căn giữa theo cả hai chiều; khi kích thước chẵn thì lệch về phía dương.
     */
    public static List<Offset> wall(int width, int height, boolean facingAlongX) {
        List<Offset> out = new ArrayList<>();
        for (int w = -(width - 1) / 2; w <= width / 2; w++)
            for (int h = -(height - 1) / 2; h <= height / 2; h++)
                out.add(facingAlongX ? new Offset(0, h, w) : new Offset(w, h, 0));
        return out;
    }

    /** Các cột trên mặt đất từ (x0, z0) tới (x1, z1) theo thuật toán Bresenham, gồm cả hai đầu. */
    public static List<Column> groundLine(int x0, int z0, int x1, int z1) {
        List<Column> out = new ArrayList<>();
        int dx = Math.abs(x1 - x0), dz = -Math.abs(z1 - z0);
        int sx = x0 < x1 ? 1 : -1, sz = z0 < z1 ? 1 : -1;
        int err = dx + dz;
        int x = x0, z = z0;
        while (true) {
            out.add(new Column(x, z));
            if (x == x1 && z == z1) return out;
            int e2 = 2 * err;
            if (e2 >= dz) {
                err += dz;
                x += sx;
            }
            if (e2 <= dx) {
                err += dx;
                z += sz;
            }
        }
    }
}
