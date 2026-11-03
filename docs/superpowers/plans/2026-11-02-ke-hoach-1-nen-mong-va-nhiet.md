# Kế hoạch 1: Nền móng + nguyên tố Nhiệt — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Dựng toàn bộ nền móng của mod "Pháp Thuật Vật Lý" (mana, Sách Công Thức, Pháp Trượng, Máy Đo, 7 thao tác, lưu nhiệt và nguội dần) cùng hai phép Nhiệt là Gia Nhiệt và Thu Nhiệt, ở mức chơi thử được trong game.

**Architecture:** Lõi vật lý là Java thuần (package `phapthuat.physics`, không import `net.minecraft`), kiểm bằng JUnit với đúng các ví dụ số của spec. Phần Minecraft (NeoForge 1.21.1) chỉ đọc thế giới, gọi lõi, rồi áp kết quả. Phép và thao tác là các lớp đăng ký vào `SpellRegistry`/`Shape`, tự khai báo thông số, để sách và trượng tự hiển thị. Hiệu ứng trong thế giới được kiểm bằng GameTest.

**Tech Stack:** Java 21, NeoForge 21.1.265 (ModDevGradle 2.0.148), Minecraft 1.21.1, JUnit 5, GameTest của Minecraft.

**Spec:** `docs/superpowers/specs/2026-11-02-he-phep-nang-luong-design.md`

**Phạm vi:** đây là kế hoạch 1/2. Kế hoạch 2 (viết sau khi chủ dự án chơi thử) làm 7 phép còn lại: Xung Lực, Hồ Quang, Từ Trường, Phát Quang, Đốt Nhiên Liệu, Bức Xạ Neutron, Nâng. Kế hoạch này làm spec mục 6, 7 (trừ lượng sắt ở 7.3), 8.1, 8.2, 9, 10, và phần mục 11 áp dụng cho hai phép Nhiệt.

## Global Constraints

- Minecraft **1.21.1**, NeoForge **21.1.265**, Java 21. Project tại `D:\code_base\New folder\mod`.
- Mọi thứ nằm trong `D:\code_base\New folder`. Không cài gì ra ngoài. Build bằng `D:\code_base\New folder\mod\g.cmd` (gọi bằng đường dẫn đầy đủ).
- Mod id `phapthuat`, package gốc `phapthuat`, tên hiển thị "Pháp Thuật Vật Lý".
- Toàn bộ chữ trong game bằng **tiếng Việt có dấu**. `vi_vn.json` và `en_us.json` có **cùng nội dung tiếng Việt**.
- **Không có máy tính hộ:** giao diện không bao giờ hiển thị trước chi phí cần thiết hay khả năng thành công. Kết quả chỉ báo **sau khi thi triển**. Sách in công thức và bảng dữ liệu, **không in đáp số** của các ví dụ trong spec.
- Mọi số liệu nằm ở một chỗ: `PhysicsConstants`, `Materials`, `EntityMass` (vật lý) và `Config` (cân bằng). Sách đọc từ chính chỗ đó.
- 1 mana = 10 kJ (config `joulesPerMana` = 10000), mana tối đa 1000, hồi 2 mana/giây; đều chỉnh được trong config.
- Lõi vật lý làm việc bằng **Joule và °C**. Chỉ tầng Minecraft mới đổi mana ↔ Joule.
- Số hiển thị kiểu Việt Nam: dấu phẩy thập phân (`Locale.forLanguageTag("vi-VN")`). Mana hiện 1 chữ số thập phân.
- Ô nhập số nhận cả `33,3` lẫn `33.3`.

## Review Focus

1. **Thông số xấu từ ô nhập hoặc packet** (chữ, rỗng, âm, `NaN`, `1e9`, dấu phẩy): phải bị từ chối hoặc kẹp về [min, max], không bao giờ crash hay ghi giá trị lạ vào trượng. Test: `ParamParserTest` (Task 5), `InscribeValidationTest` GameTest (Task 13).
2. **Nhìn vào khoảng không / ngoài giới hạn thế giới** khi thi triển: phải lấy ô không khí ở 16 khối, kẹp trong chiều cao thế giới, không crash. Test: `pointAtSkyGivesAirAt16` (Task 9).
3. **Bảng nhiệt phình to hoặc giữ mục cũ:** khối đã cháy/bị thay thì mục lưu phải bị bỏ; vượt 10 000 mục thì bỏ mục cũ nhất. Test: `ThermalTableTest` (Task 8).
4. **Mana biên:** không âm, không vượt tối đa khi Thu Nhiệt cho thêm, trả lỗi đúng khi thiếu đúng một chút. Test: `ManaPoolTest` (Task 6).
5. **Đạn phép không bao giờ chạm đất** (bắn lên trời, rơi khỏi thế giới): phải tự biến mất sau 10 giây; bắn thẳng lên thì rơi trúng người bắn. Test: `projectileDespawnsAfter10s`, `projectileFallsBackOnCaster` (Task 9).

---

### Task 1: Đổi tên project, thêm JUnit, chuẩn bị git

**Files:**
- Modify: `mod/gradle.properties` (mod_id, mod_name, mod_group_id, mod_license)
- Modify: `mod/build.gradle` (thêm JUnit)
- Modify: `mod/g.cmd` (luôn chạy từ thư mục của nó)
- Modify: `mod/src/main/templates/META-INF/neoforge.mods.toml` (description)
- Delete: `mod/src/main/java/com/example/` (cả thư mục)
- Create: `mod/src/main/java/phapthuat/PhapThuat.java`, `PhapThuatClient.java`, `Config.java`
- Create: `mod/src/test/java/phapthuat/SmokeTest.java`
- Create: `.gitignore` ở `D:\code_base\New folder` (bỏ qua `tools/`)
- Delete: `mod/build-first.log`

**Interfaces:**
- Produces: `PhapThuat.MODID = "phapthuat"`, `PhapThuat.LOGGER`, `PhapThuat(IEventBus modBus, ModContainer container)` là nơi mọi task sau đăng ký `DeferredRegister` và listener. `Config` giữ `ModConfigSpec` loại COMMON (Task 6 thêm giá trị).

- [ ] **Step 1:** Sửa `gradle.properties`: `mod_id=phapthuat`, `mod_name=Pháp Thuật Vật Lý`, `mod_group_id=phapthuat`, `mod_license=All Rights Reserved`. Sửa description trong `neoforge.mods.toml` thành `Phép thuật dựng trên vật lý thật.`
- [ ] **Step 2:** Xóa package `com.example.examplemod` và `assets/examplemod`. Tạo `PhapThuat` (`@Mod(PhapThuat.MODID)`, chưa đăng ký gì ngoài config), `PhapThuatClient` (`@Mod(value = MODID, dist = Dist.CLIENT)`, đăng ký `ConfigurationScreen` như template cũ), `Config` (builder rỗng + `SPEC`).
- [ ] **Step 3:** Trong `build.gradle` thêm:

```groovy
dependencies {
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
tasks.named('test', Test) { useJUnitPlatform() }
```

- [ ] **Step 4:** Sửa `g.cmd`: thêm `pushd "%~dp0"` trước lệnh `call` và `popd` sau, để chạy được từ bất kỳ thư mục nào.
- [ ] **Step 5:** Viết `SmokeTest.junitRuns()` với `assertEquals(2, 1 + 1)`.
- [ ] **Step 6:** Chạy `& "D:\code_base\New folder\mod\g.cmd" build`. Kỳ vọng: `BUILD SUCCESSFUL`, có dòng `> Task :test`, file `mod/build/libs/phapthuat-1.0.0.jar`.
- [ ] **Step 7 (chỉ khi chủ dự án đã cho phép dùng git):** `git init` tại `D:\code_base\New folder`, tạo `.gitignore` gốc chứa `tools/`. Commit: `chore: đổi tên project thành phapthuat, thêm JUnit`.

### Task 2: Hằng số và bảng vật liệu

**Files:**
- Create: `mod/src/main/java/phapthuat/physics/PhysicsConstants.java`
- Create: `.../physics/Material.java`, `.../physics/Materials.java`, `.../physics/Element.java`, `.../physics/Conversions.java`
- Test: `mod/src/test/java/phapthuat/physics/MaterialsTest.java`

