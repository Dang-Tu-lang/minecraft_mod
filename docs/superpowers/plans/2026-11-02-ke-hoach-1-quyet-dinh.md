# Kế hoạch 1: các quyết định đã tự đưa ra và lỗi nhỏ để lại

Ghi lại từ sổ theo dõi khi thực hiện `2026-11-02-ke-hoach-1-nen-mong-va-nhiet.md`. Mỗi dòng: quyết định — lý do — cái giá nếu sai.

## Quyết định

- Setup: Ruling: làm trên nhánh `phep-nen-tang` ngay trong thư mục thay vì git worktree — g.cmd trỏ tới ..\tools theo đường dẫn tương đối và chủ dự án muốn mọi thứ trong một thư mục — cost if wrong: không đáng kể, merge về main lúc cuối.
- Setup: Ruling: git user.name repo-local = "dangyuuki" (lấy từ email global, global không có name) — cost if wrong: đổi tên tác giả commit sau.
- Pre-flight: T7 vs T9 ModDataComponents.WAND_SLOTS: T7 "đăng ký sẵn chỗ" cho kiểu WandSlots chỉ có ở T9 — Ruling: đăng ký WAND_SLOTS ở T9 (T7 chỉ tạo file ModDataComponents rỗng) — cost if wrong: không có.
- Task 2: Ruling: package gốc đổi từ `dev.oasm.phapthuat` thành `phapthuat` (cả mod_group_id) — chủ dự án yêu cầu, đây là mod cá nhân — cost if wrong: không có; cả hai kế hoạch đã được sed theo.
- Task 2: Ruling: displayName ghi thẳng "Pháp Thuật Vật Lý" trong neoforge.mods.toml thay vì ${mod_name} — Gradle đọc gradle.properties theo ISO-8859-1 làm vỡ chữ tiếng Việt — cost if wrong: phải sửa tên ở hai chỗ nếu đổi tên mod.
- Task 2: Ruling: PhysicsConstants chỉ chứa hằng số mà kế hoạch 1 dùng; phần còn lại thêm ở kế hoạch 2 Task 1 như kế hoạch 2 đã ghi — cost if wrong: không có.
- Task 4: Ruling: nước đông thành băng lấy điểm nóng chảy và L_nc từ Materials.ICE (SiphonCalc nhận diện WATER), không thêm melt vào WATER — vì HeatCalc sẽ đòi nhiệt nóng chảy khi đun nước — cost if wrong: thêm chất lỏng khác thì phải thêm ánh xạ.
- Task 5: Ruling: Tường căn giữa theo cả chiều cao (dy từ −(h−1)/2 tới h/2) — câu chữ kế hoạch mơ hồ, căn giữa là cách tự nhiên nhất khi tâm là ô trước mặt khối — cost if wrong: đổi một vòng lặp.
- Task 6: Ruling: ManaSyncPayload gửi cả mana tối đa (mana, max) thay vì chỉ mana — HUD cần max khi config đổi — cost if wrong: không có.
- Task 6: Ruling: tạo Fmt ngay ở Task 6 (kế hoạch đặt ở Task 9) — HUD mana cần định dạng số kiểu Việt — cost if wrong: không có, cùng đường dẫn và chữ ký.
- Task 7: Ruling: en_us.json được Gradle sao từ vi_vn.json lúc processResources thay vì viết tay hai file — đảm bảo luôn cùng nội dung — cost if wrong: không có.
- Task 7: Ruling: bước kiểm tay tab sáng tạo thay bằng chạy runClient và quét log khởi động (0 lỗi dính phapthuat) — mình không thao tác được giao diện game; chủ dự án sẽ nhìn tận mắt ở Task 14 — cost if wrong: lỗi hiển thị phát hiện muộn.
- Task 8: Ruling: nguội dần tính lười theo tick (ThermalTable.get/put nhận nowTick, sweep dọn mục đã ổn định; EntityThermal có updatedAtTick) thay cho decay(dt) mỗi 20 tick — chính xác tới từng tick, GameTest không lệch nhịp — cost if wrong: không có, hành vi như spec.
- Task 8: Ruling: BlockThermal thêm materialId — cần để bỏ mục khi khối bị thay bằng vật liệu khác (spec 6.8) — cost if wrong: không có.
- Task 8: Ruling: không có lớp ModGameTests; NeoForge tự quét @GameTestHolder — đăng ký thêm sẽ trùng — cost if wrong: không có.
- Task 8: Ruling: template GameTest tự sinh (scripts/GenEmptyStructure.java → data/phapthuat/structure/empty_7x7x7.nbt) vì NeoForge 21.1.265 không có EmptyTemplate — cost if wrong: không có.
- Task 8: Ruling: Biome#getTemperature(BlockPos) là private → mở bằng META-INF/accesstransformer.cfg — cost if wrong: phải sửa AT khi nâng phiên bản MC.
- Task 9: Ruling: g.cmd luôn chạy Gradle với --offline — chủ dự án yêu cầu build không lên mạng; mọi thư viện đã có trong cache — cost if wrong: thêm thư viện mới phải tạm bỏ --offline một lần.
- Task 9: Ruling: Spell.affects nhận thêm Shape (ô không khí chỉ tính với Cầu/Tường) — kế hoạch Task 10 cần điều này — cost if wrong: không có.
- Task 9: Ruling: CastContext và Targeting.Target có thêm mặt khối (face) — Gia Nhiệt đặt lửa cạnh mặt đang nhìn — cost if wrong: không có.
- Task 9: Ruling: codec của Shape/InscribedSpell/WandSlots tách sang SpellCodecs — classpath unit test không có DFU, các record phải là Java thuần — cost if wrong: không có.
- Task 9: Ruling: thêm SpellCaster.applyOnUnits (không trả mana) cho đạn phép đã trả lúc bắn — cost if wrong: không có.
- Task 9: Ruling: thao tác Đường bỏ ô người chơi đang đứng — tránh tự đốt chân mỗi lần dùng — cost if wrong: mất một ô đầu vệt.
- Task 9: Ruling: GameTest đạn rơi trúng người bắn dùng 6 m/s thay vì 5 — với 5 m/s đạn quay lại hitbox ở tick ~19, sát mốc miễn nhiễm 20 tick, test sẽ chập chờn — cost if wrong: không có.
- Task 9: Ruling: dùng NoopRenderer của vanilla thay vì tự viết SpellProjectileRenderer — đạn chỉ hiện bằng hạt lửa — cost if wrong: không có.
- Task 9: Ruling: GameTest nhìn trời dọn cột rào chắn phía trên (GameTest tự dựng barrier quanh vùng thử) — cost if wrong: không có.
- Task 10: Ruling: tntPrimed dùng 42 mana thay vì 25 — lõi TNT 1,65 kg × 1130 × 220 K = 410 kJ ≈ 41,0 mana theo bảng 7.2; 25 mana không đủ — cost if wrong: không có.
- Task 10: Ruling: Gia Nhiệt tác động mọi khối không phải không khí (kế hoạch ghi "khối có trong MaterialLookup") — spec 8.1 có hàng "Khối không cháy khác: ngọn lửa mana", nếu lọc theo bảng thì hàng đó không bao giờ xảy ra; dung nham/magma/lửa/lửa trại cũng xếp vào nhóm này — cost if wrong: với Cầu/Tường, khối đất cũng chia năng lượng (đúng tinh thần "trải mỏng").
- Task 10: Ruling: phản tác dụng chỉ áp cho vật cháy được và sinh vật — spec 8.1 "Khối không cháy được và không khí không có luật này"; HeatCalc vẫn tính backlash cho mọi vật (thủy tinh 3× ngưỡng nứt chỉ 30 mana) — cost if wrong: không có.
- Task 10: Ruling: "vùng" của khí nóng = các ô khí + mọi khối áp sát; kính trong vùng vỡ, sinh vật trong vùng bị hất — spec chỉ ghi "trong vùng"; test airBurstBreaksGlass đặt kính sát mép cầu — cost if wrong: đổi một vòng lặp.
- Task 10: Ruling: TNT nóng tới 240°C vẫn nổ cả khi ở trong nước (luật oxy chỉ chặn lửa, nến, lửa trại, sinh vật bốc cháy) — TNT tự mang chất oxy hóa — cost if wrong: thêm một điều kiện.
- Task 10: Ruling: khối bị bít kín 6 mặt đạt nhiệt bắt cháy thì báo "thiếu oxy", không có lửa — không có ô để đặt lửa — cost if wrong: không có.
- Task 10: Ruling: vụ nổ hơi nước dùng nguồn null — level.explode loại trừ thực thể nguồn, mà spec bắt người thi triển chịu như sinh vật khác — cost if wrong: mất ghi công giết.
- Task 10: Ruling: test waterVaporizes/steamBomb kiểm khối nền cạnh mục tiêu (TARGET.north()) thay vì TARGET.below() — GameTest dọn tầng y=0 thành không khí, nền đá của khuôn nằm ở y=1; bản cũ khiến steamBomb luôn qua — cost if wrong: không có.
- Task 10: Ruling: thêm Material.latentAt (chuyển từ MeterReadout) để dòng báo thất bại hiện % chuyển pha — cost if wrong: không có.
- Task 11: Ruling: "nước" của Thu Nhiệt chỉ là khối nước nguồn (không gồm khối ngậm nước, bùn, bọt biển ướt dù MaterialLookup coi là WATER) — kế hoạch ghi "nước nguồn", và nước → băng chỉ có nghĩa với khối nước — cost if wrong: thêm ánh xạ khối.
- Task 11: Ruling: mana được/tốn của mọi đơn vị cộng thành một con số rồi thanh toán một lần (hệ số chỉ nhân phần dương của từng đơn vị) — kế hoạch ghi "tổng manaJoulesDelta" — cost if wrong: không có.
- Task 11: Ruling: phản tác dụng rút phần công còn thiếu khỏi lõi người thi triển bằng SiphonCalc rồi đặt đóng băng tối đa; mất máu "tỉ lệ" đến từ hiệu ứng lạnh theo bậc mỗi giây (lõi càng lạnh càng lâu về) — spec không cho công thức máu — cost if wrong: chỉnh bậc.
- Task 11: Ruling: hiệu ứng lạnh bỏ qua sinh vật có canFreeze() = false (vanilla: lạc đà băng, gấu bắc cực, người tuyết, người chơi sáng tạo) — giống tuyết bột vanilla — cost if wrong: một điều kiện.
- Task 11: Ruling: thêm GameTest coldCoreSlowsAndHurtsPig cho phần hiệu ứng lạnh của ThermalTicker (kế hoạch không có test cho phần này); người chơi giả của GameTest ở chế độ sáng tạo nên thử trên lợn — cost if wrong: không có.
- Task 11: Ruling: lavaPartial đặt mana 500 trước (mana đầy 1000 sẽ che mất phần nhận) và so lần 2 ≤ lần 1 với sai số 1e-6 — hai lần đều bắt đầu ở 1200°C nên η bằng nhau (spec: T_đầu là nhiệt độ lúc bắt đầu lần rút) — cost if wrong: không có.
- Task 12: Ruling: thêm GameTest WandGameTests (kế hoạch Task 12 không có test) cho phần server: thi triển ô đang chọn + hồi chiêu, ô trống không tốn mana, đổi ô và kẹp delta gói tin về ±1, phí phóng đạn — cost if wrong: không có.
- Task 12: Ruling: SpellCaster.cast trừ thêm ½·0,1·v² J khi thi triển Đạn (spec 6.5; lấp chỗ thiếu của Task 9) — test projectileChargesKineticEnergy RED→GREEN — cost if wrong: không có.
- Task 12: Ruling: TargetingGameTests.playerAt đặt thêm yHeadRot — hướng nhìn của sinh vật lấy theo đầu, moveTo chỉ xoay thân; trước đây mọi người chơi giả đều nhìn về hướng nam — cost if wrong: không có.
- Task 12: Ruling: bước thử tay runClient thay bằng mở client một lần và quét log khởi động (0 dòng ERROR) — mình không thao tác được giao diện; từ nay KHÔNG tự mở runClient vì chủ dự án yêu cầu chỉ build offline (game có thể tự gọi máy chủ Mojang); chủ dự án chơi thử ở Task 14 — cost if wrong: lỗi giao diện (HUD, lăn chuột) phát hiện muộn.
- Task 12: Ruling: dòng HUD của trượng đặt ở guiHeight − 76, trên dòng chữ của thanh mana — kế hoạch chỉ ghi "phía trên hotbar" — cost if wrong: chỉnh một hằng số sau khi chơi thử.
- Task 13: Ruling: InscribeValidator có thêm inscribe(player, slot, raw) gom kiểm tay phụ + ô 0..4 + validate + ghi + báo — để GameTest gọi thẳng phần server; thêm 2 test (thiếu thông số thao tác lấy mặc định; NaN/phép lạ/ô sai bị từ chối) cho Review Focus 1 — cost if wrong: không có.
- Task 13: Ruling: InscribePayload dùng codec riêng giới hạn id ≤ 64 ký tự và ≤ 16 thông số — gói tin giả mạo không đẩy được dữ liệu lớn — cost if wrong: không có.
- Task 13: Ruling: ô nhập ngoài [min, max] cũng tô đỏ và khóa nút khắc (kế hoạch chỉ ghi "sai định dạng"); server vẫn kẹp — người chơi không bị kẹp âm thầm — cost if wrong: nới lại một điều kiện.
- Task 13: Ruling: lời văn trang phép nhắc hằng số vật lý bằng ký hiệu (ρ_khí, c_v, P₀, ngưỡng vỡ kính...) và trỏ sang trang Hằng số, thay vì chép số vào file ngôn ngữ — giữ "mọi số liệu ở một chỗ"; luật trò chơi (3 lần, 3 giây, 2/5/9 K) vẫn ghi bằng chữ — cost if wrong: đổi luật thì sửa cả file ngôn ngữ.
- Task 13: Ruling: bỏ bước runClient thử tay (chủ dự án yêu cầu chỉ build offline, không tự mở game); giao diện sách được kiểm bằng build + GameTest phần server, chủ dự án thử tận mắt ở Task 14 — cost if wrong: lỗi bố cục phát hiện muộn.
- Task 14: Ruling: bước 3 (chơi thử sinh tồn bằng runClient) giao cho chủ dự án — chủ dự án yêu cầu chỉ build offline, mình không tự mở game; HUONG_DAN.md có danh sách thử nhanh đúng các ví dụ của bước 3 — cost if wrong: lỗi chỉ thấy khi chơi được phát hiện ở lần chơi thử của chủ dự án.
- Task 14: Ruling: HUONG_DAN.md có kèm số ví dụ của spec (33,4; 363...) — tài liệu cho chủ dự án kiểm tra, không phải nội dung trong game, nên luật "sách không in đáp số" không áp dụng — cost if wrong: xóa một mục.
- Final: Ruling: công thức đơn giản hóa (η theo T_đầu, COP theo T_cuối, nứt theo ΔT một lần) — spec quy định — giữ nguyên — cost if wrong: không có.
- Final: Ruling: nứt vỡ chặn đường nấu chảy nhiều lần của băng/đá — đúng luật "bậc cao nhất đạt tới" của spec — giữ nguyên — cost if wrong: không có.
- Final: Ruling: nhiều người chơi (bảo vệ spawn, đồng bộ config cho sách, giới hạn tần suất gói tin, khắc không cần cầm sách) — mod cá nhân chơi đơn, spec §3 loại cân bằng nhiều người chơi — giữ nguyên — cost if wrong: phải làm khi mở server cho người khác.
- Final: Ruling: bố cục HUD/sách và cột mục lục chưa cuộn — chủ dự án chơi thử sẽ chỉnh; cuộn mục lục làm ở kế hoạch 2 Task 11 — cost if wrong: không có.
- Final: Ruling: giao diện client chưa từng chạy thật (không runClient theo yêu cầu offline) — chủ dự án chơi thử — cost if wrong: lỗi giao diện phát hiện muộn.
- Final: Ruling: công cụ phát triển (script texture, sinh khuôn thử), lớp GameTest/ProbeSpell nằm trong jar chính — bố cục MDK chuẩn, ProbeSpell chỉ đăng ký trong test — giữ nguyên — cost if wrong: không có.
- Final: Ruling: đạn chạm nước thì tắt thay vì áp như Điểm — kế hoạch ghi rõ — giữ nguyên — cost if wrong: không có.
- Final: Ruling: trượng tạo bằng /give với thao tác lạ hay bảng thông số lớn — người dùng lệnh là người được tin, thông số vẫn kẹp khi đọc — giữ nguyên — cost if wrong: không có.
- Final: Ruling: không có bản tiếng Anh, thiếu hằng số kế hoạch 2, HUD mana làm tròn 1 chữ số — spec/kế hoạch quy định — giữ nguyên — cost if wrong: không có.
- Final: Ruling: đạn bị dòng nước đẩy / bay quá nhanh lọt lớp nước mỏng; làm đông nước ở biome dưới 0°C không tốn; nhắm Điểm vào thuyền vẫn mất E — trường hợp biên hoặc đúng luật spec — giữ nguyên — cost if wrong: không có.

