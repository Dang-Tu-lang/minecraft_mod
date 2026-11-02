# Kế hoạch 2: Bảy phép nền tảng còn lại — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Thêm 7 phép nền tảng còn lại (Xung Lực, Hồ Quang, Từ Trường, Phát Quang, Đốt Nhiên Liệu, Bức Xạ Neutron, Nâng) lên nền móng của kế hoạch 1, đủ 9 phép cho cả 7 nguyên tố.

**Architecture:** Giống kế hoạch 1. Mỗi phép có một lớp tính toán Java thuần trong `phapthuat.physics` (JUnit với ví dụ số của spec) và một lớp `Spell` trong `phapthuat.spell.impl` (GameTest cho hiệu ứng trong thế giới). Đốt Nhiên Liệu và Hồ Quang dùng lại `HeatSpell.applyHeat`.

**Tech Stack:** Java 21, NeoForge 21.1.265, Minecraft 1.21.1, JUnit 5, GameTest.

**Spec:** `docs/superpowers/specs/2026-11-02-he-phep-nang-luong-design.md` (mục 8.3–8.9, 7.3). Thực hiện **sau** khi toàn bộ `docs/superpowers/plans/2026-11-02-ke-hoach-1-nen-mong-va-nhiet.md` đã xong.

## Global Constraints

- Mọi ràng buộc trong mục Global Constraints của kế hoạch 1 vẫn áp dụng nguyên văn.
- Vận tốc do phép đặt cho bất kỳ thực thể nào bị kẹp tối đa **40 m/s** (2 khối/tick).
- Người chơi ở chế độ sáng tạo không bị trừ nhiên liệu (giống vanilla), nhưng vẫn bị trừ mana.
- Khối đặt tạm vào thế giới (khối sáng của Phát Quang) phải được dọn kể cả khi game bị thoát giữa chừng.

## Review Focus

1. **Thoát game khi quả cầu sáng còn đang cháy:** khối sáng không được nằm lại vĩnh viễn. Test: `PlacedLightsTest` (Task 1), `lightRemovedAfterDuration` (Task 7).
2. **Vận tốc cực lớn** (đẩy golem khi đang nhảy, nâng 30 m): không làm thực thể xuyên khối hay lỗi chunk, luôn kẹp 40 m/s. Test: `recoilCappedAt40` (Task 2), `recoilWhenAirborneIsCapped` (Task 4).
3. **Nhiên liệu nằm rải nhiều chồng hoặc không đủ:** phải gom đúng n vật phẩm từ nhiều chồng, thiếu thì không trừ gì. Test: `consumesAcrossStacks`, `missingFuelConsumesNothing` (Task 8).
4. **Nâng khối không được phép** (rương, lò, đá nền, khối không phá được): bị bỏ qua, không mất đồ trong rương. Test: `cannotLiftChestOrBedrock` (Task 10).
5. **Hồ Quang không có mục tiêu** (nhìn lên trời): coi d = 16 m, xử lý như thất bại hoặc phản tác dụng theo luật, không crash. Test: `arcAtSkyUsesSixteenMeters` (Task 5).

---

### Task 1: Mở rộng khung phép cho kế hoạch 2

**Files:**
- Modify: `.../spell/SpellParam.java`, `.../spell/Spell.java`, `.../spell/InscribeValidator.java`, `.../client/GrimoireScreen.java`, `.../client/WandHudLayer.java`
- Create: `.../physics/PhysicsConstants.java` (bổ sung), `.../thermal/PlacedLights.java` (SavedData), `.../thermal/LightTable.java` (Java thuần)
- Test: `.../spell/ParamParserTest.java` (bổ sung), `.../thermal/PlacedLightsTest.java`

**Interfaces:**
- Produces:
  - `SpellParam` thêm trường `List<String> choiceLangKeys` (rỗng = tham số số). Constructor cũ giữ nguyên, thêm factory `SpellParam.choice(String key, List<String> choiceLangKeys, int defaultIndex)`. Giá trị lưu là chỉ số kiểu double. `clamp` làm tròn về số nguyên trong [0, n−1].
  - `Spell` thêm `default List<SpellParam> shapeParams(Shape shape) { return shape.params(); }`, cho phép một phép đổi khoảng thông số của thao tác (Từ Trường đổi bán kính CAU thành 1–6). `Targeting`, `InscribeValidator` và `GrimoireScreen` phải dùng `spell.shapeParams(shape)` thay cho `shape.params()`.
  - `GrimoireScreen` vẽ tham số lựa chọn thành dãy nút bấm. `WandHudLayer` hiện tên lựa chọn thay vì số.
  - `PhysicsConstants` thêm: `BREAKDOWN_V_PER_M = 3e6`, `RAIN_BREAKDOWN_FACTOR = 0.8`, `BODY_CAPACITANCE_F = 100e-12`, `MU0 = 1.2566e-6`, `LUMENS_PER_WATT_PEAK = 683`, `PAIN_W_M2 = 2_500`, `IGNITE_W_M2 = 12_500`, `UV_BURN_W_M2 = 10`, `UVC_BLIND_W_M2 = 1`, `O2_KG_PER_M3 = 0.276`, `THORNTON_J_PER_KG_O2 = 13.1e6`, `NEUTRON_EFFICIENCY = 2e-4`, `COAL_ENERGY_J = 8.2e6`, `COAL_BURN_TICKS = 1600`, `MAX_SPELL_SPEED = 40`, `CASTER_MASS_KG = 70`.
  - `final class LightTable { void add(long pos, long expiresAtGameTime); List<Long> expired(long nowGameTime); void remove(long pos); }` và `PlacedLights.get(ServerLevel)` bọc nó (lưu NBT). Mỗi 20 tick, khối `minecraft:light` ở vị trí đã hết hạn bị xóa. Khi tải thế giới, mọi mục đã hết hạn bị xóa ngay.