**Interfaces:**
- Produces:
  - `PhysicsConstants`: `KELVIN_OFFSET = 273.15`, `G = 9.81`, `R = 8.314`, `P0 = 101_325`, `AIR_DENSITY = 1.2`, `AIR_CV = 718`, `CORE_VOLUME_M3 = 0.001`, `WATER_MOL_PER_KG = 55.51`, `STEAM_CP = 2000`, `GLASS_BREAK_PA = 7_000`, `BLAST_DAMAGE_PA = 35_000`, `TNT_REFERENCE_WORK_J = 400_000`, `MAX_EXPLOSION_POWER = 8`, `HOT_AIR_IGNITE_C = 300`, cùng các hằng số còn lại của spec 7.1 (để kế hoạch 2 dùng). `static double toKelvin(double c)`.
  - `record Material(String id, double density, double specificHeat, double ignitionC, double meltC, double latentFusion, double liquidSpecificHeat, double boilC, double latentVaporization, double youngsModulus, double thermalExpansion, double tensileStrength, double naturalTempC, double maxExtractableJ, boolean estimated)`. Đơn vị SI (J/(kg·K), J/kg, Pa, 1/K). Trường không áp dụng = `Double.NaN`. Các hàm: `boolean flammable()`, `boolean meltable()`, `boolean boils()`, `boolean brittle()`, `double coreMassKg()` (= density × 0,001), `double crackDeltaK()` (= tensileStrength / (youngsModulus × thermalExpansion), NaN nếu không giòn).
  - `Materials`: các hằng `WOOD, WOOL, LEAVES, HAY, COAL_BLOCK, WAX, TNT, WATER, ICE, SNOW, SAND, STONE, GLASS, OBSIDIAN, LAVA, MAGMA, FIRE, CAMPFIRE, AIR, LIVING_CORE` với đúng số ở spec 7.2 và 7.3 (băng và tuyết: `liquidSpecificHeat = 4186`, `boilC = 100`, `latentVaporization = 2_257_000`). `LAVA.naturalTempC = 1200`, `MAGMA.naturalTempC = 700`, `FIRE`/`CAMPFIRE.naturalTempC = 800`, `maxExtractableJ` lần lượt 20 000 và 200 000. `static List<Material> all()`.
  - `enum Element { NHIET, DONG, DIEN_TU, QUANG, HOA, HAT_NHAN, HAP_DAN }`, mỗi giá trị có `String langKey()` (`"phapthuat.element.nhiet"`...).
  - `Conversions.efficiency(Element from, Element to)`: bảng spec 7.4 (Nhiệt → Động trả `NaN`, nghĩa là tính theo Carnot). Đợt này chưa dùng.

- [ ] **Step 1: Viết test fail**

```java
@Test void woodData() { assertEquals(700, Materials.WOOD.density()); assertEquals(1700, Materials.WOOD.specificHeat()); assertEquals(300, Materials.WOOD.ignitionC()); }
@Test void coreMass() { assertEquals(0.7, Materials.WOOD.coreMassKg(), 1e-9); assertEquals(2.7, Materials.STONE.coreMassKg(), 1e-9); }
@Test void crackDeltas() {
    assertEquals(25.0, Materials.STONE.crackDeltaK(), 1e-9);
    assertEquals(47.619, Materials.GLASS.crackDeltaK(), 1e-3);
    assertEquals(2.222, Materials.ICE.crackDeltaK(), 1e-3);
    assertTrue(Double.isNaN(Materials.WOOD.crackDeltaK()));
}
@Test void estimatesFlagged() { assertTrue(Materials.LEAVES.estimated()); assertTrue(Materials.LIVING_CORE.estimated()); assertFalse(Materials.WOOD.estimated()); }
@Test void sevenElements() { assertEquals(7, Element.values().length); }
```

- [ ] **Step 2:** Chạy `g.cmd test`. Kỳ vọng: FAIL (lớp chưa tồn tại).
- [ ] **Step 3:** Viết `PhysicsConstants`, `Material`, `Materials`, `Element`, `Conversions` theo Interfaces.
- [ ] **Step 4:** Chạy `g.cmd test`. Kỳ vọng: PASS.
- [ ] **Step 5:** Commit `feat(physics): hằng số và bảng vật liệu`.

### Task 3: Lõi Gia Nhiệt

**Files:**
- Create: `.../physics/HeatTier.java`, `.../physics/HeatCalc.java`
- Test: `.../physics/HeatCalcTest.java`

**Interfaces:**
- Consumes: `Material`, `Materials`, `PhysicsConstants` (Task 2).
- Produces:
  - `enum HeatTier { NONE, CRACK, IGNITE, MELT, VAPORIZE }` (thứ tự tăng dần, dùng `compareTo`).
  - `record HeatResult(HeatTier tier, double finalTempC, double phaseProgressJ, double excessJ, double highestTierRequirementJ, boolean backlash, int backlashRadius)`
  - `static HeatResult HeatCalc.heat(Material m, double massKg, double startTempC, double phaseProgressJ, double energyJ)`
  - `record SteamBurst(double steamTempK, double workJ, double power)`; `static SteamBurst HeatCalc.steamBurst(double waterMassKg, double excessJ)`
  - `record AirBurst(double deltaTK, double finalTempK, double deltaPPa)`; `static AirBurst HeatCalc.airBurst(int airCells, double energyJ, double ambientC)`
  - `static int HeatCalc.burnSeconds(double excessJ, double joulesPerMana)` = min(10, 3 + floor(excess mana / 10))

Thuật toán `heat` (không suy ra được từ chữ ký): mô hình **entanpi từng đoạn**. Nhiệt độ tăng theo m·c cho tới điểm chuyển pha kế tiếp. Ở điểm chuyển pha, năng lượng vào `phaseProgressJ` (bình nguyên) cho tới khi đủ m·L. Sau đó tiếp tục với `liquidSpecificHeat` (nếu có) tới điểm sôi rồi m·L_hóa hơi. Bậc đạt được là bậc cao nhất thỏa:
- CRACK nếu vật giòn và E/(m·c) ≥ `crackDeltaK()`. Tính theo **ΔT của lần này**, không cộng dồn (sốc nhiệt cần thay đổi nhanh).
- IGNITE nếu cháy được và nhiệt độ đạt `ignitionC`.
- MELT nếu hết bình nguyên nóng chảy.
- VAPORIZE nếu hết bình nguyên hóa hơi.

`highestTierRequirementJ` = năng lượng cần, tính **từ trạng thái hiện tại**, để đạt bậc cao nhất mà vật liệu có. `backlash` = E ≥ 3 × requirement và requirement > 0. Bán kính = min(4, 1 + floor(E/requirement − 3)). `excessJ` = E trừ năng lượng đã dùng để đạt bậc. Khi chưa đạt bậc nào thì `finalTempC` và `phaseProgressJ` là trạng thái mới để lưu lại.

- [ ] **Step 1: Viết test fail** (mặc định nhiệt độ bắt đầu 20°C)

