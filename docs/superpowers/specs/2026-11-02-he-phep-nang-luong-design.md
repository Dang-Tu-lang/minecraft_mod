# Thiết kế: Mod "Pháp Thuật Vật Lý" — Khung hệ phép và các phép nền tảng

Ngày: 2026-11-02
Trạng thái: chờ duyệt

## 1. Ý tưởng và lý do

Một mod phép thuật cho Minecraft Java mà **mọi phép đều dựng trên vật lý thật**. Sức mạnh của người chơi đến từ **kiến thức toán, lý, hóa thật của chính họ**, không phải từ việc cày cấp. Đây là chỗ khác biệt so với mọi mod phép thuật hiện có (đã khảo sát Arcanism/Programmable Magic, Psi, Ethernano, Hex Casting: các mod đó dựa vào lập trình hoặc ghép ký hiệu, không dùng công thức và hằng số vật liệu thật làm luật chơi).

Chủ dự án không biết lập trình. Họ đề xuất ý tưởng, builder viết toàn bộ code, còn họ chơi thử trong game để đánh giá.

### 1.1 Khung hệ phép (chủ dự án đã chốt)

- **Mana là năng lượng thuần.** 1 mana = 10 kJ.
- **7 nguyên tố = 7 dạng năng lượng:** Nhiệt, Động, Điện từ, Quang, Hóa, Hạt nhân, Hấp dẫn. "Lửa, nước, băng, đất" **không phải** nguyên tố. Lửa chỉ là thứ xảy ra khi Nhiệt đủ cao và có thứ để cháy.
- **Một phép** = chuyển năng lượng thành một dạng rồi tác động lên thế giới theo đúng vật lý.
- **Thao tác** (hình dạng, hướng đi: một điểm, một tia, một đường trên mặt đất, quả cầu, bức tường, viên đạn, chính mình) là một lớp riêng, áp được lên nhiều nguyên tố. Ví dụ "đẩy lửa thành một đường tới kẻ thù" = Gia Nhiệt + thao tác Đường.
- **Ghép phép** về lâu dài = nối nhiều bước chuyển hóa năng lượng. Mỗi bước hao hụt theo hiệu suất thật (Điện → Nhiệt gần 100%, Nhiệt → Động bị giới hạn bởi Carnot...).
- Không cần chính xác như sách giáo khoa ở mọi chỗ. Phép điều khiển (nặn hình, đẩy theo hướng) vẫn được, miễn có nền vật lý.

### 1.2 Cách chơi

1. Đọc **Sách Công Thức** (công thức, giải thích, bảng dữ liệu vật liệu thật).
2. Chọn phép, chọn thao tác, **tự tính** rồi nhập thông số.
3. **Khắc** phép vào một ô của **Pháp Trượng**.
4. Chọn ô và **thi triển**.

Tính đúng thì thành công. Tính thiếu thì thất bại, mất mana. Tính sai hoặc liều lĩnh thì **phản tác dụng**, và phản tác dụng luôn là hệ quả vật lý thật (bị giật lùi theo định luật 3 Newton, tự hít khí CO, tự bị phóng điện...).

## 2. Mục tiêu đợt này

- Nền móng: mana, Sách Công Thức, Pháp Trượng, Máy Đo, lớp thao tác.
- **9 phép nền tảng**, mỗi nguyên tố có ít nhất một phép (mục 8).
- Lõi vật lý tách riêng, có unit test dùng đúng các ví dụ số trong tài liệu này.
- Kiến trúc dễ thêm phép, thao tác và (đợt sau) chuỗi ghép phép.

## 3. Không làm trong đợt này (non-goals)

- **Chuỗi ghép phép do người chơi tự nối** (Hạt nhân → Nhiệt → Động...). Đợt này người chơi "ghép" bằng cách chọn **phép + thao tác + thông số**. Một số phép tự có chuỗi vật lý bên trong (Hồ Quang dồn năng lượng thành nhiệt; Gia Nhiệt làm nước hóa hơi và nổ). Kiến trúc phải chừa chỗ cho chuỗi về sau, và bảng hiệu suất chuyển hóa (mục 7.4) đã được ghi sẵn.
- Phân hạch bằng nhiên liệu hạt nhân thật (cần thêm quặng urani, để đợt sau).
- Railgun Lorentz (sẽ là chuỗi Điện từ → Động ở đợt sau).
- Tiến trình mở khóa, lên cấp, tăng mana tối đa. Mọi phép có sẵn từ đầu.
- Ghi đè trọng lực của mọi sinh vật. Chỉ vật do phép tạo ra (đạn) mới bay theo g = 9,81 m/s².
- Cân bằng cho chơi nhiều người (vẫn phải chạy đúng client/server, nhưng chỉ cần thử chơi đơn).
- Bản dịch tiếng Anh.

## 4. Không được xảy ra (anti-goals)

- **Không có máy tính hộ.** Giao diện tuyệt đối không hiển thị trước "cần bao nhiêu mana" hay "phép này sẽ thành công". Kết quả chỉ được báo **sau khi thi triển**.
- **Không bịa công thức.** Công thức phải là công thức vật lý thật. Hằng số vật liệu lấy trong khoảng giá trị tham khảo thật. Chỗ nào là ước lượng cho game thì sách ghi rõ "ước lượng".
- Không crash game, không làm hỏng thế giới đã lưu.
- Không viết code rải rác: thêm một phép hay một thao tác mới không được bắt phải sửa ở nhiều nơi không liên quan.

## 5. Ràng buộc

- Minecraft **1.21.1**, NeoForge **21.1.265**, Java 21. Project nằm tại `D:\code_base\New folder\mod` (template MDK, đã build được).
- **Mọi thứ nằm trong `D:\code_base\New folder`.** JDK portable ở `tools\jdk-21`, cache Gradle ở `tools\gradle-home`. Luôn build bằng `D:\code_base\New folder\mod\g.cmd` (script đặt sẵn `JAVA_HOME` và `GRADLE_USER_HOME`). Gọi bằng **đường dẫn đầy đủ**, vì máy này không tự tìm lệnh trong thư mục hiện tại.
- Không cài thêm gì ra ngoài thư mục trên.
- Toàn bộ chữ trong game bằng **tiếng Việt có dấu**. Viết `vi_vn.json` và `en_us.json` cùng nội dung tiếng Việt.