- [ ] **Step 1: Viết test fail**

```java
@Test void choiceClampsToIndex() { var p = SpellParam.choice("direction", List.of("a", "b"), 0); assertEquals(1, p.clamp(7.6)); assertEquals(0, p.clamp(-3)); assertEquals(1, p.clamp(0.6)); }
// PlacedLightsTest
@Test void expiredReturned() { var t = new LightTable(); t.add(1L, 100); t.add(2L, 300); assertEquals(List.of(1L), t.expired(200)); }
```

- [ ] **Step 2:** Chạy `g.cmd test`. Kỳ vọng: FAIL.
- [ ] **Step 3:** Viết theo Interfaces.
- [ ] **Step 4:** Chạy `g.cmd test` và `g.cmd runGameTestServer` (mọi test của kế hoạch 1 vẫn phải PASS).
- [ ] **Step 5:** Commit `feat: mở rộng khung phép cho kế hoạch 2`.

### Task 2: Lõi vật lý Động và Hấp dẫn

**Files:**
- Create: `.../physics/ImpulseCalc.java`, `.../physics/LiftCalc.java`, `.../physics/IronContent.java`
- Test: `.../physics/ImpulseCalcTest.java`, `.../physics/LiftCalcTest.java`, `.../physics/IronContentTest.java`

**Interfaces:**
- Produces:
  - `static double ImpulseCalc.speed(double energyJ, double massKg)` = √(2E/m), kẹp `MAX_SPELL_SPEED`.
  - `static double ImpulseCalc.impulse(double energyJ, double massKg)` = √(2·m·E) (không kẹp).
  - `static double ImpulseCalc.recoilSpeed(double totalImpulse)` = min(J / `CASTER_MASS_KG`, `MAX_SPELL_SPEED`).
  - `static double LiftCalc.energy(double massKg, double heightM)` = m·g·h.
  - `static double LiftCalc.impactDamageHp(double massKg, double fallM)` = m·g·h / 1000.
  - `static double IronContent.kg(String entityTypeId, List<String> wornItemIds)`: golem sắt 500, xe goòng 5, cộng giáp sắt (mũ 5, áo 8, quần 7, giày 4). `static double IronContent.itemKg(String itemId, int count)`: thỏi sắt 1/cái, khối sắt 9, hạt sắt 1/9, đồ sắt khác 0.

- [ ] **Step 1: Viết test fail**

```java
@Test void zombieSpeed() { assertEquals(16.903, ImpulseCalc.speed(10_000, 70), 1e-3); }
@Test void golemImpulse() { assertEquals(6.3246, ImpulseCalc.speed(10_000, 500), 1e-4); assertEquals(3162.3, ImpulseCalc.impulse(10_000, 500), 0.1); }
@Test void recoilCappedAt40() { assertEquals(40, ImpulseCalc.recoilSpeed(3162.3), 1e-9); assertEquals(16.903, ImpulseCalc.recoilSpeed(1183.2), 1e-3); }
@Test void liftStone() { assertEquals(132_435, LiftCalc.energy(2700, 5), 1e-6); assertEquals(6_867, LiftCalc.energy(70, 10), 1e-6); }
@Test void impact() { assertEquals(132.435, LiftCalc.impactDamageHp(2700, 5), 1e-6); }
@Test void ironArmor() { assertEquals(24, IronContent.kg("minecraft:zombie", List.of("minecraft:iron_helmet", "minecraft:iron_chestplate", "minecraft:iron_leggings", "minecraft:iron_boots"))); assertEquals(500, IronContent.kg("minecraft:iron_golem", List.of())); assertEquals(5, IronContent.kg("minecraft:minecart", List.of())); assertEquals(9, IronContent.itemKg("minecraft:iron_block", 1)); }
```

- [ ] **Step 2:** `g.cmd test` → FAIL. **Step 3:** Viết. **Step 4:** `g.cmd test` → PASS. **Step 5:** Commit `feat(physics): Động và Hấp dẫn`.