## Lỗi đã sửa sau review

- mana vô hạn từ dung nham (rút đúng m·L lặp lại) — SiphonCalcTest.lavaExactLatentThenCools, ThermalTableTest.solidifiedLavaIsNotSettled, GameTest lavaExactLatentThenCools RED→GREEN, suite 57/57 + 36/36
- tiến độ chuyển pha của nước bị hiểu hai nghĩa — quy ước dấu (dương = đã hấp thụ khi đun, âm = đã rút khi làm lạnh); HeatCalcTest.frozenProgressMustRemeltFirst, SiphonCalcTest.boilingProgressReleasedFirst RED→GREEN (sửa kỳ vọng lavaPartialLatentFirst/lavaYieldsLessAsItCools theo quy ước mới), suite 57/57 + 36/36
- Máy Đo "trong nước" khác luật oxy — WorldTemps.inWater dùng chung; GameTest meterInWaterMatchesOxygenRule RED→GREEN, suite 57/57 + 36/36
- bán kính Cầu lẻ bị làm tròn — ShapeMath.sphere(double); ShapeMathTest.fractionalRadius RED→GREEN, suite 57/57 + 36/36
- Thu Nhiệt lên mob miễn lửa — GameTest siphonIgnoresFireImmune RED→GREEN, suite 57/57 + 36/36
- khối đặt lại thừa hưởng nhiệt cũ — lắng nghe BlockEvent.EntityPlaceEvent; GameTest placingBlockClearsStoredHeat RED→GREEN, suite 57/57 + 36/36
- Thu Nhiệt crash/làm lạnh sai khi khối đổi giữa chừng — kiểm lại mục tiêu lúc rút; GameTest siphonSkipsNonTargetsWithoutCrash RED→GREEN, suite 57/57 + 36/36

## Lỗi nhỏ để lại (chưa sửa)

- giáp đứng (armor stand) bị tính là sinh vật 37°C trong chia năng lượng và Thu Nhiệt.
- hạt hiệu ứng — Cầu r = 5 trên đất gửi ~10 000 gói hạt trong 1 giây; nên gộp theo lần thi triển.
- thiếu test: nhánh kẹp chiều cao thế giới, đổi vật liệu bỏ mục lưu, lưu/đọc ThermalStore qua NBT, ParamParser với -5/1e9/1e999/Infinity.
- sách ghi "dòng báo luôn ghi N" nhưng dòng một đơn vị không ghi N; Tường chỉ xét ô chân sinh vật thay vì "chạm vào".
- sách báo "Đã khắc…" trước khi server xác nhận; ClientManaCache không xóa khi thoát thế giới; gói đồng bộ mana mỗi tick khi đang hồi.
- Tường với rộng/cao lẻ vẫn làm tròn (Cầu đã sửa).
- ThermalStore.remove dùng nowTick = 0; SpellCaster.cast không kiểm lại thao tác có thuộc phép (trượng tạo bằng /give; thông số vẫn bị kẹp).