## 6. Nền móng

### 6.1 Mana

- 1 mana = 10 kJ. Tối đa 1000, hồi 2 mana/giây, hồi đầy khi hồi sinh.
- Hiển thị trên màn hình (thanh + số, 1 chữ số thập phân). Mọi ô nhập mana nhận số thập phân tới 0,01.
- Phép có thông số "năng lượng E (mana)" thì trừ đúng E, dù phép thành công hay thất bại. Nếu không đủ mana để trả thì phép không chạy, báo "không đủ mana".
- Tỉ lệ đổi, mana tối đa, tốc độ hồi và các hệ số cân bằng ghi trong tài liệu này đều chỉnh được ở file config.

### 6.2 Sách Công Thức

- Vật phẩm. Chuột phải để mở giao diện.
- **Trang mở đầu:** cách chơi; 1 mana = 10 kJ; 7 nguyên tố; luật "lõi 1 lít" (6.6); luật thao tác (6.5); bảng hằng số chung; cách đổi °C ↔ K.
- **Mỗi phép một trang:** nguyên tố, công thức, giải thích ngắn, bảng dữ liệu liên quan, các thao tác được phép, các ô nhập thông số (có đơn vị, min/max), luật thành công / thất bại / phản tác dụng.
- **Khắc phép:** khi đang cầm Pháp Trượng ở **tay phụ** và mở sách, trang phép hiện thêm chọn thao tác, chọn ô (1–5) và nút **"Khắc phép"**. Nút này ghi phép + thao tác + mọi thông số vào ô đó (ghi đè nếu ô đã có phép). Không có Pháp Trượng ở tay phụ thì chỉ đọc được.

### 6.3 Pháp Trượng

- 5 ô phép, lưu trên chính vật phẩm.
- **Shift + lăn chuột** khi đang cầm: đổi ô đang chọn. **Chuột phải**: thi triển. Hồi chiêu 0,5 giây.
- Khi đang cầm, màn hình hiện tên phép, thao tác và thông số của ô đang chọn.

### 6.4 Máy Đo

Vật phẩm. Chuột phải để hiện số liệu đo được. Dụng cụ này đo giúp người chơi, nhưng **không tính hộ**:
- Nhiệt độ môi trường tại chỗ đứng (°C và K).
- Mục tiêu đang nhìn: tên vật liệu, nhiệt độ, khoảng cách tới mục tiêu (m). Nếu là sinh vật thì thêm khối lượng (kg).
- Có đang mưa hay không, và mục tiêu có nằm trong nước không.

**Nhiệt độ môi trường:** T_mt (°C) = 25 × nhiệt độ biome tại vị trí đó (dùng hàm có tính độ cao). Ví dụ: đồng bằng 0,8 → 20°C, sa mạc 2,0 → 50°C, đồng tuyết 0,0 → 0°C.

**Nhiệt độ tự nhiên** của mục tiêu (khi chưa bị phép tác động) = T_mt, trừ: băng/tuyết = min(T_mt, 0°C); sinh vật sống 37°C; sinh vật undead = T_mt; khối nóng theo bảng 7.2. Mọi phép và Máy Đo đều dùng **nhiệt độ hiện tại** của mục tiêu: nếu mục tiêu đang được lưu trạng thái (6.8) thì lấy giá trị đã lưu, không thì lấy nhiệt độ tự nhiên.

### 6.5 Thao tác

Mỗi phép khai báo các thao tác nó cho phép. Thao tác quyết định **những đơn vị nào bị tác động** (một khối, một sinh vật, hoặc 1 m³ không khí) và năng lượng chia cho chúng thế nào.

| Thao tác | Tác động lên | Thông số thêm |
|---|---|---|
| **Điểm** | Khối hoặc sinh vật đang nhìn, tầm 16 khối. Nhìn vào khoảng không thì là ô không khí ở 16 khối. | — |
| **Tia** | Đường thẳng từ mắt theo hướng nhìn: mọi sinh vật trên đường và khối rắn đầu tiên chặn đường (tia dừng ở đó). | độ dài L (m), 1–16 |
| **Đường** | Vệt trên mặt đất từ chân người chơi tới điểm đang nhìn: khối mặt trên cùng ở mỗi bước và sinh vật đứng trên đó. | — |
| **Cầu** | Tâm cầu là ô của khối đang nhìn (nhìn vào sinh vật thì là ô dưới chân nó; nhìn vào khoảng không thì là ô không khí ở 16 khối). Gồm mọi ô có tâm cách tâm cầu ≤ r (kể cả ô không khí) và mọi sinh vật trong các ô đó. Số ô: r = 1 → 7, r = 2 → 33, r = 3 → 123, r = 4 → 257, r = 5 → 515 (sách in bảng này). | bán kính r (m), 1–5 |
| **Tường** | Mặt phẳng rộng × cao, đứng thẳng và vuông góc với hướng nhìn nằm ngang. Tâm là ô không khí ngay trước mặt khối đang nhìn. Gồm các ô trong mặt phẳng và sinh vật chạm vào. | rộng 1–7, cao 1–5 |
| **Đạn** | Bắn một viên đạn phép (0,1 kg) theo hướng nhìn, bay theo đúng chuyển động ném xiên (g = 9,81 m/s², 1 khối = 1 m, bỏ qua lực cản). Khi chạm thì áp dụng phép như **Điểm** tại chỗ chạm. Tối đa 10 giây bay. Đạn có thể rơi trúng chính người bắn (không miễn nhiễm sau 1 giây bay). | tốc độ v (m/s), 5–100; chi phí thêm ½·0,1·v² J |
| **Bản thân** | Chính người chơi. | — |

