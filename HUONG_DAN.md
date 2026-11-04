# Hướng dẫn: mod Pháp Thuật Vật Lý

Mọi thứ của mod nằm trong thư mục `C:\Users\PC\Documents\code_base\minecraft_mod`: mã nguồn ở `mod`, Java và Gradle ở `tools`. Không có gì cài ra ngoài thư mục này.

## Mở game có mod

Mở PowerShell rồi gõ:

```powershell
& "C:\Users\PC\Documents\code_base\minecraft_mod\mod\g.cmd" runClient
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

## Thử bảy phép mới (ở đồng bằng, 20°C)

Mỗi ý dưới đây chỉ nói **cách làm**, không ghi đáp số: bạn tự tính rồi mới thấy kết quả sau khi thi triển. Sách Công Thức có đủ công thức cho từng phép.

**Xung Lực** (Động) — E = ½·m·v², v = √(2E/m)
- Đứng trên đất, nhắm một con zombie, **Điểm**, chiều **Đẩy**, tăng dần năng lượng tới lúc nó bị hất bay.
- Cùng phép đó nhưng **đang nhảy** lúc thi triển: bạn bị giật lùi ngược lại (định luật 3 Newton). Đứng trên băng cũng vậy.
- Đổi sang chiều **Kéo** để hút mục tiêu về phía mình; thử **Cầu** để hất mọi thứ quanh tâm ra ngoài.
- **Bản thân**: bạn tự lao theo hướng nhìn, không bị giật lùi.

**Hồ Quang** (Điện từ) — E = U/d phải đạt 3 MV/m
- Nhắm một con lợn rồi tăng dần hiệu điện thế tới khi thấy tia phóng đúng. Dùng Máy Đo ngắm trước để biết khoảng cách cần tính.
- Lùi ra xa gấp đôi mà giữ nguyên hiệu điện thế: tia không tới nơi — tệ hơn, nó phóng ngược vào bạn.
- Thử lúc trời mưa: ngưỡng đánh thủng giảm nên tia tới được xa hơn.
- Đứng trong nước hoặc nhắm mục tiêu đang ở trong nước: điện lan ra mọi sinh vật quanh đó, kể cả bạn.
- Tăng năng lượng phóng lên cao để thấy hiệu ứng sét của vanilla (creeper tích điện, lợn hoá piglin xác sống).

**Từ Trường** (Điện từ) — u = B²/(2μ₀), chi phí = u × thể tích cầu
- Ném vài thỏi sắt xuống đất, đặt **Cầu** trùm lên rồi thi triển: thỏi sắt bị hút về tâm suốt thời gian giữ trường.
- Mặc đủ giáp sắt rồi đứng trong cầu của chính mình — bạn cũng bị kéo.
- So hai lần: một lần chỉ vật phẩm, một lần có golem sắt; tăng B lên gấp đôi xem lực hút mạnh lên bao nhiêu lần.

**Phát Quang** (Quang) — Φ = 683 × V(λ) × P
- **Điểm** với bước sóng 555 nm và công suất nhỏ: có khối sáng. Đổi bước sóng sang hai đầu quang phổ xem màu hạt và mức sáng đổi thế nào.
- Chọn 1000 nm mà vẫn mong có ánh sáng: không có gì sáng, mana vẫn mất — đúng vật lý.
- **Tia** với bước sóng nhìn thấy, công suất từ 1 W, chiếu vào một con mob: nó mất dấu bạn.
- **Tia** hồng ngoại mạnh chiếu vào gỗ: gỗ bắt lửa. Thử chiếu vào mình để biết ngưỡng đau.
- Bước sóng dưới 400 nm vào một con zombie giữa ban ngày: nó bốc cháy như đang nắng.

**Đốt Nhiên Liệu** (Hoá) — nhiệt cháy thay cho mana, cần oxy
- Có 1 than trong túi, **Điểm** vào gỗ: than bị trừ, gỗ bắt lửa — không tốn mana nào ngoài phí kích hoạt.
- Chọn 2 que mà túi chỉ có 1: không trừ gì và báo thiếu nhiên liệu.
- Đốt than trong một hộp đá kín có mob đứng trong đó: đủ oxy đâu mà cháy hết, khí CO làm mob (và bạn, nếu đứng chung) trúng độc.
- Thử các thao tác khác: **Tia** làm súng phun lửa, **Đạn** làm bom xăng.

**Bức Xạ Neutron** (Hạt nhân) — liều Gy = năng lượng hấp thụ / khối lượng
- Chiếu một con lợn ở khoảng cách vừa phải, tăng dần năng lượng và theo dõi Máy Đo (liều Gy) sau mỗi lần: hiệu ứng đổi bậc theo liều cộng dồn.
- Chèn một khối nước giữa bạn và mục tiêu rồi chiếu lại: liều tụt mạnh. Nước là vật chắn neutron tốt nhất.
- Đứng sát mục tiêu lúc chiếu để thấy liều tán xạ bắn sang chính bạn.
- Chờ một lúc rồi đo lại: liều giảm dần theo chu kỳ bán rã.

**Nâng** (Hấp dẫn) — E = m·g·h
- Nhắm một khối đá, đặt độ cao và thời gian lơ lửng: khối bay lên rồi rơi xuống, đặt lại chỗ cũ.
- Nâng khối lên ngay trên đầu một con mob rồi thả: cú rơi gây sát thương theo thế năng.
- Thử nhắm vào rương có đồ hoặc đá nền: phép thất bại và không trừ mana.
- Tự nâng mình lên cao rồi thả: sát thương rơi theo vanilla.

## Chơi bằng CurseForge

1. Chạy `& "C:\Users\PC\Documents\code_base\minecraft_mod\mod\g.cmd" build`. File mod nằm ở `C:\Users\PC\Documents\code_base\minecraft_mod\mod\build\libs\phapthuat-1.0.0.jar`.
2. Trong CurseForge: tạo profile mới → chọn Minecraft 1.21.1, NeoForge.
3. Mở thư mục của profile (nút "..." → Open Folder), bỏ file `.jar` vào thư mục `mods`.

## Gỡ sạch

1. Chạy `& "C:\Users\PC\Documents\code_base\minecraft_mod\mod\g.cmd" --stop` để tắt Gradle đang chạy ngầm.
2. Xóa thư mục `C:\Users\PC\Documents\code_base\minecraft_mod`.