### Task 3: Lõi vật lý Điện từ, Quang, Hóa, Hạt nhân

**Files:**
- Create: `.../physics/ArcCalc.java`, `.../physics/MagnetCalc.java`, `.../physics/OpticsCalc.java`, `.../physics/CombustionCalc.java`, `.../physics/RadiationCalc.java`
- Test: một file test cho mỗi lớp

**Interfaces:**
- Produces:
  - `enum ArcOutcome { HIT, BACKFIRE_CASTER, FIZZLE }`; `static ArcOutcome ArcCalc.outcome(double voltageV, double distanceM, boolean raining)` (spec 8.4: đủ cho d thì HIT; không đủ nhưng đủ cho 0,5 m thì BACKFIRE_CASTER; còn lại FIZZLE); `static double ArcCalc.requiredVoltage(double distanceM, boolean raining)`; `static double ArcCalc.chargeEnergyJ(double voltageV)` = ½·C·U²; `static double ArcCalc.damageHp(double energyJ)` = E(kJ)/20; `static boolean ArcCalc.lightningEffects(double energyJ, double joulesPerMana)` khi E ≥ 50 mana.
  - `static double MagnetCalc.energyDensity(double teslas)` = B²/(2μ₀); `static double MagnetCalc.fieldEnergyJ(double teslas, double radiusM)` (thể tích cầu hình học 4/3·π·r³ như spec 8.5); `static double MagnetCalc.accel(double k, double teslas, double ironKg, double massKg)` = k·B²·ironKg/massKg.
  - `static double OpticsCalc.luminosity(double nm)` = 1,019·exp(−285,4·(nm/1000 − 0,559)²); `static double OpticsCalc.lumens(double nm, double watts)`; `static int OpticsCalc.lightLevel(double lumens)` = kẹp(làm tròn(6 + 4·log₁₀Φ), 0, 15), Φ ≤ 0 → 0; `static double OpticsCalc.intensity(double watts, double distanceM)` = P/(4πd²); `static double OpticsCalc.laserIntensity(double watts)` = P / (π·0,01²); `isVisible(nm)` 380–750, `isUv(nm)` < 400, `isUvc(nm)` < 280, `isInfrared(nm)` > 750.
  - `record Combustion(double releasedJ, boolean carbonMonoxide)`; `static double CombustionCalc.itemEnergyJ(int burnTicks)` = burnTicks/1600 × 8,2 MJ; `static double CombustionCalc.oxygenNeededKg(double energyJ)`; `static Combustion CombustionCalc.burn(double energyJ, int connectedAirCells)` (≥ 64 ô = thoáng).
  - `enum DoseTier { NONE, NAUSEA, POISON, WITHER, SEVERE }`; `static double RadiationCalc.absorbedJ(double mana, double joulesPerMana)` = mana × J/mana × 2×10⁻⁴; `static double RadiationCalc.attenuation(int solidBlocks, int waterBlocks)` = 0,5^solid × 0,1^water; `static double RadiationCalc.doseGy(double absorbedJ, double massKg)`; `static DoseTier RadiationCalc.tier(double gy)` ngưỡng 0,5 / 2 / 6 / 10; `static double RadiationCalc.scatterDoseGy(double targetAbsorbedJ, double bystanderMassKg)` = 5% × J / m.

- [ ] **Step 1: Viết test fail**