**Luật chia năng lượng:** với Tia, Đường, Cầu, Tường, năng lượng **chia đều** cho N đơn vị mà phép tác động được. Mỗi ô (kể cả ô không khí, nếu phép tác động được không khí) là một đơn vị, mỗi sinh vật cũng là một đơn vị. Phạm vi càng rộng thì mỗi đơn vị nhận càng ít, đúng như năng lượng bị trải mỏng ngoài đời. Mỗi phép tự định nghĩa đơn vị nào nó tác động được (ví dụ Gia Nhiệt tác động được cả không khí, Xung Lực chỉ tác động lên sinh vật). Sách nói rõ luật này. Dòng báo kết quả (6.7) luôn ghi N để người chơi học cách ước lượng.

### 6.6 Luật "lõi 1 lít"

Phép nhiệt tác động lên một khối thì tác động lên **lõi 1 lít (V₀ = 0,001 m³)** của khối đó: m = ρ·V₀. Khi lõi đổi trạng thái thì cả khối đổi theo. Riêng phép **Nâng** dùng khối lượng **cả khối** (1 m³), vì nó nâng cả khối lên. Sách nói rõ luật này.

### 6.7 Báo kết quả

Sau mỗi lần thi triển, hiện một dòng có số liệu, để người chơi học từ kết quả. Ví dụ: `Gia Nhiệt: lõi gỗ sồi đạt 285°C / cần 300°C — thất bại, mất 31,6 mana`. Chỉ báo sau, không bao giờ báo trước.

### 6.8 Trạng thái được lưu lại và nguội dần (chủ dự án đã chọn)

Mục tiêu **giữ** nhiệt độ sau khi bị phép tác động, rồi trở về nhiệt độ tự nhiên dần dần như ngoài đời. Nhờ vậy thi triển nhiều lần thì cộng dồn được, và Máy Đo đo được nhiệt độ hiện tại.

- **Khối:** chỉ lưu những khối từng bị phép tác động (bảng thưa theo từng thế giới, lưu cùng file save). Mỗi mục lưu nhiệt độ lõi hiện tại và phần nhiệt chuyển pha đã nhận (ví dụ băng ở 0°C đã nhận 200/306 kJ để tan).
- **Sinh vật:** lưu nhiệt độ lõi (dùng chung cho Gia Nhiệt và Thu Nhiệt) và **liều phóng xạ cộng dồn** (Gy).
- **Nguội dần (định luật làm nguội Newton):** T(t) = T_tự nhiên + (T − T_tự nhiên)·e^(−t/τ). Mặc định khối và sinh vật có chu kỳ bán rã 20 giây (τ ≈ 28,9 s), tức sau khoảng 1 phút gần như về lại bình thường. Phần nhiệt chuyển pha đã nhận cũng giảm theo cùng tốc độ.
- **Khối nóng tự nhiên** (dung nham, magma) bị rút nhiệt thì hồi nhiệt về nhiệt độ của chúng chậm hơn: chu kỳ bán rã 5 phút. Vì vậy rút nhiệt liên tục một khối dung nham sẽ nhận mana ít dần (nhiệt độ thấp hơn thì nhiệt còn lại ít hơn và hiệu suất Carnot thấp hơn), cho tới khi nguội hẳn và thành hắc diện thạch.
- **Liều phóng xạ** giảm với chu kỳ bán rã 60 giây (ước lượng game; ngoài đời cơ thể hồi phục chậm hơn nhiều).
- **Hiệu ứng theo bậc** (lạnh ở 8.2, phóng xạ ở 8.8) được xét lại mỗi giây theo giá trị hiện tại và kéo dài chừng nào giá trị còn ở bậc đó.
- Bỏ mục lưu khi giá trị đã về gần tự nhiên (lệch dưới 1 K, liều dưới 0,05 Gy), hoặc khi khối bị phá/thay bằng khối khác. Giới hạn số mục lưu (gợi ý 10 000 mỗi thế giới) để file save không phình to.
- Mọi chu kỳ bán rã chỉnh được trong config.

## 7. Hằng số và dữ liệu

Mọi số liệu nằm ở **một chỗ duy nhất** trong code, và sách đọc từ chính chỗ đó để hiển thị. Builder được tinh chỉnh trong khoảng giá trị tham khảo thật. Nếu đổi thì phải cập nhật ví dụ ở mục 8 và unit test cho khớp.

### 7.1 Hằng số chung

| Hằng số | Giá trị |
|---|---|
| 0°C | 273,15 K |
| Gia tốc trọng trường g | 9,81 m/s² |
| Hằng số khí R | 8,314 J/(mol·K) |
| Áp suất khí quyển P₀ | 101 325 Pa |
| Không khí: ρ / c_v | 1,2 kg/m³ / 718 J/(kg·K) |
| Oxy trong không khí | 0,276 kg O₂ / m³ |
| Quy tắc Thornton (nhiệt tỏa ra trên mỗi kg O₂ khi đốt chất hữu cơ) | 13,1 MJ/kg O₂ |
| Ngưỡng đánh thủng không khí | 3 MV/m (trời mưa: × 0,8) |
| Điện dung cơ thể người | 100 pF |
| Độ từ thẩm chân không μ₀ | 1,2566 × 10⁻⁶ H/m |
| Hiệu suất phát sáng cực đại | 683 lm/W tại 555 nm |
| Ngưỡng bức xạ gây đau / làm gỗ bắt cháy | 2,5 kW/m² / 12,5 kW/m² |
| Áp suất dư làm vỡ kính / gây sát thương | 7 kPa / 35 kPa |
| Hiệu suất tạo neutron (ước lượng game) | 2 × 10⁻⁴ |

### 7.2 Vật liệu (ρ: kg/m³, c: kJ/(kg·K))