```java
@Test void woodIgnitesAt333kJ() { assertEquals(HeatTier.IGNITE, HeatCalc.heat(WOOD, 0.7, 20, 0, 333_200).tier()); }
@Test void woodShortStaysWarm() { var r = HeatCalc.heat(WOOD, 0.7, 20, 0, 316_000); assertEquals(HeatTier.NONE, r.tier()); assertEquals(285.5, r.finalTempC(), 0.1); }
@Test void partialCastsAccumulate() { var a = HeatCalc.heat(WOOD, 0.7, 20, 0, 200_000); assertEquals(HeatTier.IGNITE, HeatCalc.heat(WOOD, 0.7, a.finalTempC(), a.phaseProgressJ(), 133_200).tier()); }
@Test void stoneCracksAt25K() { assertEquals(HeatTier.CRACK, HeatCalc.heat(STONE, 2.7, 20, 0, 53_400).tier()); assertEquals(HeatTier.NONE, HeatCalc.heat(STONE, 2.7, 20, 0, 53_000).tier()); }
@Test void stoneMelts() { assertEquals(HeatTier.MELT, HeatCalc.heat(STONE, 2.7, 20, 0, 3_629_000).tier()); assertEquals(HeatTier.CRACK, HeatCalc.heat(STONE, 2.7, 20, 0, 3_628_000).tier()); }
@Test void stoneShortOfMeltIsNotBacklash() { var r = HeatCalc.heat(STONE, 2.7, 20, 0, 3_000_000); assertEquals(HeatTier.CRACK, r.tier()); assertFalse(r.backlash()); }
@Test void iceTiers() { assertEquals(HeatTier.CRACK, HeatCalc.heat(ICE, 0.917, 0, 0, 4_500).tier()); assertEquals(HeatTier.MELT, HeatCalc.heat(ICE, 0.917, 0, 0, 306_400).tier()); }
@Test void sandToGlass() { assertEquals(HeatTier.MELT, HeatCalc.heat(SAND, 1.6, 20, 0, 2_498_000).tier()); assertEquals(HeatTier.NONE, HeatCalc.heat(SAND, 1.6, 20, 0, 2_497_000).tier()); }
@Test void waterVaporizes() { var r = HeatCalc.heat(WATER, 1.0, 20, 0, 2_592_000); assertEquals(HeatTier.VAPORIZE, r.tier()); assertEquals(120, r.excessJ(), 1); }
@Test void steamPower() {
    assertEquals(1.72, HeatCalc.steamBurst(1.0, 0).power(), 0.01);
    assertEquals(172_213, HeatCalc.steamBurst(1.0, 0).workJ(), 50);
    assertEquals(4.03, HeatCalc.steamBurst(1.0, 1_000_000).power(), 0.01);
    assertEquals(8.0, HeatCalc.steamBurst(2.0, 5_000_000).power(), 1e-9);
}
@Test void airBurst() { assertEquals(28_660, HeatCalc.airBurst(7, 500_000, 20).deltaPPa(), 50); assertTrue(HeatCalc.airBurst(7, 611_000, 20).deltaPPa() >= 35_000); }
@Test void woodBacklashRadius() {
    assertEquals(1, HeatCalc.heat(WOOD, 0.7, 20, 0, 999_600).backlashRadius());
    assertEquals(2, HeatCalc.heat(WOOD, 0.7, 20, 0, 1_332_800).backlashRadius());
    assertEquals(4, HeatCalc.heat(WOOD, 0.7, 20, 0, 2_000_000).backlashRadius());
    assertFalse(HeatCalc.heat(WOOD, 0.7, 20, 0, 900_000).backlash());
}
@Test void livingCoreIgnites() { assertEquals(HeatTier.IGNITE, HeatCalc.heat(LIVING_CORE, 1.0, 37, 0, 1_270_600).tier()); }
@Test void burnSeconds() { assertEquals(3, HeatCalc.burnSeconds(0, 10_000)); assertEquals(4, HeatCalc.burnSeconds(100_000, 10_000)); assertEquals(10, HeatCalc.burnSeconds(1e7, 10_000)); }
```

- [ ] **Step 2:** Chạy `g.cmd test`. Kỳ vọng: FAIL.
- [ ] **Step 3:** Viết `HeatTier`, `HeatCalc` theo Interfaces và thuật toán ở trên. Công thức hơi nước: T_hơi = 373,15 + excess/(m × STEAM_CP); W = WATER_MOL_PER_KG × m × R × T_hơi; p = min(8, 4·W/TNT_REFERENCE_WORK_J). Không khí: m_khí = ô × 1,2; ΔT = E/(m_khí·718); ΔP = P0·(T₂/T₁ − 1).
- [ ] **Step 4:** Chạy `g.cmd test`. Kỳ vọng: PASS.
- [ ] **Step 5:** Commit `feat(physics): lõi Gia Nhiệt`.

### Task 4: Lõi Thu Nhiệt và nguội dần

**Files:**
- Create: `.../physics/SiphonCalc.java`, `.../physics/Cooling.java`, `.../physics/ColdTier.java`
- Test: `.../physics/SiphonCalcTest.java`, `.../physics/CoolingTest.java`

**Interfaces:**
- Consumes: Task 2.
- Produces:
  - `record SiphonResult(double finalTempC, double phaseProgressJ, double heatRemovedJ, double manaJoulesDelta, boolean transformed, boolean capped)`. `manaJoulesDelta` > 0 là được, < 0 là tốn (chưa tính phí kích hoạt).
  - `static SiphonResult SiphonCalc.siphon(Material m, double massKg, double startTempC, double phaseProgressJ, double heatJ, double ambientC)`
  - `enum ColdTier { NONE, SLOW, FREEZE, SEVERE }`; `static ColdTier SiphonCalc.coldTier(double dropK)` với ngưỡng 2 / 5 / 9 K.
  - `static double Cooling.relax(double value, double target, double dtSeconds, double halfLifeSeconds)` = target + (value − target)·0,5^(dt/halfLife).

Luật `siphon` (spec 8.2):
- Vật nóng có nhiệt hữu hạn (`maxExtractableJ` có giá trị: lửa, lửa trại): rút tối đa `maxExtractableJ − phaseProgressJ`; hết thì `transformed`.
- Dung nham (`meltC` = `naturalTempC` = 1200): rút **nhiệt đông đặc trước** (bình nguyên ở 1200°C, lưu ở `phaseProgressJ`), rồi mới rút nhiệt cảm nhận tới T_mt. Hết thì `transformed`.
- Nước: hạ tới 0°C rồi rút L_nc. Hết thì `transformed` (thành băng). Phần Q còn dư sau khi đông hẳn bị bỏ qua (`capped = true`).
- Lõi sinh vật / vật khác: hạ nhiệt độ theo m·c, không có mốc chuyển.
- Vật nóng không làm lạnh xuống dưới T_mt. Q vượt phần còn lại thì `capped = true` và chỉ rút phần còn lại.
- Phần nhiệt rút khi vật ở trên T_mt: được Q × (1 − T_mt,K / T_đầu,K), T_đầu là nhiệt độ lúc bắt đầu lần rút. Phần ở dưới T_mt: tốn Q / COP, COP = T_cuối,K / (T_mt,K − T_cuối,K) (T_cuối = T_mt thì không tốn).

- [ ] **Step 1: Viết test fail**

```java
@Test void lavaFullDrain() { var r = SiphonCalc.siphon(LAVA, 2.7, 1200, 0, 5_000_000, 20);
    assertEquals(4_903_200, r.heatRemovedJ(), 1); assertTrue(r.capped()); assertTrue(r.transformed()); assertEquals(3_927_500, r.manaJoulesDelta(), 2_000); }
@Test void lavaPartialLatentFirst() { var r = SiphonCalc.siphon(LAVA, 2.7, 1200, 0, 1_000_000, 20);
    assertFalse(r.transformed()); assertEquals(1200, r.finalTempC(), 1e-9); assertEquals(1_000_000, r.phaseProgressJ(), 1); assertEquals(801_000, r.manaJoulesDelta(), 1_000); }
@Test void lavaYieldsLessAsItCools() { var a = SiphonCalc.siphon(LAVA, 2.7, 1200, 1_000_000, 1_000_000, 20);
    assertEquals(916.0, a.finalTempC(), 0.5);
    var b = SiphonCalc.siphon(LAVA, 2.7, a.finalTempC(), a.phaseProgressJ(), 1_000_000, 20);
    assertTrue(b.manaJoulesDelta() < a.manaJoulesDelta()); }
@Test void waterFreezesCheaply() { var r = SiphonCalc.siphon(WATER, 1.0, 20, 0, 417_720, 20); assertTrue(r.transformed()); assertEquals(-30_585, r.manaJoulesDelta(), 100); }
@Test void fireCapped() { var r = SiphonCalc.siphon(FIRE, 0, 800, 0, 50_000, 20); assertEquals(20_000, r.heatRemovedJ(), 1e-9); assertTrue(r.capped()); assertTrue(r.transformed()); assertEquals(14_537, r.manaJoulesDelta(), 20); }
@Test void livingCoreGivesLittle() { var r = SiphonCalc.siphon(LIVING_CORE, 1.0, 37, 0, 17_500, 20); assertEquals(32, r.finalTempC(), 1e-6); assertEquals(959, r.manaJoulesDelta(), 5); }
@Test void undeadCoolingCosts() { var r = SiphonCalc.siphon(LIVING_CORE, 1.0, 20, 0, 17_500, 20); assertEquals(15, r.finalTempC(), 1e-6); assertEquals(-303.7, r.manaJoulesDelta(), 1); }
@Test void coldTiers() { assertEquals(ColdTier.NONE, SiphonCalc.coldTier(1.9)); assertEquals(ColdTier.SLOW, SiphonCalc.coldTier(2)); assertEquals(ColdTier.FREEZE, SiphonCalc.coldTier(5)); assertEquals(ColdTier.SEVERE, SiphonCalc.coldTier(9)); }
// CoolingTest
@Test void halfLife() { assertEquals(160, Cooling.relax(300, 20, 20, 20), 1e-9); assertEquals(90, Cooling.relax(300, 20, 40, 20), 1e-9); assertEquals(300, Cooling.relax(300, 20, 0, 20), 1e-9); }
```

- [ ] **Step 2:** Chạy `g.cmd test`. Kỳ vọng: FAIL.
- [ ] **Step 3:** Viết `SiphonCalc`, `ColdTier`, `Cooling` theo luật ở trên.
- [ ] **Step 4:** Chạy `g.cmd test`. Kỳ vọng: PASS.
- [ ] **Step 5:** Commit `feat(physics): lõi Thu Nhiệt và nguội dần`.