```java
// ArcCalcTest
@Test void voltageFor5m() { assertEquals(15e6, ArcCalc.requiredVoltage(5, false), 1e-3); assertEquals(12e6, ArcCalc.requiredVoltage(5, true), 1e-3); }
@Test void outcomes() { assertEquals(ArcOutcome.HIT, ArcCalc.outcome(15e6, 5, false)); assertEquals(ArcOutcome.BACKFIRE_CASTER, ArcCalc.outcome(14e6, 5, false)); assertEquals(ArcOutcome.FIZZLE, ArcCalc.outcome(1e6, 5, false)); assertEquals(ArcOutcome.HIT, ArcCalc.outcome(12e6, 5, true)); }
@Test void charge() { assertEquals(11_250, ArcCalc.chargeEnergyJ(15e6), 1e-6); }
@Test void damage() { assertEquals(10, ArcCalc.damageHp(200_000), 1e-9); assertTrue(ArcCalc.lightningEffects(500_000, 10_000)); assertFalse(ArcCalc.lightningEffects(499_000, 10_000)); }
// MagnetCalcTest
@Test void field() { assertEquals(99_474, MagnetCalc.energyDensity(0.5), 5); assertEquals(416_677, MagnetCalc.fieldEnergyJ(0.5, 1), 20); }
// OpticsCalcTest
@Test void eye() { assertEquals(1.0144, OpticsCalc.luminosity(555), 1e-3); assertEquals(0.0343, OpticsCalc.luminosity(450), 1e-3); assertEquals(0.0959, OpticsCalc.luminosity(650), 1e-3); }
@Test void levels() { assertEquals(6, OpticsCalc.lightLevel(1)); assertEquals(11, OpticsCalc.lightLevel(23.4)); assertEquals(14, OpticsCalc.lightLevel(100)); assertEquals(15, OpticsCalc.lightLevel(692.8)); assertEquals(0, OpticsCalc.lightLevel(0.01)); }
@Test void intensities() { assertEquals(10.0, OpticsCalc.intensity(3142, 5), 0.01); assertEquals(31_831, OpticsCalc.laserIntensity(10), 1); }
@Test void bands() { assertTrue(OpticsCalc.isVisible(390) && OpticsCalc.isUv(390)); assertTrue(OpticsCalc.isUvc(250)); assertTrue(OpticsCalc.isInfrared(1000)); assertFalse(OpticsCalc.isVisible(1000)); }
// CombustionCalcTest
@Test void fuels() { assertEquals(8.2e6, CombustionCalc.itemEnergyJ(1600), 1e-6); assertEquals(1_537_500, CombustionCalc.itemEnergyJ(300), 1e-6); assertEquals(512_500, CombustionCalc.itemEnergyJ(100), 1e-6); }
@Test void oxygen() { assertEquals(0.62595, CombustionCalc.oxygenNeededKg(8.2e6), 1e-4); }
@Test void enclosed() { var c = CombustionCalc.burn(8.2e6, 2); assertTrue(c.carbonMonoxide()); assertEquals(7.231e6, c.releasedJ(), 1e4); var open = CombustionCalc.burn(8.2e6, 64); assertFalse(open.carbonMonoxide()); assertEquals(8.2e6, open.releasedJ(), 1e-6); assertFalse(CombustionCalc.burn(8.2e6, 3).carbonMonoxide()); }
// RadiationCalcTest
@Test void dose() { assertEquals(420, RadiationCalc.absorbedJ(210, 10_000), 1e-9); assertEquals(6.0, RadiationCalc.doseGy(420, 70), 1e-9); }
@Test void shielding() { assertEquals(0.5, RadiationCalc.attenuation(1, 0), 1e-12); assertEquals(0.1, RadiationCalc.attenuation(0, 1), 1e-12); assertEquals(0.025, RadiationCalc.attenuation(2, 1), 1e-12); }
@Test void tiers() { assertEquals(DoseTier.NONE, RadiationCalc.tier(0.49)); assertEquals(DoseTier.NAUSEA, RadiationCalc.tier(0.5)); assertEquals(DoseTier.POISON, RadiationCalc.tier(2)); assertEquals(DoseTier.WITHER, RadiationCalc.tier(6)); assertEquals(DoseTier.SEVERE, RadiationCalc.tier(10)); }
@Test void scatter() { assertEquals(0.3, RadiationCalc.scatterDoseGy(420, 70), 1e-9); }
```

- [ ] **Step 2:** `g.cmd test` → FAIL. **Step 3:** Viết. **Step 4:** `g.cmd test` → PASS. **Step 5:** Commit `feat(physics): Điện từ, Quang, Hóa, Hạt nhân`.

### Task 4: Phép Xung Lực

**Files:**
- Create: `.../spell/impl/ImpulseSpell.java`, `.../gametest/ImpulseSpellGameTests.java`
- Modify: `.../PhapThuat.java` (đăng ký)

**Interfaces:**
- Consumes: `ImpulseCalc` (Task 2), `EntityMass` (kế hoạch 1 Task 8), khung phép.
- Produces: phép `xung_luc`, nguyên tố DONG, thao tác {DIEM, TIA, CAU, BAN_THAN}. Thông số: `energy` (mana, 0–100, mặc định 1), `direction` (lựa chọn: `phapthuat.choice.push` "Đẩy", `phapthuat.choice.pull` "Kéo"). `upfrontCostMana` = energy. `affects`: sinh vật, vật phẩm rơi, xe goòng, thuyền (không phải khối).

Áp dụng (spec 8.3), năng lượng mỗi đơn vị = E/N. Hướng: DIEM/TIA theo hướng nhìn (Kéo thì ngược lại, về phía người chơi); CAU tỏa ra từ tâm (Kéo thì hút về tâm); BAN_THAN: người chơi lao theo hướng nhìn và không bị giật ngược. Đặt `deltaMovement` = hướng × speed/20 (khối/tick), `hurtMarked = true` để đồng bộ. Giật ngược: nếu người chơi `!onGround()` hoặc đứng trên băng/băng nén/băng lam thì cộng vận tốc `recoilSpeed(tổng xung lượng)` ngược hướng đẩy (Kéo thì kéo người chơi về phía mục tiêu). Báo kết quả: `Xung Lực: Zombie (70 kg) đạt 16,9 m/s`, có giật ngược thì thêm `— bạn bị giật lùi 40,0 m/s`.