| Vật liệu | Khối Minecraft | ρ | c | Nhiệt độ quan trọng | Khác |
|---|---|---|---|---|---|
| Gỗ | gỗ khúc, ván, các khối gỗ | 700 | 1,7 | bắt cháy 300°C | |
| Len | len | 300 | 1,36 | bắt cháy 570°C | |
| Lá cây | các loại lá | 150 | 2,0 | bắt cháy 280°C (ước lượng) | |
| Rơm | kiện rơm | 150 | 1,5 | bắt cháy 250°C (ước lượng) | |
| Than | khối than | 1350 | 1,26 | bắt cháy 450°C | |
| Sáp | nến | 900 | 2,9 | bắt cháy 250°C | thắp nến |
| TNT | TNT | 1650 | 1,13 | bắt cháy 240°C | kích nổ |
| Nước | nước nguồn | 1000 | 4,186 | sôi 100°C | L_hóa hơi 2257 kJ/kg; 55,51 mol/lít; c_p hơi 2,0 |
| Băng | băng, băng nén | 917 | 2,09 | nóng chảy 0°C | L_nc 334 kJ/kg; E 9 GPa; α 5×10⁻⁵ /K; σ_bền 1 MPa |
| Tuyết | khối tuyết | 300 | 2,09 | nóng chảy 0°C | L_nc 334 kJ/kg |
| Cát (thạch anh) | cát | 1600 | 0,83 | nóng chảy 1713°C | L_nc 156 kJ/kg |
| Đá (granit) | đá, đá cuội, đá phiến sâu, granit, diorit, andesit | 2700 | 0,79 | nóng chảy 1215°C | L_nc 400 kJ/kg; E 50 GPa; α 8×10⁻⁶ /K; σ_bền 10 MPa |
| Thủy tinh | thủy tinh, tấm kính | 2500 | 0,84 | | E 70 GPa; α 9×10⁻⁶ /K; σ_bền 30 MPa |
| Hắc diện thạch | obsidian | 2400 | 0,84 | | E 70 GPa; α 6×10⁻⁶ /K; σ_bền 30 MPa (ước lượng) |
| Dung nham | dung nham nguồn | 2700 | 1,2 | 1200°C | L_nc 400 kJ/kg |
| Khối magma | khối magma | 2700 | 1,0 | 700°C (ước lượng) | |
| Lửa | khối lửa | | | 800°C | nhiệt tối đa 20 kJ (ước lượng) |
| Lửa trại | lửa trại đang cháy | | | 800°C | nhiệt tối đa 200 kJ (ước lượng) |
| Không khí | ô không khí | 1,2 | 0,718 (c_v) | | |

### 7.3 Sinh vật

- **Lõi nhiệt** của sinh vật: ρ 1000, c 3,5, bắt cháy 400°C (ước lượng). Mob miễn lửa của vanilla thì miễn phép nhiệt.
- **Khối lượng (kg, ước lượng game):** người chơi 70, zombie/dân làng/thây ma các loại 70, bộ xương 30, creeper 50, nhện 30, enderman 80, lợn 100, cừu 60, bò 600, ngựa 500, gà 2, sói 40, golem sắt 500. Sinh vật không có trong bảng: 100 kg/m³ × thể tích hitbox.
- **Lượng sắt (cho phép Từ Trường):** 1 thỏi sắt = 1 kg. Giáp sắt tính theo số thỏi để chế tạo (mũ 5, áo 8, quần 7, giày 4). Golem sắt: toàn bộ 500 kg. Xe goòng: 5 kg. Vật phẩm sắt rơi trên đất: 1 kg mỗi thỏi.

### 7.4 Hiệu suất chuyển hóa (dùng cho chuỗi ghép phép ở đợt sau)

Lưu sẵn trong bảng dữ liệu, đợt này chưa dùng: Điện → Nhiệt 100%; Điện → Động 90%; Điện → Quang 40%; Động → Nhiệt 100%; Động → Điện 90%; Hóa → Nhiệt 100%; Hóa → Điện 60%; Quang → Nhiệt 100%; Quang → Điện 20%; Hạt nhân → Nhiệt 100%; Hấp dẫn → Động 100%; Nhiệt → Động theo Carnot η = 1 − T_lạnh/T_nóng.

## 8. Chín phép nền tảng

Quy ước: ví dụ số tính ở T_mt = 20°C (293,15 K), làm tròn. "E·10 kJ" nghĩa là năng lượng E (mana) đổi ra kJ.

### 8.1 NHIỆT — Gia Nhiệt

- **Vật lý:** nhiệt dung riêng, nhiệt chuyển pha, ứng suất nhiệt, khí lý tưởng, hóa hơi giãn nở. Q = m·c·ΔT (+ m·L khi chuyển pha).
- **Thông số:** năng lượng E (mana), 0–1000.
- **Thao tác:** Điểm, Tia, Đường, Cầu, Tường, Đạn, Bản thân.
- **Kết quả theo mục tiêu.** Game tính nhiệt độ lõi đạt được rồi áp dụng **bậc cao nhất đạt tới**:

| Mục tiêu | Bậc kết quả |
|---|---|
| Khối dễ cháy (gỗ, len, lá, rơm, than) | Đạt nhiệt độ bắt cháy → đặt lửa vào ô không khí cạnh mặt đang nhìn (với Điểm) hoặc ô không khí phía trên (thao tác khác) |
| TNT / nến / lửa trại tắt | Đạt nhiệt độ bắt cháy → kích nổ / thắp |
| Băng, tuyết | Đủ Q nóng chảy → nước. Đủ cả Q nóng chảy + Q hóa hơi → bốc hơi luôn (như nước bên dưới) |
| Nước nguồn, khối ngậm nước, bọt biển ướt, bùn | Đủ Q hóa hơi → mất nước (bọt biển → khô, bùn → đất, khối ngậm nước → hết nước) + **hơi nổ** (bên dưới) |
| Cát | Đủ Q nóng chảy → thủy tinh |
| Họ đá | ΔT ≥ σ_bền/(E·α) → **nứt vỡ** (rơi đá cuội). Đủ Q nóng chảy → **dung nham** (bậc cao hơn) |
| Thủy tinh, băng, hắc diện thạch | ΔT ≥ σ_bền/(E·α) → **nứt vỡ** (thủy tinh/băng không rơi gì, hắc diện thạch rơi chính nó) |
| Khối không cháy khác | Ngọn lửa mana cháy ngắn trên mặt khối (khoảng 1 giây) rồi tắt |
| Không khí (Cầu, Tường) | Mỗi ô không khí nhận phần năng lượng của mình theo luật chia (6.5). Gộp các ô không khí lại thành một khối khí: m_khí = số ô không khí × 1,2 kg, E_khí = tổng phần năng lượng của chúng. ΔT = E_khí/(m_khí·c_v), ΔP = P₀·(T₂/T₁ − 1). ΔP ≥ 7 kPa: thủy tinh trong vùng vỡ. Mọi sinh vật bị hất ra xa tâm, lực tỉ lệ ΔP (builder chọn hệ số, 50 kPa phải hất văng rõ rệt). ΔP ≥ 35 kPa: mất máu tỉ lệ ΔP. T₂ ≥ 300°C: sinh vật trong vùng bốc cháy |
| Sinh vật | Lõi đạt 400°C → bốc cháy 3 giây, cộng 1 giây cho mỗi 10 mana dư, tối đa 10 giây |