### Task 5: Hình học thao tác, đạn đạo, chia năng lượng, đọc thông số

**Files:**
- Create: `.../physics/ShapeMath.java`, `.../physics/Ballistics.java`, `.../physics/EnergySplit.java`
- Create: `mod/src/main/java/phapthuat/spell/SpellParam.java`, `.../spell/ParamParser.java` (Java thuần, không import Minecraft)
- Test: `.../physics/ShapeMathTest.java`, `.../physics/BallisticsTest.java`, `.../spell/ParamParserTest.java`

**Interfaces:**
- Produces:
  - `record Offset(int dx, int dy, int dz)` (trong `ShapeMath`).
  - `static List<Offset> ShapeMath.sphere(int r)`: mọi ô có dx²+dy²+dz² ≤ r².
  - `static List<Offset> ShapeMath.wall(int width, int height, boolean facingAlongX)`: mặt phẳng đứng vuông góc với hướng nhìn ngang. Bề rộng nằm trên trục còn lại, cao từ 0 tới height−1 tính từ tâm trở xuống, căn giữa (lệch về dương khi chẵn).
  - `record Column(int x, int z)`; `static List<Column> ShapeMath.groundLine(int x0, int z0, int x1, int z1)`: Bresenham, gồm cả hai đầu.
  - `static double Ballistics.range(double v, double thetaRad)`; `static double Ballistics.vyAfterTick(double vy)` = vy − G/20; vị trí mỗi tick += v/20 (m).
  - `static double EnergySplit.perUnit(double total, int units)` (units ≤ 0 → 0).
  - `record SpellParam(String key, String unitLangKey, double min, double max, double defaultValue)` với `double clamp(double v)`.
  - `static OptionalDouble ParamParser.parse(String text)`: chấp nhận dấu `,` hoặc `.` thập phân, có khoảng trắng hai đầu; từ chối rỗng, chữ, `NaN`, `Infinity`.

- [ ] **Step 1: Viết test fail**

```java
@Test void sphereCounts() { assertEquals(List.of(7, 33, 123, 257, 515), IntStream.rangeClosed(1, 5).mapToObj(r -> ShapeMath.sphere(r).size()).toList()); }
@Test void wallAlongX() { var w = ShapeMath.wall(3, 2, true); assertEquals(6, w.size()); assertTrue(w.stream().allMatch(o -> o.dx() == 0)); }
@Test void groundLine() { assertEquals(6, ShapeMath.groundLine(0, 0, 5, 0).size()); assertEquals(new ShapeMath.Column(5, 3), ShapeMath.groundLine(0, 0, 5, 3).getLast()); }
@Test void range45() { assertEquals(40.77, Ballistics.range(20, Math.PI / 4), 0.01); }
@Test void gravityPerSecond() { double vy = 0; for (int i = 0; i < 20; i++) vy = Ballistics.vyAfterTick(vy); assertEquals(-9.81, vy, 1e-9); }
@Test void split() { assertEquals(100, EnergySplit.perUnit(700, 7), 1e-9); assertEquals(0, EnergySplit.perUnit(700, 0), 1e-9); }
@Test void parseVietnameseComma() { assertEquals(33.3, ParamParser.parse(" 33,3 ").getAsDouble(), 1e-9); assertEquals(33.3, ParamParser.parse("33.3").getAsDouble(), 1e-9); }
@Test void parseRejectsJunk() { for (String s : List.of("", "abc", "NaN", "Infinity", "1,2,3")) assertTrue(ParamParser.parse(s).isEmpty(), s); }
@Test void clamp() { var p = new SpellParam("energy", "phapthuat.unit.mana", 0, 1000, 10); assertEquals(1000, p.clamp(1e9)); assertEquals(0, p.clamp(-5)); }
```

- [ ] **Step 2:** Chạy `g.cmd test`. Kỳ vọng: FAIL.
- [ ] **Step 3:** Viết các lớp theo Interfaces.
- [ ] **Step 4:** Chạy `g.cmd test`. Kỳ vọng: PASS.
- [ ] **Step 5:** Commit `feat(physics): hình học thao tác, đạn đạo, đọc thông số`.

### Task 6: Config và mana

**Files:**
- Modify: `.../Config.java`
- Create: `.../mana/ManaPool.java` (Java thuần), `.../mana/ManaAttachments.java`, `.../mana/ManaManager.java`, `.../network/ManaSyncPayload.java`, `.../network/ModNetwork.java`, `.../client/ManaHudLayer.java`
- Modify: `.../PhapThuat.java`, `.../PhapThuatClient.java`
- Test: `.../mana/ManaPoolTest.java`

**Interfaces:**
- Produces:
  - `Config`: `JOULES_PER_MANA` (10000), `MAX_MANA` (1000), `REGEN_PER_SECOND` (2), `HALF_LIFE_SECONDS` (20), `HOT_SOURCE_HALF_LIFE_SECONDS` (300), `DOSE_HALF_LIFE_SECONDS` (60), `SIPHON_GAIN_FACTOR` (1.0), `STEAM_EXPLOSION_BREAKS_BLOCKS` (true), `AIR_BURST_KNOCKBACK_PER_KPA` (0.03, vận tốc m/tick cho mỗi kPa, sao cho 50 kPa ≈ 1,5), `MAX_TRACKED_BLOCKS` (10000). Kèm `static double joulesPerMana()` tiện dùng.
  - `final class ManaPool { ManaPool(double max); double current(); boolean trySpend(double mana); void add(double mana); void regen(double seconds, double perSecond); void fill(); }`: không bao giờ < 0 hay > max.
  - Attachment `ManaAttachments.MANA`: `AttachmentType<Double>`, mặc định `MAX_MANA` (chết là có attachment mới, tức hồi đầy), serialize bằng `Codec.DOUBLE`.
  - `ManaManager`: `static double get(Player p)`, `static boolean trySpend(ServerPlayer p, double mana)`, `static void add(ServerPlayer p, double mana)`. Hồi mana trong `PlayerTickEvent.Post` (server); gửi `ManaSyncPayload(double mana)` khi giá trị hiển thị (làm tròn 0,1) thay đổi, khi đăng nhập và khi hồi sinh.
  - `ModNetwork.register(RegisterPayloadHandlersEvent)`: kênh phiên bản `"1"`. Các task sau thêm payload vào đây.
  - Client: `ClientManaCache.get()` (trong `ManaHudLayer` hoặc lớp riêng) giữ giá trị nhận được.

- [ ] **Step 1: Viết test fail**

```java
@Test void startsFull() { assertEquals(1000, new ManaPool(1000).current()); }
@Test void spend() { var p = new ManaPool(1000); assertTrue(p.trySpend(33.3)); assertEquals(966.7, p.current(), 1e-9); }
@Test void cannotOverspend() { var p = new ManaPool(1000); p.trySpend(999.95); assertFalse(p.trySpend(0.1)); assertEquals(0.05, p.current(), 1e-9); }
@Test void addCaps() { var p = new ManaPool(1000); p.trySpend(100); p.add(392.8); assertEquals(1000, p.current()); }
@Test void regen() { var p = new ManaPool(1000); p.trySpend(10); p.regen(1.0, 2); assertEquals(992, p.current(), 1e-9); }
```

- [ ] **Step 2:** Chạy `g.cmd test`. Kỳ vọng: FAIL.
- [ ] **Step 3:** Viết `ManaPool`, `Config`, attachment, `ManaManager` (dùng `ManaPool` để tính), payload, HUD. HUD: thanh xanh dương ngang phía trên thanh máu bên trái, cạnh đó là chữ `Mana 966,7 / 1000`.
- [ ] **Step 4:** Chạy `g.cmd test` rồi `g.cmd build`. Kỳ vọng: PASS và `BUILD SUCCESSFUL`.
- [ ] **Step 5:** Commit `feat: hệ mana`.

### Task 7: Vật phẩm, công thức chế tạo, texture, file ngôn ngữ

**Files:**
- Create: `.../registry/ModItems.java`, `.../registry/ModDataComponents.java`, `.../registry/ModCreativeTab.java`
- Create: `.../item/GrimoireItem.java`, `.../item/WandItem.java`, `.../item/MeterItem.java` (Task 8, 12, 13 bổ sung hành vi)
- Create: `mod/src/main/resources/assets/phapthuat/lang/vi_vn.json`, `en_us.json`
- Create: `.../assets/phapthuat/models/item/{grimoire,wand,meter}.json`
- Create: `.../assets/phapthuat/textures/item/{grimoire,wand,meter}.png` (sinh bằng script)
- Create: `mod/scripts/gen_textures.ps1`
- Create: `mod/src/main/resources/data/phapthuat/recipe/{grimoire,wand,meter}.json`