- [ ] **Step 1: Viết GameTest fail**
  - `pushZombie`: zombie cách 3 khối, DIEM, 1 mana, Đẩy → ngay sau đó độ lớn vận tốc ≈ 0,845 khối/tick (sai số 0,05), hướng ra xa người chơi.
  - `pullItem`: thỏi sắt rơi cách 4 khối, Kéo → vận tốc hướng về người chơi.
  - `noRecoilOnGround`: người chơi đứng trên đá đẩy golem sắt 1 mana → vận tốc người chơi ≈ 0.
  - `recoilWhenAirborneIsCapped`: người chơi lơ lửng (không có nền dưới chân) đẩy golem 1 mana → vận tốc người chơi ≈ 2,0 khối/tick, ngược hướng.
  - `recoilOnIce`: người chơi đứng trên băng → có giật ngược.
- [ ] **Step 2:** `g.cmd runGameTestServer` → FAIL. **Step 3:** Viết và đăng ký. **Step 4:** `g.cmd test` + `g.cmd runGameTestServer` → PASS. **Step 5:** Commit `feat: phép Xung Lực`.

### Task 5: Phép Hồ Quang

**Files:**
- Create: `.../spell/impl/ArcSpell.java`, `.../gametest/ArcSpellGameTests.java`
- Modify: `.../PhapThuat.java`

**Interfaces:**
- Consumes: `ArcCalc` (Task 3), `HeatSpell.applyHeat` (kế hoạch 1 Task 10).
- Produces: phép `ho_quang`, nguyên tố DIEN_TU, thao tác {DIEM}. Thông số: `voltage` (kV, 1–50 000, mặc định 1000), `energy` (mana, 0–200, mặc định 5). `upfrontCostMana` = energy + `chargeEnergyJ(voltage × 1000)` / joulesPerMana. `affects`: khối và sinh vật.

Áp dụng (spec 8.4): d = khoảng cách từ mắt người chơi tới điểm trúng; không có mục tiêu thì d = 16. Mưa = `level.isRainingAt(vị trí người chơi)`.
- HIT: sinh vật mất `damageHp(E)` máu (`damageSources().lightningBolt()` hoặc nguồn sát thương riêng "phóng điện"). Gọi `HeatSpell.applyHeat(ctx, [đơn vị], E, Component.translatable("phapthuat.spell.ho_quang.name"))`. `lightningEffects` thì gọi `entity.thunderHit(level, tia sét giả không gây lửa)` để có biến đổi vanilla (creeper tích điện, lợn → piglin xác sống, dân làng → phù thủy). Vẽ tia bằng hạt `ELECTRIC_SPARK` dọc đường thẳng.
- BACKFIRE_CASTER: người chơi chịu `damageHp(E)`, tia ngắn ở tay.
- FIZZLE: chỉ có hạt lách tách, không ai bị thương.
- Nước: nếu mục tiêu hoặc người chơi đang ở trong nước thì thay vì đánh một mục tiêu, chia `damageHp(E)` cho mọi sinh vật đang ở trong nước trong bán kính 4 khối (kể cả người chơi).
- Báo kết quả: `Hồ Quang: E = 3,00 MV/m ≥ 3,00 MV/m — trúng Zombie, 10,0 sát thương`, hoặc `... 2,80 MV/m < 3,00 MV/m — điện phóng ngược vào bạn!`.

- [ ] **Step 1: Viết GameTest fail**
  - `arcHitsAt5m`: zombie cách đúng 5,0 m, 15 000 kV, 20 mana → zombie còn 10 máu; mana giảm 20 + 1,125.
  - `arcBackfiresOnCaster`: 14 000 kV → người chơi mất 10 máu, zombie nguyên vẹn.
  - `arcFizzles`: 1000 kV → không ai mất máu.
  - `arcAtSkyUsesSixteenMeters`: nhìn lên trời, 40 000 kV (40 MV / 16 m = 2,5 MV/m < 3) → phản tác dụng vào người chơi, không crash.
  - `chargedCreeper`: creeper cách 3 m, 10 000 kV, 50 mana → creeper `isPowered()`.
  - `waterSpreads`: người chơi và 2 zombie cùng đứng trong nước trong 4 khối → cả 3 đều mất máu.
- [ ] **Step 2–5:** như Task 4. Commit `feat: phép Hồ Quang`.

### Task 6: Phép Từ Trường

**Files:**
- Create: `.../spell/impl/MagnetSpell.java`, `.../gametest/MagnetSpellGameTests.java`
- Modify: `.../PhapThuat.java`, `.../Config.java` (thêm `MAGNET_K`, mặc định 0,4 m/s² trên mỗi T², chỉnh sau khi thử)

