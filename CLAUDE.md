# Pháp Thuật Vật Lý — định nghĩa để làm tiếp

Mod Minecraft 1.21.1 / NeoForge 21.1.265 / Java 21: mỗi phép là một công thức vật lý thật. Chủ dự án nói tiếng Việt, code C++/Python nhưng chưa dùng Java, nên Claude viết toàn bộ Java. Hãy giải thích bằng tiếng Việt, so sánh với C++/Python khi cần.

## Tài liệu gốc

- Spec (nguồn chuẩn, thắng kế hoạch khi mâu thuẫn): `docs/superpowers/specs/2026-11-02-he-phep-nang-luong-design.md`
- Kế hoạch 1 (đã xong): `docs/superpowers/plans/2026-11-02-ke-hoach-1-nen-mong-va-nhiet.md`. Các quyết định và lỗi nhỏ để lại: `docs/superpowers/plans/2026-11-02-ke-hoach-1-quyet-dinh.md`
- Kế hoạch 2 (đang làm): `docs/superpowers/plans/2026-11-02-ke-hoach-2-bay-phep-con-lai.md`
- Hướng dẫn cho chủ dự án: `HUONG_DAN.md`

## Ràng buộc bắt buộc

- Mọi thứ nằm trong `D:\code_base\New folder`. JDK, cache Gradle và CodeGraph ở `tools\` (git bỏ qua). Không cài gì ra ngoài.
- Build **chỉ** qua `mod\g.cmd` (luôn `--offline`). **Không tự chạy `runClient`**, vì game có thể gọi máy chủ Mojang. Chủ dự án tự chơi thử.
- Không dùng email hay domain của chủ dự án trong code. Package gốc là `phapthuat`.
- Chữ trong game bằng tiếng Việt có dấu. Chỉ sửa `mod/src/main/resources/assets/phapthuat/lang/vi_vn.json`; `en_us.json` do Gradle chép từ đó lúc build. Chuỗi đi qua `I18n.get` (sách) dùng `String.format`, nên ký tự `%` phải viết `%%`.
- Số hiển thị dùng dấu phẩy thập phân kiểu Việt qua `spell/Fmt`.
- Mọi số liệu nằm ở một chỗ: `PhysicsConstants`, `Materials`, `EntityMass`, `IronContent` (vật lý) và `Config` (cân bằng). Sách đọc số từ đó. Sách **không in đáp số** của ví dụ; kết quả chỉ báo sau khi thi triển.
- Commit message viết tiếng Việt. Chỉ push khi chủ dự án bảo.

## Lệnh

```powershell
& "D:\code_base\New folder\mod\g.cmd" compileJava compileTestJava   # biên dịch
& "D:\code_base\New folder\mod\g.cmd" test                          # JUnit (lõi vật lý)
& "D:\code_base\New folder\mod\g.cmd" runGameTestServer             # GameTest trong thế giới
& "D:\code_base\New folder\mod\g.cmd" build                         # ra mod/build/libs/phapthuat-1.0.0.jar
```

## Kiến trúc (`mod/src/main/java/phapthuat`)

- `physics/`: Java thuần, **không import `net.minecraft`**, kiểm bằng JUnit với ví dụ số của spec. Mỗi phép có một lớp `*Calc`.
- `spell/`: khung phép.
  - `Spell` là interface. Có `shapeParams(shape)` để phép đổi khoảng thông số của thao tác, `allParams(shape)`, và `upfrontCostMana(spell, level, units)` cho chi phí phụ thuộc mục tiêu.
  - `SpellParam` là thông số số hoặc lựa chọn (`SpellParam.choice`, giá trị là chỉ số).
  - `SpellCaster`: chọn mục tiêu → trừ mana → `apply`. Riêng Đạn thì trừ lúc bắn, kèm ½·m·v².
  - Các lớp còn lại: `Shape`, `InscribedSpell`, `WandSlots`, `Targeting`, `TimedEffects`, `InscribeValidator` (không tin dữ liệu từ client). Codec nằm ở `SpellCodecs`, để các record còn lại là Java thuần.
- `spell/impl/`: từng phép. `HeatSpell.applyHeat` và `affectsHeat` được dùng lại cho các phép sinh nhiệt (Hồ Quang, Đốt Nhiên Liệu, Phát Quang).
- `thermal/`:
  - `ThermalStore` lưu nhiệt theo khối (SavedData), nguội lười theo tick.
  - `EntityThermal` lưu lõi nhiệt và liều phóng xạ của sinh vật.
  - `WorldTemps`: nhiệt độ môi trường, nhiệt độ tự nhiên, và luật oxy `inWater` (Máy Đo dùng chung).
  - `ThermalTicker`: dọn bảng nhiệt, hiệu ứng lạnh, xóa nhiệt khi đặt khối, dọn khối sáng `PlacedLights`.
- **Quy ước tiến độ chuyển pha** (`BlockThermal.phaseProgressJ`, cũng là input/output của `HeatCalc`/`SiphonCalc`): **dương** là đã hấp thụ khi đun (đang chảy/sôi dở), **âm** là đã rút khi làm lạnh (đang đông dở; −m·L là đông hẳn).
- `client/`: HUD, sách (`GrimoireScreen`, `GrimoireText`), Shift + lăn chuột. Chỉ nạp ở client; code chung gọi qua `ClientHooks` sau `isClientSide()`.
- Đăng ký phép trong constructor `PhapThuat` bằng `SpellRegistry.register(...)`.

## Mẹo GameTest (đã mất công tìm ra)

- Khuôn `empty_7x7x7`: **nền đá nằm ở tầng y = 1**, vì GameTest dọn tầng y = 0 thành không khí. Khối mục tiêu `(3, 1, 3)` nằm trong nền.
- Người chơi giả: dùng `TargetingGameTests.playerAt` (đã đặt `yHeadRot`; hướng nhìn lấy theo đầu). Nó ở **chế độ sáng tạo** nên không mất máu: thử sát thương bằng mob (lợn, zombie).
- `helper.placeAt` lấy vật phẩm từ **tay chính** của người chơi, không lấy từ stack truyền vào.
- Biome của GameTest có T_mt = 20°C. Người chơi giả bắt đầu với 1000 mana.

## Trạng thái hiện tại (2026-11-03)

- Nhánh `phep-nen-tang`. Kế hoạch 1 đã xong và qua review độc lập (lúc đó 57 unit test và 36 GameTest đều xanh).
- **Kế hoạch 2: Task 1–3 đã commit**: khung mở rộng, lõi vật lý Động, Hấp dẫn, Điện từ, Quang, Hóa, Hạt nhân. **Test của kế hoạch 2 chưa chạy lần nào.** Chủ dự án muốn viết hết rồi test **một lần** ở cuối (Task 11); mỗi task chỉ chạy `compileJava compileTestJava`.
- **Làm tiếp từ Task 4 (Xung Lực)**, rồi tới Task 11. Sổ theo dõi nằm ở máy (git bỏ qua): `.superpowers/sdd/2026-11-02-ke-hoach-2-bay-phep-con-lai/progress.md`. Đọc nó trước khi làm: task có dòng `complete` là đã xong, các dòng `Ruling:` là quyết định đã chốt.
- Quyết định đã chốt trước cho các task còn lại:
  - Nâng tính chi phí theo khối lượng mục tiêu qua `Spell.upfrontCostMana(spell, level, units)` (đã có sẵn).
  - Liều ≥ 10 Gy: mất 50% máu áp **lúc thi triển**, khi liều vừa vượt ngưỡng.
  - Thêm cuộn cho cột mục lục của sách ở Task 11 (9 phép sẽ tràn màn hình).
- Sau kế hoạch 2: cùng chủ dự án thiết kế **phép kết hợp** từ các phép cơ bản. Bàn ý tưởng trước, chưa code ngay.

## Công cụ

- CodeGraph MCP (`tools\codegraph`, `.mcp.json`, telemetry tắt; chỉ mục ở `.codegraph/`, tự đồng bộ): dùng `codegraph_explore` và `codegraph_search` để tra code thay vì đọc lại nhiều file.