**Interfaces:**
- Produces: `ModItems.GRIMOIRE`, `ModItems.WAND`, `ModItems.METER` (`DeferredItem`, `stacksTo(1)`). `ModDataComponents.WAND_SLOTS` (`DataComponentType<WandSlots>`, kiểu `WandSlots` tạo ở Task 9; trong task này đăng ký sẵn chỗ). Tab sáng tạo "Pháp Thuật Vật Lý" chứa 3 vật phẩm. Khóa ngôn ngữ dạng `item.phapthuat.grimoire` = "Sách Công Thức", `item.phapthuat.wand` = "Pháp Trượng", `item.phapthuat.meter` = "Máy Đo", `phapthuat.element.*` = "Nhiệt", "Động", "Điện từ", "Quang", "Hóa", "Hạt nhân", "Hấp dẫn".

- [ ] **Step 1:** Đăng ký vật phẩm và tab. Chưa có hành vi.
- [ ] **Step 2:** Viết `gen_textures.ps1` (dùng `System.Drawing` có sẵn của Windows) vẽ 3 ảnh 16×16: sách bìa đỏ sẫm có ký hiệu ngọn lửa vàng, trượng gỗ có đầu pha lê xanh, máy đo xám có mặt kính và kim đỏ. Chạy bằng `powershell -ExecutionPolicy Bypass -File "D:\code_base\New folder\mod\scripts\gen_textures.ps1"`.
- [ ] **Step 3:** Công thức chế tạo:
  - Sách: không định hình, 1 sách + 1 than (than gỗ cũng được, dùng tag `minecraft:coals`).
  - Trượng: định hình `"  G"`, `" S "`, `"S  "`, G = thỏi vàng, S = que.
  - Máy đo: định hình `" P "`, `"IRI"`, `" I "`, P = tấm kính, I = thỏi sắt, R = bụi đỏ.
- [ ] **Step 4:** Chạy `g.cmd build`. Kỳ vọng: `BUILD SUCCESSFUL`. Chạy `g.cmd runClient`, vào thế giới sáng tạo: tab "Pháp Thuật Vật Lý" có 3 vật phẩm với tên tiếng Việt và texture đúng. Thoát game.
- [ ] **Step 5:** Commit `feat: vật phẩm Sách, Trượng, Máy Đo`.

### Task 8: Lưu nhiệt, nhiệt độ môi trường, Máy Đo

**Files:**
- Create: `.../thermal/ThermalTable.java` (Java thuần), `.../thermal/BlockThermal.java` (Java thuần)
- Create: `.../thermal/ThermalStore.java` (SavedData theo từng thế giới), `.../thermal/EntityThermal.java`, `.../thermal/ThermalAttachments.java`, `.../thermal/ThermalTicker.java`
- Create: `.../thermal/WorldTemps.java`, `.../thermal/MaterialLookup.java`, `.../physics/EntityMass.java` (Java thuần, bảng spec 7.3 theo id thực thể)
- Modify: `.../item/MeterItem.java`
- Create: `mod/src/main/java/phapthuat/gametest/ThermalGameTests.java`, `.../gametest/ModGameTests.java`
- Test: `.../thermal/ThermalTableTest.java`, `.../physics/EntityMassTest.java`

**Interfaces:**
- Consumes: `Cooling` (Task 4), `Materials` (Task 2), `Config` (Task 6).
- Produces:
  - `record BlockThermal(double tempC, double phaseProgressJ, double naturalC, double halfLifeSeconds)`.
  - `final class ThermalTable { ThermalTable(int maxEntries); Optional<BlockThermal> get(long key); void put(long key, BlockThermal v); void remove(long key); void decay(double dtSeconds); int size(); }`: `decay` đưa mỗi mục về `naturalC` (và progress về 0) bằng `Cooling.relax`, bỏ mục khi |temp − natural| < 1 K và progress < 1 J; vượt `maxEntries` thì bỏ mục được thêm lâu nhất.
  - `ThermalStore.get(ServerLevel)`: bọc `ThermalTable` (key = `BlockPos.asLong()`), lưu/đọc NBT. `decay` chạy mỗi 20 tick trong `LevelTickEvent.Post`. Bỏ mục khi khối ở vị trí đó không còn là vật liệu đã lưu (kiểm khi đọc).
  - `record EntityThermal(double coreTempC, double doseGy)`; attachment `ThermalAttachments.ENTITY_THERMAL`. Mỗi 20 tick (`EntityTickEvent.Post`, chỉ với thực thể có data) về tự nhiên theo `HALF_LIFE_SECONDS` (liều theo `DOSE_HALF_LIFE_SECONDS`), gỡ data khi đã về gần tự nhiên.
  - `WorldTemps.ambientC(Level, BlockPos)` = 25 × nhiệt độ biome tại vị trí đó (hàm có tính độ cao của `Biome`; nếu không truy cập được từ ngoài thì dùng access transformer). `WorldTemps.currentBlock(ServerLevel, BlockPos, Material) → BlockThermal` (giá trị lưu, hoặc trạng thái tự nhiên spec 6.4). `WorldTemps.currentEntityC(LivingEntity)`. `WorldTemps.isUndead(Entity)` dùng `EntityTypeTags.UNDEAD`.
  - `MaterialLookup.of(BlockState) → Optional<Material>`: theo tag/khối trong spec 7.2 (`BlockTags.LOGS`, `PLANKS`, `WOOL`, `LEAVES`, nước nguồn, `ICE`/`PACKED_ICE`, cát, đá các loại ở 7.2, thủy tinh/tấm kính mọi màu, obsidian, dung nham nguồn, magma, lửa, lửa trại đang cháy, nến, TNT, kiện rơm, khối than, không khí).
  - `EntityMass.kg(String entityTypeId, double hitboxVolumeM3)`: bảng spec 7.3, không có trong bảng thì 100 × thể tích hitbox.
  - Máy Đo (chuột phải, phía server, gửi tin nhắn hệ thống): `Nhiệt độ môi trường: 20,0°C (293,2 K)`, `Mục tiêu: Gỗ — 285,5°C`, (nếu có tiến độ chuyển pha) `đã chuyển pha 42%`, `Khoảng cách: 5,0 m`, với sinh vật thêm `Khối lượng: 70 kg` và `Liều phóng xạ: 0,0 Gy`, cuối cùng `Trời mưa: có/không`, `Trong nước: có/không`.

- [ ] **Step 1: Viết test fail**

```java
@Test void decaysTowardNatural() { var t = new ThermalTable(10_000); t.put(1L, new BlockThermal(300, 0, 20, 20)); t.decay(20); assertEquals(160, t.get(1L).orElseThrow().tempC(), 1e-9); }
@Test void settledEntriesRemoved() { var t = new ThermalTable(10_000); t.put(1L, new BlockThermal(20.5, 0, 20, 20)); t.decay(1); assertTrue(t.get(1L).isEmpty()); }
@Test void capEvictsOldest() { var t = new ThermalTable(2); t.put(1L, new BlockThermal(300, 0, 20, 20)); t.put(2L, new BlockThermal(300, 0, 20, 20)); t.put(3L, new BlockThermal(300, 0, 20, 20)); assertEquals(2, t.size()); assertTrue(t.get(1L).isEmpty()); }
// EntityMassTest
@Test void table() { assertEquals(70, EntityMass.kg("minecraft:zombie", 0.7)); assertEquals(600, EntityMass.kg("minecraft:cow", 1.1)); assertEquals(50, EntityMass.kg("minecraft:unknown_thing", 0.5), 1e-9); }
```

- [ ] **Step 2:** Chạy `g.cmd test`. Kỳ vọng: FAIL.
- [ ] **Step 3:** Viết các lớp theo Interfaces. Đăng ký GameTest: `ModGameTests` nghe `RegisterGameTestsEvent` và đăng ký các lớp test. Lớp test dùng `@GameTestHolder(PhapThuat.MODID)`. Template trống: dùng annotation `EmptyTemplate` của NeoForge nếu có trong 21.1.265. Nếu không có, tạo `data/phapthuat/structure/empty_7x7x7.nbt` (khối không khí 7×7×7 trên nền đá) bằng một tiện ích ghi `NbtIo`, rồi tham chiếu `template = "phapthuat:empty_7x7x7"`.
- [ ] **Step 4:** Viết GameTest `ThermalGameTests.storedTempDecays`: đặt gỗ, ghi `ThermalStore` 300°C, sau 400 tick (20 giây) đọc ≈ 160°C (sai số 3°C).
- [ ] **Step 5:** Chạy `g.cmd test` rồi `g.cmd runGameTestServer`. Kỳ vọng: PASS và log có `All required tests passed`.
- [ ] **Step 6:** Commit `feat: lưu nhiệt, nhiệt độ môi trường, Máy Đo`.

