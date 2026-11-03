# Hướng dẫn: mod Pháp Thuật Vật Lý

Mọi thứ của mod nằm trong thư mục `D:\code_base\New folder`: mã nguồn ở `mod`, Java và Gradle ở `tools`. Không có gì cài ra ngoài thư mục này.

## Mở game có mod

Mở PowerShell rồi gõ:

```powershell
& "D:\code_base\New folder\mod\g.cmd" runClient
```

Lần đầu mất khoảng 1–2 phút. Phần build luôn chạy offline. Riêng game Minecraft khi mở có thể tự kết nối máy chủ Mojang (đăng nhập, skin) như bình thường.

## Chế tạo 3 vật phẩm

Cả ba nằm trong tab sáng tạo "Pháp Thuật Vật Lý". Ở chế độ sinh tồn thì chế tạo như sau:

- **Sách Công Thức:** 1 quyển sách + 1 than (than đá hoặc than gỗ), xếp chỗ nào cũng được.
- **Pháp Trượng:** 1 thỏi vàng ở góc trên bên phải, 2 que xếp chéo xuống góc dưới bên trái.
- **Máy Đo:** tấm kính ở giữa hàng trên; thỏi sắt, bụi đỏ, thỏi sắt ở hàng giữa; thỏi sắt ở giữa hàng dưới.

## Khắc phép và thi triển

1. Cầm **Pháp Trượng ở tay phụ** (phím F để đổi tay), **Sách** ở tay chính, chuột phải để mở sách.
2. Chọn một phép ở cột trái (ví dụ Gia Nhiệt). Phía dưới trang hiện khu khắc phép: chọn thao tác, nhập số (gõ `33,4` hay `33.4` đều được), chọn ô 1–5, bấm **Khắc phép**. Ô nhập sai thì viền đỏ và nút khắc bị khóa.
3. Chuyển trượng sang tay chính. **Chuột phải** để thi triển, **Shift + lăn chuột** để đổi ô. Dòng chữ phía trên thanh vật phẩm cho biết ô đang chọn.
4. Kết quả hiện ở giữa màn hình sau khi thi triển. Máy Đo (chuột phải) cho biết nhiệt độ môi trường, nhiệt độ mục tiêu, khoảng cách và khối lượng.

## Danh sách thử nhanh (ở đồng bằng, 20°C)

- Gia Nhiệt · Điểm · **33,4** vào khối gỗ → gỗ bốc cháy. **31,6** → không cháy, Máy Đo thấy gỗ khoảng 285°C; thêm 2 mana ngay sau đó → cháy.
- Gia Nhiệt · Điểm · **5,4** vào đá → đá nứt, rơi đá cuội. **363** → đá thành dung nham.
- Gia Nhiệt · Điểm · **259,3** vào nước → nước bốc hơi, chỉ có đám hơi. **359,3** → nổ như TNT ("bom hơi nước").
- Gia Nhiệt · Cầu r = 1 · **50** vào khoảng không cạnh tấm kính → kính vỡ, sinh vật gần đó bị hất văng.
- Gia Nhiệt · Đạn · 20 m/s, ngẩng 45° → đạn bay xa khoảng 40 m.
- Thu Nhiệt · Điểm · **500** vào dung nham → hắc diện thạch, nhận khoảng 392,8 mana.
- Thu Nhiệt · Điểm · **41,8** vào nước → băng, tốn khoảng 4,1 mana (gồm 1 mana phí kích hoạt).
- Nung một khối gỗ chưa tới mức cháy, thoát thế giới rồi vào lại trong vòng 20 giây → Máy Đo vẫn đo được nhiệt độ còn lưu.

## Chơi bằng CurseForge

1. Chạy `& "D:\code_base\New folder\mod\g.cmd" build`. File mod nằm ở `D:\code_base\New folder\mod\build\libs\phapthuat-1.0.0.jar`.
2. Trong CurseForge: tạo profile mới → chọn Minecraft 1.21.1, NeoForge.
3. Mở thư mục của profile (nút "..." → Open Folder), bỏ file `.jar` vào thư mục `mods`.

## Gỡ sạch

1. Chạy `& "D:\code_base\New folder\mod\g.cmd" --stop` để tắt Gradle đang chạy ngầm.
2. Xóa thư mục `D:\code_base\New folder`.