- **Hơi nổ khi nước hóa hơi:** E_dư = phần năng lượng còn lại sau khi hóa hơi; T_hơi = 373,15 + E_dư / (m × 2,0) K; W = 55,51 × m × 8,314 × T_hơi (J); công suất nổ p = 4 × W / 400 kJ, tối đa 8 (TNT vanilla = 4).
  - p < 2: chỉ có đám hơi (bán kính 2 khối, 3 giây, bỏng hơi 1 máu/giây) và hất văng nhẹ.
  - p ≥ 2: thêm vụ nổ kiểu vanilla công suất p (phá khối, có tùy chọn config để tắt). Bán kính ảnh hưởng ≈ 2·p khối, sách ghi rõ.
- **Luật oxy:** mục tiêu nằm trong nước thì không bắt lửa.
- **Thất bại:** chưa đạt bậc nào thì mục tiêu chỉ bốc khói và **giữ nhiệt độ mới** (6.8), mana mất. Thi triển tiếp trước khi nó nguội thì nhiệt cộng dồn.
- **Phản tác dụng:**
  - Năng lượng một đơn vị nhận ≥ 3 lần mức cần cho **bậc cao nhất mà vật liệu đó có** (gỗ: bắt cháy; đá: nóng chảy; băng: bốc hơi; sinh vật: bắt cháy): lửa bùng ra khối dễ cháy xung quanh (bán kính 1, cộng 1 cho mỗi lần dư thêm, tối đa 4). Người chơi trong bán kính đó bị cháy. Nhắm bậc cao mà không đủ năng lượng (ví dụ muốn nấu chảy đá mà chỉ đủ làm nứt) thì chỉ là thất bại một phần, **không** phản tác dụng. Khối không cháy được và không khí không có luật này.
  - Người chơi nằm trong vùng hơi, vùng không khí nóng hoặc vụ nổ thì chịu y như sinh vật khác.
  - Đạn bắn lên trời có thể rơi trúng chính mình.
  - Bản thân: tự đốt chính mình.
- **Ví dụ:**
  - Gỗ: 0,7 × 1,7 × 280 = 333,2 kJ → **33,3 mana**.
  - Đá nứt: ΔT = 10 MPa / (50 GPa × 8×10⁻⁶) = 25 K → 2,7 × 0,79 × 25 = 53,3 kJ → **5,3 mana**. Đá chảy: 2,7 × 0,79 × 1195 + 2,7 × 400 = 3628,9 kJ → **362,9 mana**.
  - Băng chảy: 0,917 × 334 = 306,3 kJ → **30,6 mana**. Cát → thủy tinh: 1,6 × 0,83 × 1693 + 1,6 × 156 = 2497,9 kJ → **249,8 mana**.
  - Nước bốc hơi: 4,186 × 80 + 2257 = 2591,9 kJ → **259,2 mana**, W ≈ 172,2 kJ, p ≈ 1,7 (chỉ đám hơi). Thêm 100 mana dư: T_hơi ≈ 873 K, W ≈ 403 kJ, p ≈ **4,0** (ngang TNT), tổng **359,2 mana**. Đây là "bom hơi nước" của chủ dự án.
  - Không khí, Cầu r = 1 (7 ô, toàn không khí), E = 50 mana: m_khí = 8,4 kg; ΔT ≈ 82,9 K; ΔP ≈ **28,7 kPa** (vỡ kính, hất văng, chưa gây sát thương). Muốn chạm ngưỡng sát thương 35 kPa thì cần **≈ 61,1 mana**.
  - Đạn v = 20 m/s, ngẩng 45°: tầm xa = v²/g ≈ **40,8 m**.

### 8.2 NHIỆT — Thu Nhiệt

- **Vật lý:** định luật 2 nhiệt động lực học. Rút nhiệt từ vật **nóng hơn** môi trường thì **được** mana (động cơ nhiệt, η = 1 − T_mt/T_vật). Làm lạnh vật **xuống dưới** môi trường thì **tốn** mana (máy lạnh, công W = Q / COP, COP = T_cuối/(T_mt − T_cuối)). Nhiệt độ luôn tính bằng Kelvin.
- **Thông số:** nhiệt muốn rút Q (mana quy đổi), 0–1000.
- **Thao tác:** Điểm, Cầu, Bản thân.
- **Tính:** game hạ nhiệt độ lõi mục tiêu theo Q (Q = m·c·ΔT + chuyển pha). Phần nhiệt ở trên T_mt cho mana = Q_phần × (1 − T_mt/T_đầu). Phần dưới T_mt tốn mana = Q_phần / COP. Mana được/tốn tính và trừ/cộng lúc thi triển. Phí kích hoạt 1 mana.
- **Kết quả theo mục tiêu:**
  - Dung nham: rút hết tới T_mt (Q ≥ Q_max) → hắc diện thạch. Q_max = m·c·(1200 − T_mt) + m·L_nc.
  - Khối magma: rút hết → đá Nether.
  - Lửa, lửa trại: rút hết nhiệt tối đa (bảng 7.2) → tắt.
  - Nước: hạ tới 0°C và đủ L_nc → băng.
  - Sinh vật sống (37°C): thân nhiệt giảm. ΔT ≥ 2 K → chậm chạp; ≥ 5 K → hiệu ứng đóng băng (như tuyết bột vanilla) + 1 máu/giây; ≥ 9 K → mất máu nặng. Sinh vật đang cháy thì tắt lửa.
  - Sinh vật undead (ở T_mt): chỉ làm lạnh được, tốn mana theo COP.
  - Rút một phần (chưa tới mốc chuyển trạng thái): nhận/trả mana, mục tiêu giữ trạng thái nhưng **nhiệt độ mới được lưu lại** (6.8). Lần rút sau tính từ nhiệt độ đó.
  - Vật nóng có nhiệt hữu hạn (lửa, dung nham...): Q vượt Q_max thì chỉ rút tới Q_max, phần dư bỏ qua và có báo.