**Interfaces:**
- Consumes: `MagnetCalc`, `IronContent` (Task 2, 3), `TimedEffects` (kế hoạch 1 Task 9).
- Produces: phép `tu_truong`, nguyên tố DIEN_TU, thao tác {CAU} với `shapeParams(CAU)` = bán kính 1–6. Thông số: `field` (T, 0,1–5, mặc định 0,5), `duration` (giây, 1–10, mặc định 3). `upfrontCostMana` = `fieldEnergyJ(field, radius)` / joulesPerMana. `affects`: thực thể có `IronContent` > 0.

Áp dụng (spec 8.5): một `TimedEffect` kéo dài `duration` giây. Mỗi tick, mọi thực thể chứa sắt trong cầu (tính lại mỗi tick, kể cả người chơi mặc giáp sắt) được cộng vận tốc hướng về tâm: `accel(MAGNET_K, B, sắt, khối lượng)` / 400 (m/s² sang khối/tick²). Hạt hiệu ứng dạng đường sức quanh tâm. Báo kết quả: `Từ Trường: 0,50 T, bán kính 1 m — năng lượng trường 41,7 mana`.

- [ ] **Step 1: Viết GameTest fail**
  - `pullsIronIngot`: thỏi sắt rơi cách tâm 3 khối, B 0,5, r 4, 2 giây → sau 20 tick khoảng cách tới tâm giảm.
  - `armoredZombiePulledPlainNot`: zombie đủ giáp sắt bị kéo về tâm, zombie không giáp đứng yên.
  - `casterWithIronArmorPulled`: người chơi mặc giáp sắt trong cầu bị kéo.
  - `costMatchesFieldEnergy`: B 0,5, r 1 → mana giảm 41,7 (sai số 0,1).
- [ ] **Step 2–5:** như Task 4. Commit `feat: phép Từ Trường`.

### Task 7: Phép Phát Quang

**Files:**
- Create: `.../spell/impl/LightSpell.java`, `.../gametest/LightSpellGameTests.java`
- Modify: `.../PhapThuat.java`

**Interfaces:**
- Consumes: `OpticsCalc` (Task 3), `PlacedLights` (Task 1), `TimedEffects`, `HeatSpell.affectsHeat`.
- Produces: phép `phat_quang`, nguyên tố QUANG, thao tác {DIEM, TIA}. Thông số: `wavelength` (nm, 100–2000, mặc định 555), `power` (W, 0,01–100 000, mặc định 1), `duration` (giây, 1–600, mặc định 60). `upfrontCostMana` = power × duration / joulesPerMana.

Áp dụng (spec 8.6) bằng một `TimedEffect` kéo dài `duration` giây:
- DIEM: quả cầu ở ô không khí trước mặt khối đang nhìn. Nếu `isVisible` và `lightLevel(lumens) > 0`: đặt `minecraft:light` mức đó (chỉ khi ô là không khí) và ghi vào `PlacedLights` với thời điểm hết hạn; hết hạn thì xóa. Hạt màu theo bước sóng. Mỗi giây, với mọi sinh vật trong 8 khối (kể cả người chơi), I = `intensity(P, d)`: UV và I ≥ 10 thì undead bốc cháy; UV-C và I ≥ 10 thì mất 1 máu; UV-C và I ≥ 1 tại người chơi thì người chơi bị mù 3 giây; hồng ngoại và I ≥ 2 500 thì mất 1 máu; I ≥ 12 500 thì bốc cháy (khối dễ cháy trong 8 khối có I ≥ 12 500 cũng bắt lửa).
- TIA: chùm theo hướng lúc thi triển, dài tới khối rắn đầu tiên (tối đa 16). Mỗi giây, mọi sinh vật trên chùm và khối cuối chịu I = `laserIntensity(P)` theo cùng các ngưỡng. Thêm: nhìn thấy và P ≥ 1 W chiếu trúng sinh vật thì người chơi bị mù 3 giây, mob mất mục tiêu (`setTarget(null)`).
- Báo kết quả: `Phát Quang: 555 nm, 1,00 W → 692,8 lm, mức sáng 15`, hoặc `1000 nm là hồng ngoại — không nhìn thấy được`.

- [ ] **Step 1: Viết GameTest fail**
  - `greenLightLevel15`: 555 nm, 1 W, 10 giây → ô quả cầu là `minecraft:light` mức 15.
  - `lightRemovedAfterDuration`: sau 10 giây + 40 tick → ô đó là không khí, `PlacedLights` rỗng.
  - `infraredNoLight`: 1000 nm → không có khối sáng; mana vẫn bị trừ.
  - `uvBurnsZombie`: 350 nm, 3142 W, zombie cách quả cầu 4 khối → zombie đang cháy sau 25 tick.
  - `irLaserIgnitesWood`: 1000 nm, 10 W, TIA, 2 giây, nhắm vào gỗ → có lửa cạnh gỗ.
- [ ] **Step 2–5:** như Task 4. Commit `feat: phép Phát Quang`.

### Task 8: Phép Đốt Nhiên Liệu