### Task 9: Khung phép: thao tác, ô trượng, chọn mục tiêu, thi triển, đạn phép

**Files:**
- Create: `.../spell/Shape.java`, `.../spell/Spell.java`, `.../spell/SpellRegistry.java`, `.../spell/InscribedSpell.java`, `.../spell/WandSlots.java`, `.../spell/SpellUnit.java`, `.../spell/CastContext.java`, `.../spell/CastReport.java`, `.../spell/Targeting.java`, `.../spell/SpellCaster.java`, `.../spell/Fmt.java`
- Create: `.../entity/SpellProjectile.java`, `.../registry/ModEntities.java`, `.../client/SpellProjectileRenderer.java` (chỉ vẽ hạt hiệu ứng)
- Modify: `.../registry/ModDataComponents.java` (kiểu `WandSlots` thật)
- Create: `.../gametest/TargetingGameTests.java`
- Test: `.../spell/WandSlotsTest.java`

**Interfaces:**
- Consumes: `SpellParam`, `ShapeMath`, `Ballistics`, `EnergySplit` (Task 5), `ManaManager`, `Config` (Task 6), `Element` (Task 2).
- Produces:
  - `enum Shape { DIEM, TIA, DUONG, CAU, TUONG, DAN, BAN_THAN }`. Mỗi giá trị có `List<SpellParam> params()`: TIA `length` 1–16 (mặc định 8); CAU `radius` 1–5 (2); TUONG `width` 1–7 (3), `height` 1–5 (2); DAN `speed` 5–100 (20); các thao tác khác không có thông số. Kèm `langKey()`.
  - `interface Spell { String id(); Element element(); Set<Shape> shapes(); List<SpellParam> params(); double upfrontCostMana(InscribedSpell s); boolean affects(ServerLevel level, SpellUnit unit); CastReport apply(CastContext ctx, List<SpellUnit> units); }`. `apply` tự chia năng lượng (gọi `EnergySplit`), tự cộng/trừ thêm mana nếu có (Thu Nhiệt), tự xử lý phản tác dụng.
  - `SpellRegistry.register(Spell)`, `Optional<Spell> get(String id)`, `List<Spell> all()` (theo thứ tự đăng ký).
  - `record InscribedSpell(String spellId, Shape shape, Map<String, Double> params)` với `CODEC`, `STREAM_CODEC`, `double param(String key)`.
  - `record WandSlots(List<Optional<InscribedSpell>> slots, int selected)`: `SIZE = 5`; `EMPTY`; `Optional<InscribedSpell> get(int i)`; `WandSlots with(int i, InscribedSpell s)`; `WandSlots cycle(int delta)` (vòng 0..4); `CODEC`, `STREAM_CODEC`.
  - `sealed interface SpellUnit` gồm `record BlockUnit(BlockPos pos)`, `record AirUnit(BlockPos pos)`, `record EntityUnit(Entity entity)`.
  - `record CastContext(ServerLevel level, ServerPlayer caster, InscribedSpell spell, double joulesPerMana, Vec3 center)`.
  - `record CastReport(boolean success, Component message)`.
  - `Targeting.resolve(ServerPlayer caster, InscribedSpell s) → List<SpellUnit>` và `Targeting.center(...)`. Tầm 16 khối theo spec 6.5. Nhìn vào khoảng không thì là ô không khí ở 16 khối, kẹp trong chiều cao thế giới. Chỉ giữ những đơn vị mà `spell.affects(...)` chấp nhận.
  - `SpellCaster.cast(ServerPlayer p, InscribedSpell s) → CastReport`: tìm phép → trừ `upfrontCostMana` (thiếu thì báo `Không đủ mana`) → với DAN thì sinh `SpellProjectile`, ngược lại `Targeting.resolve` → `spell.apply` → gửi `report.message()` lên thanh hành động (action bar) của người chơi. `SpellCaster.castOnUnits(ServerPlayer p, InscribedSpell s, List<SpellUnit> units, Vec3 center)` bỏ qua bước chọn mục tiêu (dùng cho đạn và GameTest).
  - `SpellProjectile`: khối lượng 0,1 kg, vận tốc ban đầu `speed` (m/s) theo hướng nhìn, mỗi tick `Ballistics.vyAfterTick`, không dùng trọng lực vanilla. Chạm khối hoặc sinh vật thì gọi `castOnUnits` với đơn vị bị chạm. Chạm nước thì tắt (hạt hơi nước), không có tác dụng. Sau 200 tick thì biến mất. Từ tick thứ 20 trở đi được phép chạm người bắn.
  - `Fmt.num(double v, int decimals)` (dấu phẩy kiểu Việt), `Fmt.mana(double)`, `Fmt.tempC(double)`.
  - `interface TimedEffect { boolean tick(ServerLevel level, int ageTicks); }` (trả `false` là kết thúc) và `TimedEffects.add(ServerLevel level, TimedEffect e)`, chạy mỗi tick trong `LevelTickEvent.Post`. Không lưu qua lần thoát game, vì hiệu ứng nào cũng ngắn (≤ 600 giây). Riêng khối đặt tạm vào thế giới thì phải có cơ chế dọn riêng (kế hoạch 2). Dùng cho đám hơi của Gia Nhiệt và mọi hiệu ứng kéo dài ở kế hoạch 2.

- [ ] **Step 1: Viết test fail** (`WandSlotsTest`, JUnit thuần, chỉ test logic không cần codec)

```java
@Test void emptyHasFive() { assertEquals(5, WandSlots.EMPTY.slots().size()); assertTrue(WandSlots.EMPTY.get(0).isEmpty()); }
@Test void cycleWraps() { assertEquals(4, WandSlots.EMPTY.cycle(-1).selected()); assertEquals(0, WandSlots.EMPTY.cycle(5).selected()); }
@Test void withReplaces() { var s = new InscribedSpell("gia_nhiet", Shape.DIEM, Map.of("energy", 33.3)); assertEquals(s, WandSlots.EMPTY.with(2, s).get(2).orElseThrow()); }
```

- [ ] **Step 2:** Chạy `g.cmd test`. Kỳ vọng: FAIL.
- [ ] **Step 3:** Viết các lớp theo Interfaces. Đăng ký thực thể và renderer.
- [ ] **Step 4:** Viết GameTest trong `TargetingGameTests`, dùng một `Spell` giả chỉ ghi lại danh sách đơn vị:
  - `sphereResolvesSevenCells`: thao tác CAU r = 1 quanh một ô trong không khí → 7 đơn vị.
  - `pointAtSkyGivesAirAt16`: người chơi giả nhìn thẳng lên trời → 1 `AirUnit` cách mắt 16 khối.
  - `projectileDespawnsAfter10s`: bắn DAN thẳng lên với 100 m/s ở nơi không có trần → sau 201 tick thực thể không còn.
  - `projectileFallsBackOnCaster`: bắn DAN thẳng lên với 5 m/s → spell giả được gọi với `EntityUnit` là chính người bắn.
- [ ] **Step 5:** Chạy `g.cmd test` rồi `g.cmd runGameTestServer`. Kỳ vọng: PASS.
- [ ] **Step 6:** Commit `feat: khung phép, thao tác, đạn phép`.

### Task 10: Phép Gia Nhiệt trong thế giới

**Files:**
- Create: `.../spell/impl/HeatSpell.java`
- Modify: `.../PhapThuat.java` (đăng ký vào `SpellRegistry`)
- Create: `.../gametest/HeatSpellGameTests.java`

**Interfaces:**
- Consumes: `HeatCalc` (Task 3), `ThermalStore`, `WorldTemps`, `MaterialLookup` (Task 8), khung phép (Task 9).
- Produces: phép id `gia_nhiet`, nguyên tố NHIET, thao tác {DIEM, TIA, DUONG, CAU, TUONG, DAN, BAN_THAN}, thông số `energy` (mana, 0–1000, mặc định 10). `upfrontCostMana` = energy. `affects`: khối có trong `MaterialLookup`, ô không khí (chỉ với CAU và TUONG), sinh vật không miễn lửa.
  - `static CastReport HeatSpell.applyHeat(CastContext ctx, List<SpellUnit> units, double totalJoules, Component sourceName)`: toàn bộ luật áp nhiệt bên dưới, với năng lượng truyền vào. `HeatSpell.apply` chỉ gọi hàm này với energy × joulesPerMana. Kế hoạch 2 dùng lại cho Đốt Nhiên Liệu và Hồ Quang.
  - `static boolean HeatSpell.affectsHeat(ServerLevel level, SpellUnit unit, Shape shape)`: luật `affects` ở trên, để phép khác dùng lại.