- **Phản tác dụng:**
  - Không đủ mana để trả công làm lạnh: phần còn thiếu bị rút từ thân nhiệt người chơi → người chơi bị đóng băng và mất máu tỉ lệ.
  - Bản thân hoặc đứng trong Cầu: tự hạ thân nhiệt như sinh vật khác.
- **Ví dụ:**
  - Dung nham: Q_max = 2,7 × 1,2 × 1180 + 2,7 × 400 = 4903 kJ; η = 1 − 293,15/1473,15 ≈ 0,80 → nhận **≈ 392,8 mana**, dung nham thành hắc diện thạch.
  - Nước → băng: Q = 4,186 × 20 + 334 = 417,7 kJ; COP = 273,15/20 ≈ 13,66 → tốn **≈ 3,1 mana**. Làm đá rẻ, vì chênh lệch nhiệt độ nhỏ (đúng như máy lạnh ngoài đời).
- Hệ số mana nhận được chỉnh được trong config (mặc định 1,0).

### 8.3 ĐỘNG — Xung Lực

- **Vật lý:** động năng E = ½mv², xung lượng J = m·v, **định luật 3 Newton**.
- **Thông số:** năng lượng E (mana), 0–100; chiều: Đẩy (ra xa người chơi) hoặc Kéo (về phía người chơi).
- **Thao tác:** Điểm, Tia, Cầu (hướng tỏa ra từ tâm, hoặc hút về tâm khi Kéo), Bản thân (lao theo hướng nhìn).
- **Tác động lên:** sinh vật, vật phẩm rơi, xe goòng, thuyền. Không tác động lên khối.
- **Tính:** mục tiêu khối lượng m nhận vận tốc v = √(2E/m) theo hướng của thao tác. Xung lượng J = √(2·m·E).
- **Phản tác dụng (định luật 3 Newton):** nếu người chơi **không đứng trên mặt đất** (đang nhảy, rơi, bơi) hoặc đứng trên băng/băng nén/băng lam, người chơi nhận xung lượng ngược chiều: Δv = J_tổng / 70 kg. Đứng trên đất thường thì mặt đất hấp thụ lực đẩy ngược. Đẩy một con bò 600 kg khi đang nhảy sẽ hất chính mình bay đi.
- Vận tốc áp dụng giới hạn tối đa 40 m/s để không làm lỗi game.
- **Ví dụ:** zombie 70 kg, E = 1 mana (10 kJ) → v = √(20 000/70) ≈ **16,9 m/s**. Golem sắt 500 kg, E = 1 mana → v ≈ 6,3 m/s, J ≈ 3162 N·s. Nếu người chơi đang nhảy thì bị giật lùi ≈ 45 m/s, giới hạn còn 40.

### 8.4 ĐIỆN TỪ — Hồ Quang

- **Vật lý:** điện trường E = U/d, ngưỡng đánh thủng không khí 3 MV/m. Năng lượng tích điện ½·C·U² với C = điện dung cơ thể 100 pF. Điện năng thành nhiệt khi phóng qua mục tiêu.
- **Thông số:** hiệu điện thế U (kV), 1–50 000; năng lượng phóng E (mana), 0–200.
- **Thao tác:** Điểm.
- **Tính:** d = khoảng cách thật từ người chơi tới mục tiêu khi thi triển (đo được bằng Máy Đo). Chi phí = E + ½·100 pF·U² (đổi ra mana).
- **Thành công** khi U/d ≥ ngưỡng (trời mưa: ngưỡng × 0,8): tia hồ quang nối người chơi với mục tiêu.
  - Sinh vật mất máu = E (kJ) / 20.
  - Năng lượng E cũng dồn vào mục tiêu dưới dạng nhiệt, áp dụng luật Gia Nhiệt (có thể làm cháy).
  - E ≥ 50 mana: hiệu ứng sét của vanilla (creeper tích điện, lợn → piglin xác sống, dân làng → phù thủy).
- **Thất bại / phản tác dụng:**
  - U/d chưa đủ nhưng U/0,5 m ≥ ngưỡng: điện phóng vào vật dẫn gần nhất là **chính người chơi**, người chơi chịu toàn bộ sát thương.
  - U quá thấp cả cho 0,5 m: chỉ lách tách, mana mất.
  - Mục tiêu hoặc người chơi đang ở trong nước: điện lan trong nước, mọi sinh vật trong nước trong bán kính 4 khối (kể cả người chơi) chia nhau sát thương.
- **Ví dụ:** mục tiêu cách 5 m → cần U ≥ **15 000 kV**; phí tích điện ½ × 10⁻¹⁰ × (1,5×10⁷)² = 11,25 kJ ≈ **1,1 mana**. Phóng E = 20 mana → **10 máu** sát thương.

### 8.5 ĐIỆN TỪ — Từ Trường