**Files:**
- Create: `.../spell/impl/FuelSpell.java`, `.../spell/impl/FuelChoices.java`, `.../gametest/FuelSpellGameTests.java`
- Modify: `.../PhapThuat.java`

**Interfaces:**
- Consumes: `CombustionCalc` (Task 3), `HeatSpell.applyHeat`, `HeatSpell.affectsHeat` (kế hoạch 1).
- Produces: phép `dot_nhien_lieu`, nguyên tố HOA, thao tác {DIEM, TIA, DUONG, CAU, TUONG, DAN}. Thông số: `fuel` (lựa chọn theo `FuelChoices`: than đá, than củi, khối than, ván gỗ (tag `minecraft:planks`), gỗ khúc (tag `minecraft:logs_that_burn`), que, que quỷ lửa), `count` (1–16, mặc định 1). `upfrontCostMana` = 1. `affects` = `HeatSpell.affectsHeat`.
  - `FuelChoices.matches(int index, ItemStack s)`, `FuelChoices.langKey(int index)`.

Áp dụng (spec 8.7):
- Gom đúng `count` vật phẩm khớp từ túi đồ (nhiều chồng cũng được). Thiếu thì không trừ gì và báo `Thiếu nhiên liệu: cần 1 Than đá`. Chế độ sáng tạo thì không trừ.
- Tổng năng lượng = Σ `itemEnergyJ(stack.getBurnTime(RecipeType.SMELTING))`.
- Oxy: đếm ô không khí liên thông với tâm tác động (flood fill 6 hướng, dừng ở 64). `CombustionCalc.burn`.
- Gọi `HeatSpell.applyHeat(ctx, units, released, Component.translatable("phapthuat.spell.dot_nhien_lieu.name"))`.
- `carbonMonoxide`: mọi sinh vật trong vùng không khí đã đếm (kể cả người chơi) bị buồn nôn và trúng độc 10 giây.
- Với DAN: nhiên liệu bị trừ lúc bắn, oxy đếm tại chỗ chạm.
- Báo kết quả: `Đốt Nhiên Liệu: 1 Than đá = 8,20 MJ; oxy đủ` hoặc `chỉ đủ oxy cho 88% — sinh khí CO!`, kèm dòng của Gia Nhiệt.

- [ ] **Step 1: Viết GameTest fail**
  - `coalIgnitesWood`: người chơi có 1 than, DIEM vào gỗ cách 6 khối → than còn 0, gỗ có lửa.
  - `consumesAcrossStacks`: 2 chồng que, mỗi chồng 1 cái, count 2 → cả hai đều bị trừ.
  - `missingFuelConsumesNothing`: chỉ có 1 que, count 2 → không trừ gì, không có lửa.
  - `enclosedPocketMakesCO`: hộp đá kín 2 ô không khí có một con lợn, tâm tác động trong hộp, 1 than → lợn có hiệu ứng buồn nôn và độc.
  - `openAirNoCO`: ngoài trời → không ai trúng độc.
- [ ] **Step 2–5:** như Task 4. Commit `feat: phép Đốt Nhiên Liệu`.

### Task 9: Phép Bức Xạ Neutron

**Files:**
- Create: `.../spell/impl/RadiationSpell.java`, `.../gametest/RadiationSpellGameTests.java`
- Modify: `.../PhapThuat.java`, `.../thermal/ThermalTicker.java` (hiệu ứng theo liều mỗi giây)

**Interfaces:**
- Consumes: `RadiationCalc` (Task 3), `EntityThermal.doseGy` và nhịp giảm liều (kế hoạch 1 Task 8), `EntityMass`.
- Produces: phép `buc_xa_neutron`, nguyên tố HAT_NHAN, thao tác {DIEM, TIA, CAU}. Thông số `energy` (mana, 0–1000, mặc định 10). `upfrontCostMana` = energy. `affects`: chỉ sinh vật.

Áp dụng (spec 8.8): năng lượng hấp thụ mỗi đơn vị = `absorbedJ(energy)`/N × `attenuation(số khối rắn, số khối nước trên đoạn thẳng từ mắt người chơi tới mục tiêu)`. Liều cộng dồn vào `EntityThermal.doseGy`. Tán xạ: mọi sinh vật khác trong 3 khối quanh mục tiêu (kể cả người chơi) cộng `scatterDoseGy`. `ThermalTicker` mỗi giây áp hiệu ứng theo `tier(liều hiện tại)`: NAUSEA → buồn nôn + yếu ớt; POISON → độc + yếu ớt + chậm chạp; WITHER → khô héo; SEVERE → khô héo, và **một lần** khi vừa vượt 10 Gy thì mất 50% máu tối đa. Hiệu ứng làm mới mỗi giây (thời lượng 2 giây), nên tự tắt khi liều giảm khỏi bậc. Báo kết quả: `Bức Xạ Neutron: Lợn (100 kg) nhận 6,00 Gy (tổng 6,00 Gy)`.