Áp dụng mỗi đơn vị theo spec 8.1 (năng lượng mỗi đơn vị = energy × joulesPerMana / N):
- Khối: `HeatCalc.heat` với trạng thái hiện tại từ `WorldTemps.currentBlock`. NONE thì lưu trạng thái mới vào `ThermalStore` và tạo hạt khói. CRACK thì phá khối, rơi vật phẩm như spec (đá → đá cuội, obsidian → obsidian, thủy tinh/băng không rơi). IGNITE thì đặt lửa (ô cạnh mặt nhìn với DIEM, ô phía trên với thao tác khác), kích nổ TNT, thắp nến/lửa trại. MELT: băng/tuyết → nước, cát → thủy tinh, đá → dung nham. VAPORIZE: bỏ nước (bọt biển ướt → khô, bùn → đất, khối ngậm nước → hết nước) rồi gọi `HeatCalc.steamBurst`. p < 2 thì chỉ có đám hơi (bán kính 2, 3 giây, 1 máu/giây) và hất nhẹ; p ≥ 2 thì `level.explode(..., p, STEAM_EXPLOSION_BREAKS_BLOCKS ? TNT : NONE)`.
- Mục tiêu trong nước (ô kề có nước, hoặc khối ngậm nước) thì không bắt lửa.
- Khối không cháy khác: hạt lửa khoảng 1 giây trên mặt khối.
- Ô không khí: gộp lại, gọi `HeatCalc.airBurst(sốÔ, tổngJ, ambient)`. Tác dụng: vỡ kính khi ≥ 7 kPa; hất văng (`AIR_BURST_KNOCKBACK_PER_KPA` × ΔP kPa, hướng ra xa tâm); mất máu (ΔP − 35 kPa)/5 kPa khi ≥ 35 kPa; bốc cháy khi T₂ ≥ 300°C.
- Sinh vật: lõi 1 kg (`LIVING_CORE`), trạng thái từ `EntityThermal`. Đạt IGNITE thì `setRemainingFireTicks(HeatCalc.burnSeconds(...) × 20)`. Chưa đạt thì lưu nhiệt độ lõi.
- Phản tác dụng: `backlash` thì đặt lửa lên các khối dễ cháy trong `backlashRadius`, và người chơi trong bán kính đó bị cháy 3 giây. Người chơi trong vùng hơi, khí nóng hay vụ nổ thì chịu như sinh vật khác (tự nhiên, không miễn).
- Báo kết quả một dòng (spec 6.7), ví dụ `Gia Nhiệt: lõi Gỗ đạt 285,5°C / cần 300,0°C — thất bại, mất 31,6 mana`, `Gia Nhiệt: Đá nóng chảy thành dung nham — 363,0 mana`, `Gia Nhiệt (7 đơn vị): khí nóng +82,9 K, áp suất dư 28,7 kPa`.

- [ ] **Step 1: Viết GameTest fail** (`HeatSpellGameTests`, người chơi giả đủ mana, gọi `SpellCaster.castOnUnits` với một `BlockUnit`/`AirUnit`/`EntityUnit`, ở biome có T_mt = 20°C, hoặc chuẩn hóa bằng cách đặt sẵn trạng thái tự nhiên)
  - `woodIgnitesWith33_4Mana`: gỗ + 33,4 mana → ô bên cạnh là lửa.
  - `woodShortKeepsHeat`: gỗ + 31,6 mana → không có lửa; `ThermalStore` có ≈ 285,5°C. Thêm 2 mana ngay sau đó → có lửa.
  - `stoneCracksWith5_4Mana`: đá + 5,4 → khối biến mất, có vật phẩm đá cuội rơi.
  - `stoneMeltsWith363Mana`: đá + 363 → dung nham nguồn.
  - `iceMeltsWith30_7Mana`: băng + 30,7 → nước. (Nếu biome lạnh hơn 0°C thì dùng biome mặc định của test.)
  - `waterVaporizesWith259_3Mana`: nước nguồn + 259,3 → không khí, không có vụ nổ.
  - `steamBombWith359_3Mana`: nước + 359,3 → nước biến mất, khối xung quanh bị phá (vụ nổ công suất ≈ 4).
  - `tntPrimed`: TNT + 25 mana → xuất hiện `PrimedTnt`.
  - `noFireUnderwater`: khối gỗ mà ô không khí cạnh mặt nhìn là nước nguồn + 40 mana → không có lửa ở đâu cả.
  - `airBurstBreaksGlass`: CAU r = 1 quanh ô không khí, cách 1 ô có tấm kính, 50 mana → tấm kính vỡ.
  - `backlashBurnsCaster`: gỗ cách người chơi 1 khối + 100 mana → người chơi đang cháy.
- [ ] **Step 2:** Chạy `g.cmd runGameTestServer`. Kỳ vọng: FAIL (phép chưa có).
- [ ] **Step 3:** Viết `HeatSpell` theo luật ở trên và đăng ký.
- [ ] **Step 4:** Chạy `g.cmd test` rồi `g.cmd runGameTestServer`. Kỳ vọng: PASS.
- [ ] **Step 5:** Commit `feat: phép Gia Nhiệt`.

### Task 11: Phép Thu Nhiệt trong thế giới

**Files:**
- Create: `.../spell/impl/SiphonSpell.java`
- Modify: `.../PhapThuat.java`, `.../thermal/ThermalTicker.java` (hiệu ứng lạnh theo bậc mỗi giây)
- Create: `.../gametest/SiphonSpellGameTests.java`

**Interfaces:**
- Consumes: `SiphonCalc`, `ColdTier` (Task 4), Task 8, Task 9.
- Produces: phép id `thu_nhiet`, nguyên tố NHIET, thao tác {DIEM, CAU, BAN_THAN}, thông số `heat` (mana quy đổi, 0–1000, mặc định 10). `upfrontCostMana` = 1 (phí kích hoạt). `affects`: dung nham nguồn, magma, lửa, lửa trại đang cháy, nước nguồn, sinh vật.

Áp dụng theo spec 8.2 (nhiệt mỗi đơn vị = heat × joulesPerMana / N):
- `SiphonCalc.siphon` với trạng thái hiện tại. Lưu trạng thái mới (dung nham/magma dùng `HOT_SOURCE_HALF_LIFE_SECONDS`).
- `transformed`: dung nham → obsidian, magma → netherrack, lửa → không khí, lửa trại → tắt, nước → băng.
- Sinh vật: dùng lõi 1 kg (`LIVING_CORE`) và `EntityThermal.coreTempC` (cùng một nhiệt độ lõi với Gia Nhiệt, như spec 6.8). Đang cháy thì tắt lửa. Mỗi giây `ThermalTicker` xét `SiphonCalc.coldTier(tự nhiên − hiện tại)`: SLOW → chậm chạp; FREEZE → `setTicksFrozen` tối đa + 1 máu/giây; SEVERE → 3 máu/giây.
- Tổng `manaJoulesDelta` × `SIPHON_GAIN_FACTOR` (chỉ áp hệ số cho phần dương) đổi ra mana. Dương thì `ManaManager.add`. Âm mà không đủ mana thì trừ hết mana, phần thiếu đổi ra nhiệt rút từ người chơi: hạ lõi người chơi theo `SiphonCalc` rồi `setTicksFrozen` tối đa (phản tác dụng).
- `capped` thì báo thêm `đã rút hết nhiệt có thể rút`.
- Báo kết quả, ví dụ `Thu Nhiệt: Dung nham nguội thành hắc diện thạch — nhận 392,8 mana (η = 0,80)`, `Thu Nhiệt: Nước đông thành băng — tốn 3,1 mana (COP 13,7)`.

- [ ] **Step 1: Viết GameTest fail**
  - `lavaFullDrainGivesObsidianAndMana`: người chơi còn 500 mana; dung nham nguồn + heat 500 → obsidian; mana ≈ 500 − 1 + 392,8 (sai số 1).
  - `lavaPartialStaysLavaAndYieldsLess`: hai lần heat 100 liên tiếp → vẫn là dung nham; mana nhận lần 2 ≤ lần 1.
  - `waterFreezes`: nước + heat 41,8 → băng; mana giảm ≈ 1 + 3,1.
  - `fireExtinguished`: lửa + heat 5 → không khí.
  - `overdrawFreezesCaster`: người chơi còn 1,5 mana; nước + heat 500 (làm lạnh tốn vượt mana) → mana 0 và `player.getTicksFrozen() > 0`.