- **Vật lý:** năng lượng từ trường u = B²/(2μ₀) trên mỗi m³. Lực hút vật sắt từ tăng theo B².
- **Thông số:** cảm ứng từ B (T), 0,1–5; bán kính r (m), 1–6; thời gian t (giây), 1–10.
- **Thao tác:** Cầu (tâm ở điểm đang nhìn). Phép này luôn có dạng cầu.
- **Chi phí:** u × 4/3·π·r³ (đổi ra mana), trừ một lần lúc thi triển.
- **Hiệu ứng** trong t giây: mọi thứ chứa sắt trong cầu bị hút về tâm với gia tốc a = k·B²·(m_sắt/m) (builder chọn k sao cho B = 0,5 T kéo rõ một zombie mặc đủ giáp sắt). Gồm vật phẩm sắt rơi, xe goòng, golem sắt, sinh vật mặc giáp sắt.
- **Phản tác dụng:** người chơi mặc giáp sắt ở trong cầu cũng bị hút về tâm.
- **Ví dụ:** B = 0,5 T → u ≈ 99,5 kJ/m³; r = 1 m → ≈ 416,7 kJ → **41,7 mana**.

### 8.6 QUANG — Phát Quang

- **Vật lý:** bước sóng quyết định ánh sáng là gì. Mắt nhạy nhất ở 555 nm (màu xanh lá). Quang thông Φ = 683 × V(λ) × P, với V(λ) ≈ 1,019·exp(−285,4·(λ_µm − 0,559)²). Cường độ giảm theo nghịch đảo bình phương khoảng cách: I = P/(4πd²).
- **Thông số:** bước sóng λ (nm), 100–2000; công suất P (W), 0,01–100 000; thời gian t (giây), 1–600.
- **Thao tác:** Điểm (quả cầu sáng ở ô không khí trước mặt khối đang nhìn), Tia (tia laser, bán kính chùm 1 cm, cường độ = P / 3,14×10⁻⁴ m², chiếu suốt t giây theo hướng lúc thi triển).
- **Chi phí:** P·t (đổi ra mana).
- **Hiệu ứng theo bước sóng** (áp dụng mỗi giây, trong 8 khối, gồm cả người chơi):
  - **380–750 nm, nhìn thấy:** phát sáng (Điểm). Mức sáng = 6 + 4·log₁₀(Φ), làm tròn, kẹp trong 0–15 (100 lm ≈ cây đuốc, mức 14). Màu hạt hiệu ứng theo bước sóng. Laser nhìn thấy, công suất ≥ 1 W, chiếu trúng sinh vật: mù tạm thời với người chơi, mất dấu mục tiêu với mob.
  - **Dưới 400 nm, tia cực tím:** undead ở nơi I ≥ 10 W/m² bốc cháy như dưới nắng.
  - **Dưới 280 nm, UV-C:** mọi sinh vật ở nơi I ≥ 10 W/m² mất 1 máu/giây.
  - **Trên 750 nm, hồng ngoại:** vô hình, chỉ tỏa nhiệt. I ≥ 2,5 kW/m² gây đau 1 máu/giây; I ≥ 12,5 kW/m² làm khối dễ cháy bắt lửa và sinh vật bốc cháy.
- **Thất bại:** chọn ngoài vùng nhìn thấy mà mong có ánh sáng thì sẽ không có gì sáng. Đây không phải lỗi game mà là vật lý. Mana vẫn mất.
- **Phản tác dụng:** người chơi đứng ở chỗ có I vượt ngưỡng thì chịu như sinh vật khác. Riêng UV-C ≥ 1 W/m² tại chỗ người chơi gây mù tạm thời (bỏng giác mạc, như "mù tuyết").
- **Ví dụ:** 555 nm, 1 W → V ≈ 1,01 (công thức xấp xỉ) → Φ ≈ 690 lm → mức sáng 15, chạy 60 giây tốn **0,006 mana**: ánh sáng hiệu suất cao rất rẻ. 450 nm (xanh dương), 1 W → V ≈ 0,034 → Φ ≈ 23 lm → mức 11. Đèn UV diệt undead bán kính 5 m: P = 10 × 4π × 25 ≈ 3142 W; 60 giây → **18,9 mana**.

### 8.7 HÓA — Đốt Nhiên Liệu

- **Vật lý:** phản ứng cháy tỏa nhiệt, cần oxy (quy tắc Thornton: 13,1 MJ nhiệt trên mỗi kg O₂). Thiếu oxy thì cháy không hết và sinh ra khí CO. Đây là "lửa từ phản ứng hóa học".
- **Thông số:** loại nhiên liệu (chọn trong các nhiên liệu vanilla, trừ xô dung nham); số lượng n, 1–16.
- **Thao tác:** Điểm, Tia (súng phun lửa), Đường, Cầu, Tường, Đạn (bom xăng).
- **Tính:** năng lượng một vật phẩm = (thời gian cháy trong lò của vanilla / 1600) × 8,2 MJ. Than = 8,2 MJ, ván gỗ = 1,54 MJ, que = 0,51 MJ. Tổng = n × năng lượng một vật phẩm. Năng lượng này **thay cho mana** và được áp dụng y như Gia Nhiệt (cùng bậc kết quả, cùng phản tác dụng) qua thao tác đã chọn. Phí kích hoạt 1 mana. Nhiên liệu bị trừ khỏi túi đồ lúc thi triển; không đủ thì thất bại với báo "thiếu nhiên liệu".
- **Oxy:** O₂ cần = tổng năng lượng / 13,1 MJ/kg. Game đếm số ô không khí liên thông quanh điểm tác động (tối đa 64). Từ 64 ô trở lên coi như ngoài trời thoáng. Nếu số ô × 0,276 kg nhỏ hơn O₂ cần:
  - Năng lượng tỏa ra giảm theo tỉ lệ O₂ có / O₂ cần.
  - Sinh ra CO: mọi sinh vật trong vùng không khí kín đó bị buồn nôn và trúng độc trong 10 giây.
- **Phản tác dụng:** đốt trong hang kín mà người chơi cũng đứng trong vùng đó thì tự trúng độc CO. Đây là bài học an toàn có thật ngoài đời.
- **Ví dụ:** 1 than = 8,2 MJ (tương đương 820 mana nhiệt), cần 0,626 kg O₂ ≈ 2,3 m³ không khí, tức khoảng 3 ô.