- [ ] **Step 1: Viết GameTest fail**
  - `pigWitherAt6Gy`: lợn cách 5 khối, 300 mana → liều ≈ 6,0; có hiệu ứng khô héo sau 21 tick.
  - `waterShield`: một khối nước giữa người chơi và lợn → liều ≈ 0,6, chỉ có buồn nôn.
  - `doseAccumulates`: hai lần 150 mana → tổng ≈ 6,0.
  - `scatterHitsCasterNearby`: lợn cách 2 khối → người chơi có liều > 0.
  - `doseDecays`: đặt liều 6,0, sau 1200 tick (60 giây) ≈ 3,0 (sai số 0,2).
- [ ] **Step 2–5:** như Task 4. Commit `feat: phép Bức Xạ Neutron`.

### Task 10: Phép Nâng

**Files:**
- Create: `.../spell/impl/LiftSpell.java`, `.../gametest/LiftSpellGameTests.java`
- Modify: `.../PhapThuat.java`, `.../physics/Materials.java` (thêm `DEFAULT_SOLID`: ρ 1500, ước lượng, dùng cho khối không có trong bảng khi tính khối lượng cả khối)

**Interfaces:**
- Consumes: `LiftCalc` (Task 2), `EntityMass`, `Materials`, `MaterialLookup`, `TimedEffects`.
- Produces: phép `nang`, nguyên tố HAP_DAN, thao tác {DIEM, CAU, BAN_THAN}. Thông số: `height` (m, 0,5–30, mặc định 3), `duration` (giây, 1–10, mặc định 3). `upfrontCostMana` = Σ `LiftCalc.energy(m, height)` / joulesPerMana của các đơn vị nhắm tới (tính ở `Targeting` trước khi trừ). Không đủ mana thì không nâng gì. `affects`: sinh vật và khối rắn không có block entity, độ cứng ≥ 0, không phải đá nền.

Áp dụng (spec 8.9) bằng một `TimedEffect`:
- Sinh vật: tắt trọng lực tạm thời, đưa lên đều tới `height` trong 1 giây, giữ ở đó tới hết `duration`, rồi bật lại trọng lực (sát thương rơi theo vanilla).
- Khối: thay bằng `FallingBlockEntity` (khối lượng ρ × 1 m³), nâng như sinh vật, hết thời gian thì thả. Khi rơi trúng sinh vật, sinh vật mất `impactDamageHp(m, quãng rơi)` máu (dùng `setHurtsEntities` hoặc tự kiểm va chạm), sau đó khối đặt lại như vanilla.
- Báo kết quả: `Nâng: Đá (2700 kg) lên 5,0 m — 13,2 mana`.

- [ ] **Step 1: Viết GameTest fail**
  - `liftZombie10m`: zombie, 10 m, 3 giây → sau 30 tick, Y tăng ≥ 9; mana giảm ≈ 0,69.
  - `liftStoneBlock`: đá, 5 m → ô cũ thành không khí, có `FallingBlockEntity`; mana giảm ≈ 13,2.
  - `droppedBlockHurts`: nâng đá 3 m ngay trên đầu một zombie, 1 giây → zombie mất máu.
  - `cannotLiftChestOrBedrock`: nhắm vào rương có đồ, rồi vào đá nền → cả hai giữ nguyên, rương còn đủ đồ, không trừ mana.
- [ ] **Step 2–5:** như Task 4. Commit `feat: phép Nâng`.

### Task 11: Trang sách cho 7 phép, hướng dẫn, kiểm tra cuối

**Files:**
- Modify: `lang/vi_vn.json`, `lang/en_us.json` (tên, công thức, giải thích, luật cho 7 phép, tên lựa chọn, đơn vị kV/T/nm/W/giây/m)
- Modify: `D:\code_base\New folder\HUONG_DAN.md` (thêm phần 7 phép mới, ví dụ cách dùng an toàn nhưng **không ghi đáp số**)

- [ ] **Step 1:** Viết lời văn tiếng Việt cho 7 trang phép từ spec 8.3–8.9: nguyên tố, công thức, ý nghĩa vật lý, luật thành công/thất bại/phản tác dụng, các thao tác. Không in đáp số ví dụ.
- [ ] **Step 2:** Chạy `g.cmd build`, `g.cmd test`, `g.cmd runGameTestServer`. Kỳ vọng: tất cả thành công, kể cả mọi test của kế hoạch 1.
- [ ] **Step 3:** Chạy `g.cmd runClient`, thế giới sinh tồn mới: khắc lần lượt cả 9 phép vào trượng (mỗi lần 5 ô), thi triển mỗi phép ít nhất một lần với mỗi thao tác chính của nó. Kiểm tra: không crash, dòng kết quả đúng định dạng, sách hiện đủ 9 trang nhóm theo 7 nguyên tố.
- [ ] **Step 4:** Commit `docs: trang sách và hướng dẫn cho 7 phép`.