- [ ] **Step 2:** Chạy `g.cmd runGameTestServer`. Kỳ vọng: FAIL.
- [ ] **Step 3:** Viết `SiphonSpell` và phần hiệu ứng lạnh trong `ThermalTicker`, rồi đăng ký.
- [ ] **Step 4:** Chạy `g.cmd test` rồi `g.cmd runGameTestServer`. Kỳ vọng: PASS.
- [ ] **Step 5:** Commit `feat: phép Thu Nhiệt`.

### Task 12: Dùng Pháp Trượng: thi triển, đổi ô, HUD

**Files:**
- Modify: `.../item/WandItem.java`
- Create: `.../network/SelectSlotPayload.java`, `.../client/WandInput.java`, `.../client/WandHudLayer.java`
- Modify: `.../network/ModNetwork.java`, `.../PhapThuatClient.java`

**Interfaces:**
- Consumes: `WandSlots`, `SpellCaster`, `SpellRegistry`, `Fmt` (Task 9).
- Produces: `SelectSlotPayload(int delta)` (client → server): server áp `WandSlots.cycle(delta)` lên trượng đang cầm tay chính.

- [ ] **Step 1:** `WandItem.use` (server): ô đang chọn trống thì báo `Ô 3 chưa khắc phép`, ngược lại `SpellCaster.cast`. Hồi chiêu 10 tick (`player.getCooldowns().addCooldown`). Trả `InteractionResultHolder.success`.
- [ ] **Step 2:** `WandInput`: nghe `InputEvent.MouseScrollingEvent`. Nếu đang cầm trượng ở tay chính và đang Shift thì gửi `SelectSlotPayload(±1)` và hủy sự kiện (không đổi ô hotbar).
- [ ] **Step 3:** `WandHudLayer`: khi cầm trượng, vẽ phía trên hotbar: `[3/5] Gia Nhiệt · Điểm · năng lượng 33,3 mana`, ô trống thì `[3/5] (trống)`.
- [ ] **Step 4:** Chạy `g.cmd build` rồi `g.cmd runClient`. Thử bằng tay: lấy trượng từ tab sáng tạo, dùng lệnh `/give` nếu cần. Shift + lăn chuột thì số ô đổi 1 → 5 → 1. Chuột phải với ô trống thì hiện `Ô n chưa khắc phép`. Thoát game.
- [ ] **Step 5:** Commit `feat: dùng Pháp Trượng`.

### Task 13: Sách Công Thức: đọc, chọn thao tác, khắc phép

**Files:**
- Modify: `.../item/GrimoireItem.java`
- Create: `.../client/GrimoireScreen.java`, `.../client/ClientHooks.java`, `.../network/InscribePayload.java`, `.../spell/InscribeValidator.java`
- Modify: `.../network/ModNetwork.java`, `lang/vi_vn.json`, `lang/en_us.json`
- Create: `.../gametest/InscribeGameTests.java`

**Interfaces:**
- Consumes: `SpellRegistry`, `Shape`, `SpellParam`, `ParamParser`, `WandSlots`, `Materials`, `PhysicsConstants`, `Config`.
- Produces:
  - `InscribePayload(int slot, InscribedSpell spell)` (client → server).
  - `InscribeValidator.validate(InscribedSpell raw) → Optional<InscribedSpell>`: phép phải tồn tại, thao tác phải thuộc `spell.shapes()`, slot trong 0..4. Mỗi thông số của phép và của thao tác: thiếu thì lấy mặc định, NaN/vô cực thì từ chối, có giá trị thì `clamp`. Khóa lạ bị bỏ.
  - Server: chỉ ghi khi tay phụ đang cầm Pháp Trượng. Báo `Đã khắc Gia Nhiệt vào ô 3`.

- [ ] **Step 1: Viết GameTest fail** `InscribeGameTests`:
  - `inscribeClampsAndWrites`: tay phụ có trượng; validate + ghi `InscribedSpell("gia_nhiet", DIEM, {energy: 1e9, bogus: 5})` vào ô 2 → ô 2 có energy = 1000, không có `bogus`.
  - `inscribeRejectsWrongShape`: `("thu_nhiet", TIA, ...)` → bị từ chối, trượng không đổi.
  - `inscribeRequiresWandInOffhand`: tay phụ trống → không ghi gì.
- [ ] **Step 2:** Chạy `g.cmd runGameTestServer`. Kỳ vọng: FAIL.
- [ ] **Step 3:** Viết `InscribeValidator`, payload và handler.
- [ ] **Step 4:** Viết `GrimoireScreen`. `GrimoireItem.use` phía client gọi `ClientHooks.openGrimoire()` (lớp chỉ nạp ở client).
  - Cột trái: "Mở đầu", "Hằng số và vật liệu", rồi mỗi phép trong `SpellRegistry.all()` (nhóm theo nguyên tố).
  - Trang phải cuộn được. **Mở đầu**: cách chơi, 7 nguyên tố, 1 mana = `Config.JOULES_PER_MANA` J, luật lõi 1 lít, luật thao tác kèm bảng số ô của cầu (7/33/123/257/515), luật lưu nhiệt và nguội dần (chu kỳ bán rã đọc từ Config), đổi °C ↔ K. **Hằng số và vật liệu**: in từ `PhysicsConstants`, `Materials.all()` và `EntityMass`, đánh dấu "(ước lượng)" theo `estimated()`. **Trang phép**: nguyên tố, công thức, giải thích, luật thành công/thất bại/phản tác dụng (lời văn tiếng Việt viết từ spec 8.1 và 8.2, **không in đáp số ví dụ**), các thao tác được phép.
  - Khi tay phụ có trượng, trang phép có thêm: nút chọn thao tác (chỉ các thao tác được phép), ô nhập cho mỗi thông số của phép và của thao tác (nhãn có đơn vị và khoảng min–max, đọc bằng `ParamParser`, sai định dạng thì viền đỏ và nút khắc bị khóa), nút ô 1–5, nút **"Khắc phép"** gửi `InscribePayload`. Không hiển thị bất kỳ con số dự đoán nào.
- [ ] **Step 5:** Chạy `g.cmd runGameTestServer` rồi `g.cmd runClient`. Thử bằng tay: mở sách, đọc 3 loại trang; cầm trượng tay phụ; khắc Gia Nhiệt · Điểm · 33,4 vào ô 1; chuột phải vào một khúc gỗ → gỗ bốc cháy, dòng kết quả hiện ra, mana giảm 33,4. Nhập `abc` → nút khắc bị khóa. Thoát game.
- [ ] **Step 6:** Commit `feat: Sách Công Thức và khắc phép`.

### Task 14: Hướng dẫn cho chủ dự án và kiểm tra cuối

**Files:**
- Create: `D:\code_base\New folder\HUONG_DAN.md`

- [ ] **Step 1:** Viết `HUONG_DAN.md` bằng tiếng Việt, ngắn:
  - Cách mở game có mod: mở PowerShell, `& "D:\code_base\New folder\mod\g.cmd" runClient`.
  - Cách chế tạo 3 vật phẩm (công thức ở Task 7).
  - Cách khắc phép và thi triển.
  - Cách bỏ `mod\build\libs\phapthuat-1.0.0.jar` vào một bản cài 1.21.1 NeoForge trên CurseForge (tạo profile mới → chọn NeoForge 1.21.1 → mở thư mục profile → bỏ jar vào `mods`).
  - Cách gỡ sạch: chạy `g.cmd --stop` rồi xóa thư mục `D:\code_base\New folder`.
- [ ] **Step 2:** Chạy `g.cmd build`, `g.cmd test`, `g.cmd runGameTestServer`. Kỳ vọng: cả ba thành công, không test nào fail.
- [ ] **Step 3:** Chạy `g.cmd runClient`. Tạo thế giới mới ở chế độ sinh tồn, chế tạo đủ 3 vật phẩm từ nguyên liệu thật. Thử Gia Nhiệt (gỗ, đá, nước, quả cầu không khí, đạn) và Thu Nhiệt (dung nham, nước). Thoát rồi vào lại thế giới trong vòng 20 giây sau khi nung một khối: Máy Đo vẫn đo được nhiệt độ còn lưu.
- [ ] **Step 4:** Commit `docs: hướng dẫn cho chủ dự án`.