### 8.8 HẠT NHÂN — Bức Xạ Neutron

- **Vật lý:** liều hấp thụ D (Gy) = năng lượng hấp thụ / khối lượng. Chỉ một lượng năng lượng rất nhỏ cũng gây hại lớn, nhưng tạo ra neutron thì rất tốn năng lượng (hiệu suất 2×10⁻⁴). Nước là vật chắn neutron tốt nhất.
- **Thông số:** năng lượng phát E (mana), 0–1000.
- **Thao tác:** Điểm, Tia, Cầu.
- **Tính:** năng lượng hấp thụ mỗi đơn vị = E·10 kJ × 2×10⁻⁴ (chia theo luật thao tác). Đi qua mỗi khối rắn giữa người chơi và mục tiêu: × 0,5. Đi qua mỗi khối nước: × 0,1. Liều lần này = năng lượng hấp thụ / khối lượng sinh vật, **cộng dồn** vào liều hiện có của sinh vật (6.8).
- **Hiệu ứng theo liều cộng dồn hiện tại** (theo luật miễn nhiễm của vanilla):

| Liều | Hiệu ứng |
|---|---|
| < 0,5 Gy | Không có gì |
| 0,5–2 Gy | Buồn nôn 10 giây + yếu ớt |
| 2–6 Gy | Trúng độc 10 giây + yếu ớt + chậm chạp |
| 6–10 Gy | Khô héo (wither) 10 giây |
| ≥ 10 Gy | Mất 50% máu tối đa + khô héo |

- Chỉ tác động lên sinh vật, không tác động lên khối, không để lại phóng xạ tồn dư.
- **Phản tác dụng (tán xạ):** mọi sinh vật trong 3 khối quanh điểm tác động (kể cả người chơi) nhận thêm liều = 5% năng lượng hấp thụ của mục tiêu chia cho khối lượng của chúng. Muốn an toàn thì đứng xa, hoặc đặt nước chắn giữa.
- **Ví dụ:** zombie 70 kg, muốn 6 Gy → cần hấp thụ 420 J → E = 420 / (10⁴ × 2×10⁻⁴) = **210 mana**.

### 8.9 HẤP DẪN — Nâng

- **Vật lý:** thế năng hấp dẫn E = m·g·h. Vật rơi từ độ cao h va chạm với năng lượng m·g·h.
- **Thông số:** độ cao h (m), 0,5–30; thời gian lơ lửng t (giây), 1–10.
- **Thao tác:** Điểm, Cầu, Bản thân.
- **Tác động lên:** sinh vật (khối lượng ở 7.3) và khối (khối lượng cả khối = ρ × 1 m³). Không nâng được đá nền, khối không phá được và khối có dữ liệu riêng như rương, lò.
- **Chi phí:** tổng m·g·h của mọi thứ được nâng (đổi ra mana).
- **Hiệu ứng:** mục tiêu bay lên h mét, lơ lửng t giây rồi được thả ra. Sinh vật rơi theo vật lý vanilla (có sát thương rơi). Khối thành khối rơi; nếu rơi trúng sinh vật thì gây sát thương = m·g·h_rơi / 1 kJ (máu), sau đó đặt lại thành khối.
- **Phản tác dụng:** nâng khối lên trên đầu mình thì nó rơi trúng mình. Tự nâng mình quá cao thì chịu sát thương rơi.
- **Ví dụ:** khối đá 2700 kg lên 5 m → 132,4 kJ → **13,2 mana**. Zombie 70 kg lên 10 m → 6,9 kJ → **0,69 mana**.

## 9. Gợi ý kỹ thuật (builder được tự quyết)

- Tách **lõi vật lý** thành Java thuần, không phụ thuộc Minecraft. Mỗi phép có hàm tính kết quả từ (thông số, dữ liệu mục tiêu, môi trường) → (bậc kết quả, các con số, phản tác dụng). Viết **JUnit test** dùng đúng các ví dụ ở mục 8.
- Phần Minecraft chỉ lo đọc thế giới, gọi lõi vật lý, rồi áp dụng kết quả.
- Phép và thao tác đều là lớp đăng ký vào danh sách, tự khai báo thông số (tên, đơn vị, min, max) và tương thích với nhau, để sách tự vẽ ô nhập và Pháp Trượng tự hiển thị.
- Gợi ý API NeoForge 1.21.1: data component cho ô phép, data attachment cho mana, packet cho khắc phép / đổi ô / đồng bộ mana.

## 10. Để builder tự quyết

- Tên lớp, cấu trúc package, mod id. Tên hiển thị: "Pháp Thuật Vật Lý".
- Công thức chế tạo Sách, Pháp Trượng, Máy Đo: rẻ, làm được đầu game, không cần đi Nether.
- Texture pixel art 16×16 đơn giản, tự vẽ bằng script.
- Bố cục giao diện sách và HUD, miễn đủ nội dung ở mục 6.
- Hệ số hất văng, hệ số k của Từ Trường, hiệu ứng hạt, âm thanh.
- Thứ tự làm các phép (gợi ý: nền móng + Gia Nhiệt trước, vì nhiều phép khác dùng lại luật của nó).
- Thử trong game bằng `runClient`. Chủ dự án sẽ tự chơi thử để đánh giá cuối cùng.

## 11. Hoàn thành khi

- `g.cmd build` thành công, mọi unit test của lõi vật lý pass.
- Nhiệt độ và liều phóng xạ được lưu, nguội dần đúng chu kỳ bán rã, và còn nguyên sau khi thoát game rồi vào lại.
- Trong game: chế tạo được Sách, Pháp Trượng, Máy Đo; khắc được phép + thao tác vào trượng; thi triển được cả 9 phép với mọi thao tác chúng cho phép; mỗi phép có đủ thành công, thất bại, phản tác dụng như mô tả và báo kết quả đúng số liệu.
- Có hướng dẫn ngắn bằng tiếng Việt cho chủ dự án: cách mở game có mod (`runClient`) và cách bỏ file mod vào một bản 1.21.1 NeoForge trên CurseForge.
