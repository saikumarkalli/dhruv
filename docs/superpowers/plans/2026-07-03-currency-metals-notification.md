# Currency Accuracy + Gold/Silver + Configurable Daily Rate Notification — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade the Finance currency feature with accurate keyless FX + gold/silver spot data, an honest on-screen freshness/source indicator, a UI/UX refresh, and an opt-in daily notification (configurable time) reporting USD→INR + gold + silver with day-over-day change.

**Architecture:** Keyless data only (Frankfurter/ECB for FX + Fawaz Ahmed's currency-api for FX-fallback & metals `xau`/`xag`), self-healing to the existing Room cache. A keyed daily-snapshot table powers day-over-day deltas. A WorkManager `PeriodicWorkRequest` (wired through Koin) fires at the user-chosen time and posts a local notification. All new logic lives in `:apps:finance:data` (pure/data) and `:apps:finance:feature:currency` (Worker/notifier/scheduler/UI); settings state lives in `:libs:settings`; the app shell (`:apps:finance:app`) does buildConfig, Koin/manifest wiring, the Settings UI, and the runtime permission request.

**Tech Stack:** Kotlin, Jetpack Compose (Material3), Koin DI, Room, Retrofit+Moshi+OkHttp, WorkManager, DataStore, JUnit4 + kotlinx-coroutines-test + Turbine, JVM fakes (no Robolectric SQLite).

**Spec:** `docs/superpowers/specs/2026-07-03-currency-realtime-rates-daily-notification-design.md`

## Global Constraints

- **DI = Koin only** (no Hilt — AGP 9 incompatible, ADR-0010). Each feature exposes a `module {}`; app aggregates in `CalculatorApplication`.
- **No secrets/keys** in repo or APK (GitLeaks gates CI). All data sources are **keyless**.
- **Module boundaries (ArchUnit `DependencyRulesTest`)**: `feature → feature` FORBIDDEN; `feature → data` via repository only; `core → app` FORBIDDEN. `feature:currency` may depend on `:data`, `:libs:core`, `:libs:settings` (all already declared).
- **Every route wrapped in `FeatureHost`**; VM does `crashReporter.setModule("currency")`, exposes `featureError: StateFlow<Throwable?>` set by a `CoroutineExceptionHandler`, and wraps one op in `performanceTracer.trace("currency_…")`.
- **Compose Screen→UiState→Content + Koin `viewModel {}`** pattern; Compose/Material3 only; no web/React.
- **Design system from `:libs:core`**: use `MaterialTheme` tokens only — respect `AppTheme` (dark/light/system), user accent (`#D4AF37` default), `DhruvFont`. Never hardcode colors/fonts.
- **Preserve existing `testTag`s**: `currency_from_btn`, `currency_to_btn`, `currency_input_field`, `currency_swap_btn`, `currency_output_val`.
- **Money math uses `Double` only for display of already-authoritative rates** (rates arrive as `Double` from the APIs); metal gram conversion constant `GRAMS_PER_TROY_OZ = 31.1034768`.
- **No new DPDP consent gate** (public GET, local notification). Only new permission: `POST_NOTIFICATIONS`, requested at opt-in, SDK-guarded (minSdk 26).
- **Tests must not use real Room** (Robolectric SQLite fails on the Windows host — ADR-0013). Use the JVM fakes in `apps/finance/data/src/test/java/com/dhruv/finance/data/Fakes.kt`.
- **Metal values are international spot**, labelled as such (not local retail).
- **Build:** `JAVA_HOME` = Android Studio JBR. Gate: `./gradlew regressionCheck`.
- **Commits:** end message body with `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>`. Do not push unless asked.

---

## File Structure

**`:apps:finance:data`** (pure/data)
- `RateSnapshotEntity.kt` — `rate_snapshots` table (keyed daily series).
- `RateSnapshotDao.kt` — upsert / latest-before-date.
- `AppDatabase.kt` (modify) — add entity, version 5→6, `MIGRATION_5_6`.
- `api/CurrencyApi.kt` (rewrite) — Frankfurter + Fawaz DTOs + `CurrencyApiClient`.
- `RateMeta.kt` — source/freshness/quota value type.
- `model/DailyRate.kt` — `Delta`, `MetalSpot`, `MetalQuote`, `DailyRateReport`, `CurrencyResult`.
- `MetalPricing.kt` — pure spot derivation.
- `RateDelta.kt` — pure `computeDelta`.
- `CurrencyRepository.kt` (modify) + `ICurrencyRepository.kt` (modify) — `fetchLatest`, `buildDailyReport`, `getDailyReport`.
- `Fakes.kt` (modify) — add `FakeRateSnapshotDao`.

**`:libs:settings`**
- `AppSettings.kt` (modify) — 5 new fields.
- `SettingsKeys.kt` (modify) — 5 new keys.
- `SettingsRepositoryImpl.kt` (modify) — map + persist in `observe`/`update`.

**`:apps:finance:feature:currency`**
- `notify/DailyRateNotificationFormatter.kt` — pure title/body builder.
- `notify/RateNotifier.kt` — Android notification channel + post.
- `notify/DailyRateUseCase.kt` — orchestration (fetch→format→notify).
- `notify/DailyRateWorker.kt` — `CoroutineWorker`.
- `notify/ScheduleMath.kt` — pure initial-delay math.
- `notify/DailyRateScheduler.kt` — enqueue/cancel WorkManager job.
- `di/CurrencyModule.kt` (modify) — bind notifier/use-case/scheduler/worker.
- `CurrencyViewModel.kt` (modify) — expose `RateMeta`, gold/silver quotes, `selectedMetal`.
- `CurrencyScreen.kt` (modify) — freshness line, metal cards, detail sheet (ui-ux-pro-max).
- `build.gradle.kts` (modify) — WorkManager + koin-workmanager deps.

**`:apps:finance:app`**
- `build.gradle.kts` (modify) — new buildConfig URLs.
- `AndroidManifest.xml` (modify) — `POST_NOTIFICATIONS` + remove default WM initializer.
- `CalculatorApplication.kt` (modify) — `workManagerFactory()`.
- `di/PlatformModule.kt` (modify) — new `CurrencyApiClient` args; snapshot DAO via `dataModule`.
- `di/DataModule` binding (in `:data`) — expose `rateSnapshotDao()`.
- Settings UI section (new composable) + `SettingsViewModel` wiring + permission request + `MainActivity` re-ensure schedule.

**Catalog**
- `gradle/libs.versions.toml` (modify) — `work-runtime-ktx`, `koin-workmanager`.

---

# PHASE A — FX accuracy, freshness, configurable notification

## Task A1: Keyed daily-snapshot table (entity + DAO + migration + fake)

**Files:**
- Create: `apps/finance/data/src/main/java/com/dhruv/finance/data/RateSnapshotEntity.kt`
- Create: `apps/finance/data/src/main/java/com/dhruv/finance/data/RateSnapshotDao.kt`
- Modify: `apps/finance/data/src/main/java/com/dhruv/finance/data/AppDatabase.kt`
- Modify: `apps/finance/data/src/test/java/com/dhruv/finance/data/Fakes.kt`
- Test: `apps/finance/data/src/test/java/com/dhruv/finance/data/RateSnapshotDaoFakeTest.kt`

**Interfaces:**
- Produces: `RateSnapshotEntity(date: String, series: String, value: Double)`;
  `object RateSeries { const val USD_INR="USD_INR"; const val XAU_USD_OZ="XAU_USD_OZ"; const val XAG_USD_OZ="XAG_USD_OZ" }`;
  `RateSnapshotDao.upsert(rows: List<RateSnapshotEntity>)`, `latestBefore(series: String, date: String): RateSnapshotEntity?`, `getByDate(series: String, date: String): RateSnapshotEntity?`.

- [ ] **Step 1: Write the failing test**

`RateSnapshotDaoFakeTest.kt`:
```kotlin
package com.dhruv.finance.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RateSnapshotDaoFakeTest {
    private fun dao() = FakeRateSnapshotDao()

    @Test
    fun upsertReplacesSameDateAndSeries() = runTest {
        val dao = dao()
        dao.upsert(listOf(RateSnapshotEntity("2026-07-01", RateSeries.USD_INR, 95.0)))
        dao.upsert(listOf(RateSnapshotEntity("2026-07-01", RateSeries.USD_INR, 95.4)))
        assertEquals(95.4, dao.getByDate(RateSeries.USD_INR, "2026-07-01")!!.value, 0.0)
    }

    @Test
    fun latestBeforeReturnsMostRecentEarlierRow() = runTest {
        val dao = dao()
        dao.upsert(
            listOf(
                RateSnapshotEntity("2026-06-29", RateSeries.USD_INR, 94.0),
                RateSnapshotEntity("2026-06-30", RateSeries.USD_INR, 95.0),
                RateSnapshotEntity("2026-07-01", RateSeries.USD_INR, 96.0),
            ),
        )
        assertEquals(95.0, dao.latestBefore(RateSeries.USD_INR, "2026-07-01")!!.value, 0.0)
        assertNull(dao.latestBefore(RateSeries.USD_INR, "2026-06-29"))
    }

    @Test
    fun latestBeforeIsSeriesScoped() = runTest {
        val dao = dao()
        dao.upsert(
            listOf(
                RateSnapshotEntity("2026-06-30", RateSeries.XAU_USD_OZ, 2500.0),
                RateSnapshotEntity("2026-06-30", RateSeries.USD_INR, 95.0),
            ),
        )
        assertEquals(2500.0, dao.latestBefore(RateSeries.XAU_USD_OZ, "2026-07-01")!!.value, 0.0)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :apps:finance:data:testDebugUnitTest --tests "com.dhruv.finance.data.RateSnapshotDaoFakeTest"`
Expected: FAIL — `FakeRateSnapshotDao`, `RateSnapshotEntity`, `RateSeries` unresolved.

- [ ] **Step 3: Create the entity + DAO**

`RateSnapshotEntity.kt`:
```kotlin
package com.dhruv.finance.data

import androidx.room.Entity

/** One stored daily value per (date, series) — powers day-over-day deltas. date = ISO-8601 (yyyy-MM-dd, UTC). */
@Entity(tableName = "rate_snapshots", primaryKeys = ["date", "series"])
data class RateSnapshotEntity(
    val date: String,
    val series: String,
    val value: Double,
)

object RateSeries {
    const val USD_INR = "USD_INR"
    const val XAU_USD_OZ = "XAU_USD_OZ"
    const val XAG_USD_OZ = "XAG_USD_OZ"
}
```

`RateSnapshotDao.kt`:
```kotlin
package com.dhruv.finance.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RateSnapshotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rows: List<RateSnapshotEntity>)

    @Query("SELECT * FROM rate_snapshots WHERE series = :series AND date < :date ORDER BY date DESC LIMIT 1")
    suspend fun latestBefore(series: String, date: String): RateSnapshotEntity?

    @Query("SELECT * FROM rate_snapshots WHERE series = :series AND date = :date LIMIT 1")
    suspend fun getByDate(series: String, date: String): RateSnapshotEntity?
}
```

- [ ] **Step 4: Add the fake to `Fakes.kt`**

Append to `apps/finance/data/src/test/java/com/dhruv/finance/data/Fakes.kt`:
```kotlin
class FakeRateSnapshotDao : RateSnapshotDao {
    private val store = LinkedHashMap<Pair<String, String>, RateSnapshotEntity>() // (date,series) -> row

    override suspend fun upsert(rows: List<RateSnapshotEntity>) {
        rows.forEach { store[it.date to it.series] = it }
    }

    override suspend fun latestBefore(series: String, date: String): RateSnapshotEntity? =
        store.values
            .filter { it.series == series && it.date < date }
            .maxByOrNull { it.date }

    override suspend fun getByDate(series: String, date: String): RateSnapshotEntity? =
        store[date to series]
}
```

- [ ] **Step 5: Register entity + migration in `AppDatabase.kt`**

Change the `@Database` annotation and add the DAO accessor + migration:
```kotlin
@Database(
    entities = [HistoryEntity::class, CurrencyRateEntity::class, RateSnapshotEntity::class],
    version = 6,
    exportSchema = false,
)
```
Add accessor inside the class:
```kotlin
    abstract fun rateSnapshotDao(): RateSnapshotDao
```
Add migration in the companion (next to `MIGRATION_4_5`):
```kotlin
        val MIGRATION_5_6 =
            object : Migration(5, 6) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `rate_snapshots` (`date` TEXT NOT NULL, `series` TEXT NOT NULL, `value` REAL NOT NULL, PRIMARY KEY(`date`, `series`))",
                    )
                }
            }
```
Add it to the builder chain:
```kotlin
                        .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `./gradlew :apps:finance:data:testDebugUnitTest --tests "com.dhruv.finance.data.RateSnapshotDaoFakeTest"`
Expected: PASS (3 tests).

- [ ] **Step 7: Commit**

```bash
git add apps/finance/data/src/main/java/com/dhruv/finance/data/RateSnapshotEntity.kt \
        apps/finance/data/src/main/java/com/dhruv/finance/data/RateSnapshotDao.kt \
        apps/finance/data/src/main/java/com/dhruv/finance/data/AppDatabase.kt \
        apps/finance/data/src/test/java/com/dhruv/finance/data/Fakes.kt \
        apps/finance/data/src/test/java/com/dhruv/finance/data/RateSnapshotDaoFakeTest.kt
git commit -m "feat(data): add keyed daily rate_snapshots table (migration 5->6)"
```

---

## Task A2: Pure day-over-day delta

**Files:**
- Create: `apps/finance/data/src/main/java/com/dhruv/finance/data/RateDelta.kt`
- Create: `apps/finance/data/src/main/java/com/dhruv/finance/data/model/DailyRate.kt`
- Test: `apps/finance/data/src/test/java/com/dhruv/finance/data/RateDeltaTest.kt`

**Interfaces:**
- Produces: `data class Delta(current: Double, previous: Double?, absChange: Double?, pctChange: Double?)`;
  `fun computeDelta(current: Double, previous: Double?): Delta`.
  Also the shared model holders `MetalSpot`, `MetalQuote`, `DailyRateReport`, `CurrencyResult` used by later tasks.

- [ ] **Step 1: Write the failing test**

`RateDeltaTest.kt`:
```kotlin
package com.dhruv.finance.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RateDeltaTest {
    @Test
    fun riseComputesPositiveChange() {
        val d = computeDelta(current = 95.24, previous = 95.12)
        assertEquals(0.12, d.absChange!!, 1e-9)
        assertEquals(0.1261, d.pctChange!!, 1e-3)
    }

    @Test
    fun fallComputesNegativeChange() {
        val d = computeDelta(current = 94.0, previous = 95.0)
        assertEquals(-1.0, d.absChange!!, 1e-9)
        assertEquals(-1.0526, d.pctChange!!, 1e-3)
    }

    @Test
    fun missingPreviousLeavesChangesNull() {
        val d = computeDelta(current = 95.0, previous = null)
        assertNull(d.absChange)
        assertNull(d.pctChange)
        assertEquals(95.0, d.current, 0.0)
    }

    @Test
    fun zeroPreviousAvoidsDivideByZero() {
        val d = computeDelta(current = 95.0, previous = 0.0)
        assertEquals(95.0, d.absChange!!, 0.0)
        assertNull(d.pctChange) // undefined %; omitted
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :apps:finance:data:testDebugUnitTest --tests "com.dhruv.finance.data.RateDeltaTest"`
Expected: FAIL — `computeDelta`/`Delta` unresolved.

- [ ] **Step 3: Create model holders + delta fn**

`model/DailyRate.kt`:
```kotlin
package com.dhruv.finance.data.model

import java.time.LocalDate

/** International spot price of one metal, derived from a keyless source. */
data class MetalSpot(
    val perGramInr: Double,
    val per10gInr: Double,
    val perOzUsd: Double,
    val perOzInr: Double,
)

/** A metal's spot plus its day-over-day delta on the per-10g INR value. */
data class MetalQuote(
    val spot: MetalSpot,
    val per10gDelta: com.dhruv.finance.data.Delta,
)

/** Everything the screen cards + daily notification need. gold/silver null when metals unavailable. */
data class DailyRateReport(
    val usdInr: com.dhruv.finance.data.Delta,
    val gold: MetalQuote?,
    val silver: MetalQuote?,
    val rateDate: LocalDate?,
)

/** One FX fetch result: full rate map (base = USD) + provenance. */
data class CurrencyResult(
    val rates: Map<String, Double>,
    val meta: com.dhruv.finance.data.RateMeta,
)
```

`RateDelta.kt`:
```kotlin
package com.dhruv.finance.data

/** Day-over-day change of a value. changes are null when [previous] is null. pctChange null if previous == 0. */
data class Delta(
    val current: Double,
    val previous: Double?,
    val absChange: Double?,
    val pctChange: Double?,
)

fun computeDelta(current: Double, previous: Double?): Delta {
    if (previous == null) return Delta(current, null, null, null)
    val abs = current - previous
    val pct = if (previous == 0.0) null else (abs / previous) * 100.0
    return Delta(current, previous, abs, pct)
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :apps:finance:data:testDebugUnitTest --tests "com.dhruv.finance.data.RateDeltaTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add apps/finance/data/src/main/java/com/dhruv/finance/data/RateDelta.kt \
        apps/finance/data/src/main/java/com/dhruv/finance/data/model/DailyRate.kt \
        apps/finance/data/src/test/java/com/dhruv/finance/data/RateDeltaTest.kt
git commit -m "feat(data): add pure day-over-day delta + daily-rate model holders"
```

---

## Task A3: Keyless API clients (Frankfurter primary, Fawaz fallback) + buildConfig URLs

**Files:**
- Rewrite: `apps/finance/data/src/main/java/com/dhruv/finance/data/api/CurrencyApi.kt`
- Create: `apps/finance/data/src/main/java/com/dhruv/finance/data/RateMeta.kt`
- Modify: `apps/finance/app/build.gradle.kts:18-25`
- Test: `apps/finance/data/src/test/java/com/dhruv/finance/data/api/CurrencyDtoTest.kt`

**Interfaces:**
- Produces: `data class RateMeta(sourceName, rateDate: LocalDate?, fetchedAt: Long, quotaLimit: Int? = null, quotaRemaining: Int? = null)`;
  `FrankfurterResponse(base: String, date: String, rates: Map<String, Double>)`;
  `FawazResponse(date: String, usd: Map<String, Double>)`;
  `interface FrankfurterApi { suspend fun latest(base): FrankfurterResponse }`;
  `interface FawazApi { suspend fun usd(): FawazResponse }`;
  `class CurrencyApiClient(frankfurterBaseUrl, fawazBaseUrl, timeoutSeconds, userAgent)` exposing `frankfurter: FrankfurterApi`, `fawaz: FawazApi`.

- [ ] **Step 1: Write the failing test**

`api/CurrencyDtoTest.kt` (Moshi parse tests — pure JVM, no network):
```kotlin
package com.dhruv.finance.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyDtoTest {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @Test
    fun parsesFrankfurter() {
        val json = """{"amount":1.0,"base":"USD","date":"2026-07-02","rates":{"INR":95.39,"EUR":0.92}}"""
        val r = moshi.adapter(FrankfurterResponse::class.java).fromJson(json)!!
        assertEquals("2026-07-02", r.date)
        assertEquals(95.39, r.rates["INR"]!!, 0.0)
    }

    @Test
    fun parsesFawazWithMetals() {
        val json = """{"date":"2026-07-02","usd":{"inr":95.39,"xau":0.0004,"xag":0.033}}"""
        val r = moshi.adapter(FawazResponse::class.java).fromJson(json)!!
        assertEquals("2026-07-02", r.date)
        assertEquals(0.0004, r.usd["xau"]!!, 0.0)
        assertEquals(95.39, r.usd["inr"]!!, 0.0)
    }
}
```
> Note: `:data` already depends on Moshi via `converter-moshi`; if `moshi-kotlin` (reflection) isn't on the `:data` test classpath, add `testImplementation(libs.moshi.kotlin)` to `apps/finance/data/build.gradle.kts` in this step. Production DTOs use `@JsonClass(generateAdapter = true)` (KSP codegen already configured in `:data`), so runtime parsing does not rely on reflection.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :apps:finance:data:testDebugUnitTest --tests "com.dhruv.finance.data.api.CurrencyDtoTest"`
Expected: FAIL — `FrankfurterResponse`/`FawazResponse` unresolved.

- [ ] **Step 3: Create `RateMeta.kt`**

```kotlin
package com.dhruv.finance.data

import java.time.LocalDate

/**
 * Provenance/freshness for a fetched rate set. quota* stay null for keyless sources (Frankfurter/Fawaz);
 * they exist so the UI can render a real quota if a keyed source is ever added (spec §2 D2).
 */
data class RateMeta(
    val sourceName: String,
    val rateDate: LocalDate?,
    val fetchedAt: Long,
    val quotaLimit: Int? = null,
    val quotaRemaining: Int? = null,
)
```

- [ ] **Step 4: Rewrite `api/CurrencyApi.kt`**

```kotlin
package com.dhruv.finance.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

/** Frankfurter (ECB reference rates). https://api.frankfurter.dev/v1/latest?base=USD */
@JsonClass(generateAdapter = true)
data class FrankfurterResponse(
    @param:Json(name = "base") val base: String,
    @param:Json(name = "date") val date: String,
    @param:Json(name = "rates") val rates: Map<String, Double>,
)

/** Fawaz Ahmed currency-api. .../v1/currencies/usd.json  -> { date, usd: { inr, xau, xag, ... } } */
@JsonClass(generateAdapter = true)
data class FawazResponse(
    @param:Json(name = "date") val date: String,
    @param:Json(name = "usd") val usd: Map<String, Double>,
)

interface FrankfurterApi {
    @GET("v1/latest")
    suspend fun latest(
        @retrofit2.http.Query("base") base: String,
    ): FrankfurterResponse
}

interface FawazApi {
    // Path kept flexible so the base URL can point at jsDelivr or the Cloudflare mirror.
    @GET("{path}")
    suspend fun usd(
        @Path(value = "path", encoded = true) path: String = "npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.min.json",
    ): FawazResponse
}

class CurrencyApiClient(
    frankfurterBaseUrl: String,
    fawazBaseUrl: String,
    timeoutSeconds: Long,
    userAgent: String,
) {
    private val okHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request =
                    chain.request().newBuilder()
                        .header("User-Agent", userAgent)
                        .header("Accept", "application/json")
                        .build()
                chain.proceed(request)
            }.build()

    private fun retrofit(baseUrl: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

    val frankfurter: FrankfurterApi = retrofit(frankfurterBaseUrl).create(FrankfurterApi::class.java)
    val fawaz: FawazApi = retrofit(fawazBaseUrl).create(FawazApi::class.java)
}
```

- [ ] **Step 5: Update buildConfig URLs in `apps/finance/app/build.gradle.kts`**

Replace lines 18–19 and keep timeout/user-agent:
```kotlin
        buildConfigField("String", "CURRENCY_API_BASE_URL", "\"https://api.frankfurter.dev/\"")
        buildConfigField("String", "CURRENCY_API_FALLBACK_BASE_URL", "\"https://cdn.jsdelivr.net/\"")
```
(The Fawaz path is baked into `FawazApi.usd()`; the base URL is the jsDelivr CDN root. `CURRENCY_API_TIMEOUT_SECONDS`/`CURRENCY_API_USER_AGENT` are unchanged.)

- [ ] **Step 6: Run tests to verify they pass**

Run: `./gradlew :apps:finance:data:testDebugUnitTest --tests "com.dhruv.finance.data.api.CurrencyDtoTest"`
Expected: PASS (2 tests).

- [ ] **Step 7: Commit**

```bash
git add apps/finance/data/src/main/java/com/dhruv/finance/data/api/CurrencyApi.kt \
        apps/finance/data/src/main/java/com/dhruv/finance/data/RateMeta.kt \
        apps/finance/data/build.gradle.kts apps/finance/app/build.gradle.kts \
        apps/finance/data/src/test/java/com/dhruv/finance/data/api/CurrencyDtoTest.kt
git commit -m "feat(data): keyless Frankfurter+Fawaz API clients + RateMeta"
```

---

## Task A4: Repository — `fetchLatest`, `buildDailyReport` (FX-only), `getDailyReport`

**Files:**
- Modify: `apps/finance/data/src/main/java/com/dhruv/finance/data/ICurrencyRepository.kt`
- Modify: `apps/finance/data/src/main/java/com/dhruv/finance/data/CurrencyRepository.kt`
- Modify: `apps/finance/data/src/test/java/com/dhruv/finance/data/CurrencyRepositoryTest.kt`
- Modify: `apps/finance/feature/currency/src/test/java/com/dhruv/finance/currency/CurrencyViewModelTest.kt` (fake must implement new methods)
- Test: `apps/finance/data/src/test/java/com/dhruv/finance/data/CurrencyReportTest.kt`

**Interfaces:**
- Consumes: `CurrencyResult`, `RateMeta`, `Delta`, `DailyRateReport` (A2/A3), `RateSnapshotDao`/`RateSeries` (A1), `CurrencyApiClient` (A3).
- Produces (added to `ICurrencyRepository`):
  - `suspend fun fetchLatest(baseCurrency: String = "USD"): Result<CurrencyResult>`
  - `suspend fun buildDailyReport(result: CurrencyResult): DailyRateReport`
  - `suspend fun getDailyReport(baseCurrency: String = "USD"): Result<DailyRateReport>`
  - `CurrencyRepository` constructor gains `rateSnapshotDao: RateSnapshotDao` and a `clock: () -> Long = System::currentTimeMillis`.

- [ ] **Step 1: Write the failing test**

`CurrencyReportTest.kt` (uses fakes + a fake `CurrencyApiClient` via a seam — see Step 3 note):
```kotlin
package com.dhruv.finance.data

import com.dhruv.finance.data.model.CurrencyResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class CurrencyReportTest {
    private fun repo(snap: FakeRateSnapshotDao = FakeRateSnapshotDao()) =
        CurrencyRepository(
            currencyRateDao = FakeCurrencyRateDao(),
            snapshotDao = snap,
            fxFetcher = { Result.success(mapOf("INR" to 95.4, "EUR" to 0.92) to LocalDate.parse("2026-07-02")) },
            metalFetcher = { Result.success(null) }, // Phase A: no metals
            clock = { 1_720_000_000_000L },
        )

    @Test
    fun buildsUsdInrDeltaAgainstYesterdaySnapshot() = runTest {
        val snap = FakeRateSnapshotDao()
        snap.upsert(listOf(RateSnapshotEntity("2026-07-01", RateSeries.USD_INR, 95.0)))
        val r = repo(snap)
        val result = r.fetchLatest("USD").getOrThrow()
        val report = r.buildDailyReport(result)
        assertEquals(95.4, report.usdInr.current, 1e-9)
        assertEquals(0.4, report.usdInr.absChange!!, 1e-9)
        assertNull(report.gold)
        assertEquals(LocalDate.parse("2026-07-02"), report.rateDate)
    }

    @Test
    fun writesTodaySnapshotSoNextRunHasPrevious() = runTest {
        val snap = FakeRateSnapshotDao()
        val r = repo(snap)
        r.buildDailyReport(r.fetchLatest("USD").getOrThrow())
        // 1_720_000_000_000L -> 2024-07-03 UTC; assert snapshot written under that date key
        val today = java.time.Instant.ofEpochMilli(1_720_000_000_000L).atZone(java.time.ZoneOffset.UTC).toLocalDate().toString()
        assertEquals(95.4, snap.getByDate(RateSeries.USD_INR, today)!!.value, 1e-9)
    }
}
```
> Design seam: to keep the repository unit-testable without Retrofit, `CurrencyRepository` takes two suspend function seams injected by Koin in production from `CurrencyApiClient`:
> `fxFetcher: suspend (base: String) -> Result<Pair<Map<String, Double>, LocalDate?>>` and
> `metalFetcher: suspend () -> Result<com.dhruv.finance.data.MetalSpots?>` (MetalSpots defined in Phase B; in Phase A the production `metalFetcher` returns `Result.success(null)`).

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :apps:finance:data:testDebugUnitTest --tests "com.dhruv.finance.data.CurrencyReportTest"`
Expected: FAIL — new constructor params/methods unresolved.

- [ ] **Step 3: Extend `ICurrencyRepository.kt`**

Add to the interface (keep the existing three methods for back-compat):
```kotlin
    /** One FX fetch (Frankfurter -> Fawaz -> cache) returning the rate map + provenance. */
    suspend fun fetchLatest(baseCurrency: String = "USD"): Result<CurrencyResult>

    /** Computes USD->INR (+ metals in Phase B) deltas vs snapshots, writes today's snapshots. */
    suspend fun buildDailyReport(result: CurrencyResult): DailyRateReport

    /** Convenience for the daily worker: fetchLatest then buildDailyReport. */
    suspend fun getDailyReport(baseCurrency: String = "USD"): Result<DailyRateReport>
```
Add imports: `import com.dhruv.finance.data.model.CurrencyResult`, `import com.dhruv.finance.data.model.DailyRateReport`.

- [ ] **Step 4: Implement in `CurrencyRepository.kt`**

Update the constructor and add methods (keep existing `getAllRates`/`getRate`/`fetchAndCacheLatestRates`):
```kotlin
class CurrencyRepository(
    private val currencyRateDao: CurrencyRateDao,
    private val snapshotDao: RateSnapshotDao,
    private val fxFetcher: suspend (base: String) -> Result<Pair<Map<String, Double>, java.time.LocalDate?>>,
    private val metalFetcher: suspend () -> Result<MetalSpots?>,
    private val clock: () -> Long = System::currentTimeMillis,
) : ICurrencyRepository {

    private fun todayUtc(): String =
        java.time.Instant.ofEpochMilli(clock()).atZone(java.time.ZoneOffset.UTC).toLocalDate().toString()

    override suspend fun fetchLatest(baseCurrency: String): Result<CurrencyResult> =
        withContext(Dispatchers.IO) {
            fxFetcher(baseCurrency).map { (rates, date) ->
                if (rates.isNotEmpty()) {
                    currencyRateDao.clearAllRates()
                    currencyRateDao.insertRates(rates.map { (c, r) -> CurrencyRateEntity(c, r, clock()) })
                }
                CurrencyResult(rates = rates, meta = RateMeta("Frankfurter (ECB)", date, clock()))
            }.recoverCatching {
                val cached = currencyRateDao.getAllRates()
                if (cached.isEmpty()) throw it
                CurrencyResult(cached.associate { e -> e.currencyCode to e.rate }, RateMeta("Cache", null, cached.first().timestamp))
            }
        }

    override suspend fun buildDailyReport(result: CurrencyResult): DailyRateReport =
        withContext(Dispatchers.IO) {
            val today = todayUtc()
            val usdInrNow = result.rates["INR"] ?: currencyRateDao.getRateByCode("INR")?.rate ?: 0.0
            val prevInr = snapshotDao.latestBefore(RateSeries.USD_INR, today)?.value
            val toWrite = mutableListOf(RateSnapshotEntity(today, RateSeries.USD_INR, usdInrNow))

            val metals = metalFetcher().getOrNull()
            val gold = metals?.let { m ->
                val prev = snapshotDao.latestBefore(RateSeries.XAU_USD_OZ, today)?.value
                toWrite += RateSnapshotEntity(today, RateSeries.XAU_USD_OZ, m.gold.perOzUsd)
                MetalQuote(m.gold, computeDelta(m.gold.per10gInr, prev?.let { m.gold.per10gInr /* placeholder */ }))
            }
            val silver = metals?.let { m ->
                val prev = snapshotDao.latestBefore(RateSeries.XAG_USD_OZ, today)?.value
                toWrite += RateSnapshotEntity(today, RateSeries.XAG_USD_OZ, m.silver.perOzUsd)
                MetalQuote(m.silver, computeDelta(m.silver.per10gInr, prev?.let { m.silver.per10gInr /* placeholder */ }))
            }
            snapshotDao.upsert(toWrite)
            DailyRateReport(
                usdInr = computeDelta(usdInrNow, prevInr),
                gold = gold,
                silver = silver,
                rateDate = result.meta.rateDate,
            )
        }

    override suspend fun getDailyReport(baseCurrency: String): Result<DailyRateReport> =
        fetchLatest(baseCurrency).mapCatching { buildDailyReport(it) }
```
> The `/* placeholder */` metal-delta wiring is **completed in Task B2** (which stores per-10g-derived previous values correctly). In Phase A `metalFetcher` returns `null`, so `gold`/`silver` stay null and these branches never execute — the tests in this task assert exactly that.
Add imports at top: `import com.dhruv.finance.data.model.CurrencyResult`, `import com.dhruv.finance.data.model.DailyRateReport`, `import com.dhruv.finance.data.model.MetalQuote`.

- [ ] **Step 5: Fix the two existing fakes to implement the new methods**

In `CurrencyViewModelTest.kt`'s inner `FakeCurrencyRepository`, add:
```kotlin
        override suspend fun fetchLatest(baseCurrency: String) =
            Result.success(com.dhruv.finance.data.model.CurrencyResult(rates, com.dhruv.finance.data.RateMeta("Test", null, 0L)))

        override suspend fun buildDailyReport(result: com.dhruv.finance.data.model.CurrencyResult) =
            com.dhruv.finance.data.model.DailyRateReport(com.dhruv.finance.data.computeDelta(rates["INR"] ?: 0.0, null), null, null, null)

        override suspend fun getDailyReport(baseCurrency: String) =
            Result.success(buildDailyReport(fetchLatest(baseCurrency).getOrThrow()))
```
In `CurrencyRepositoryTest.kt`, update the `CurrencyRepository(...)` constructor call to the new 5-arg signature (pass `FakeRateSnapshotDao()`, an `fxFetcher` returning the test's rates, `metalFetcher = { Result.success(null) }`, and a fixed `clock`). Keep the existing `fetchAndCacheLatestRates` assertions valid (that method is unchanged; if it still references `apiClient`, keep an overloaded constructor OR migrate those tests to `fetchLatest` — prefer migrating them to `fetchLatest`).

- [ ] **Step 6: Provide the `MetalSpots` placeholder type (filled in Phase B)**

Create `apps/finance/data/src/main/java/com/dhruv/finance/data/MetalSpots.kt`:
```kotlin
package com.dhruv.finance.data

import com.dhruv.finance.data.model.MetalSpot

/** Gold+silver spot pair from the keyless metal source (Fawaz xau/xag). */
data class MetalSpots(val gold: MetalSpot, val silver: MetalSpot)
```

- [ ] **Step 7: Run tests to verify they pass**

Run: `./gradlew :apps:finance:data:testDebugUnitTest`
Expected: PASS (new `CurrencyReportTest` + existing data tests green).

- [ ] **Step 8: Commit**

```bash
git add apps/finance/data/src/main/java/com/dhruv/finance/data/ICurrencyRepository.kt \
        apps/finance/data/src/main/java/com/dhruv/finance/data/CurrencyRepository.kt \
        apps/finance/data/src/main/java/com/dhruv/finance/data/MetalSpots.kt \
        apps/finance/data/src/test/java/com/dhruv/finance/data/CurrencyReportTest.kt \
        apps/finance/data/src/test/java/com/dhruv/finance/data/CurrencyRepositoryTest.kt \
        apps/finance/feature/currency/src/test/java/com/dhruv/finance/currency/CurrencyViewModelTest.kt
git commit -m "feat(data): repository fetchLatest + buildDailyReport + getDailyReport (FX)"
```

---

## Task A5: Pure notification formatter

**Files:**
- Create: `apps/finance/feature/currency/src/main/java/com/dhruv/finance/currency/notify/DailyRateNotificationFormatter.kt`
- Test: `apps/finance/feature/currency/src/test/java/com/dhruv/finance/currency/notify/DailyRateNotificationFormatterTest.kt`

**Interfaces:**
- Consumes: `DailyRateReport`, `MetalQuote`, `Delta` (A2).
- Produces: `data class NotificationText(title: String, body: String)`;
  `object DailyRateNotificationFormatter { fun format(report: DailyRateReport, includeGold: Boolean, includeSilver: Boolean): NotificationText }`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.dhruv.finance.currency.notify

import com.dhruv.finance.data.Delta
import com.dhruv.finance.data.model.DailyRateReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyRateNotificationFormatterTest {
    private fun report(inr: Delta, gold: com.dhruv.finance.data.model.MetalQuote? = null) =
        DailyRateReport(usdInr = inr, gold = gold, silver = null, rateDate = null)

    @Test
    fun titleShowsUsdInrWithUpArrowAndPct() {
        val t = DailyRateNotificationFormatter.format(
            report(Delta(95.39, 95.27, 0.12, 0.1259)), includeGold = false, includeSilver = false,
        )
        assertTrue(t.title.contains("1 USD = ₹95.39"))
        assertTrue(t.title.contains("▲"))
        assertTrue(t.title.contains("0.13%"))
    }

    @Test
    fun missingDeltaOmitsArrow() {
        val t = DailyRateNotificationFormatter.format(
            report(Delta(95.39, null, null, null)), includeGold = false, includeSilver = false,
        )
        assertEquals("1 USD = ₹95.39", t.title)
    }

    @Test
    fun bodyIncludesGoldWhenEnabledAndPresent() {
        val gold = com.dhruv.finance.data.model.MetalQuote(
            spot = com.dhruv.finance.data.model.MetalSpot(perGramInr = 7000.0, per10gInr = 70000.0, perOzUsd = 2500.0, perOzInr = 217650.0),
            per10gDelta = Delta(70000.0, 69800.0, 200.0, 0.286),
        )
        val t = DailyRateNotificationFormatter.format(report(Delta(95.39, 95.3, 0.09, 0.09), gold), includeGold = true, includeSilver = false)
        assertTrue(t.body.contains("Gold"))
        assertTrue(t.body.contains("70,000") || t.body.contains("₹70000") || t.body.contains("₹70,000"))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :apps:finance:feature:currency:testDebugUnitTest --tests "com.dhruv.finance.currency.notify.DailyRateNotificationFormatterTest"`
Expected: FAIL — formatter unresolved.

- [ ] **Step 3: Implement the formatter**

```kotlin
package com.dhruv.finance.currency.notify

import com.dhruv.finance.data.Delta
import com.dhruv.finance.data.model.DailyRateReport
import com.dhruv.finance.data.model.MetalQuote
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class NotificationText(val title: String, val body: String)

object DailyRateNotificationFormatter {
    private val two = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
    private val whole = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))

    private fun deltaSuffix(d: Delta): String {
        val abs = d.absChange ?: return ""
        val pct = d.pctChange
        val arrow = if (abs >= 0) "▲" else "▼"
        val pctStr = pct?.let { " (${if (it >= 0) "+" else ""}${two.format(it)}%)" } ?: ""
        return "  $arrow${two.format(kotlin.math.abs(abs))}$pctStr"
    }

    fun format(report: DailyRateReport, includeGold: Boolean, includeSilver: Boolean): NotificationText {
        val title = "1 USD = ₹${two.format(report.usdInr.current)}${deltaSuffix(report.usdInr)}"
        val lines = mutableListOf<String>()
        if (includeGold) report.gold?.let { lines += metalLine("Gold 24K", it) }
        if (includeSilver) report.silver?.let { lines += metalLine("Silver", it) }
        return NotificationText(title = title, body = lines.joinToString("\n").ifEmpty { title })
    }

    private fun metalLine(label: String, q: MetalQuote): String =
        "$label ₹${whole.format(q.spot.per10gInr)}/10g${deltaSuffix(q.per10gDelta)}"
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :apps:finance:feature:currency:testDebugUnitTest --tests "com.dhruv.finance.currency.notify.DailyRateNotificationFormatterTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add apps/finance/feature/currency/src/main/java/com/dhruv/finance/currency/notify/DailyRateNotificationFormatter.kt \
        apps/finance/feature/currency/src/test/java/com/dhruv/finance/currency/notify/DailyRateNotificationFormatterTest.kt
git commit -m "feat(currency): pure daily-rate notification formatter"
```

---

## Task A6: Pure schedule math + WorkManager scheduler + deps

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `apps/finance/feature/currency/build.gradle.kts`
- Create: `apps/finance/feature/currency/src/main/java/com/dhruv/finance/currency/notify/ScheduleMath.kt`
- Create: `apps/finance/feature/currency/src/main/java/com/dhruv/finance/currency/notify/DailyRateScheduler.kt`
- Test: `apps/finance/feature/currency/src/test/java/com/dhruv/finance/currency/notify/ScheduleMathTest.kt`

**Interfaces:**
- Produces: `object ScheduleMath { fun initialDelayMillis(nowMillis: Long, zone: java.time.ZoneId, hour: Int, minute: Int): Long }`;
  `class DailyRateScheduler(context: Context) { fun schedule(hour: Int, minute: Int); fun cancel() }`;
  work name constant `DailyRateScheduler.WORK_NAME = "dhruv_daily_rate_notification"`.

- [ ] **Step 1: Write the failing test**

`ScheduleMathTest.kt`:
```kotlin
package com.dhruv.finance.currency.notify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ScheduleMathTest {
    private val zone = ZoneId.of("Asia/Kolkata")

    @Test
    fun laterTodaySchedulesSameDay() {
        val now = ZonedDateTime.of(2026, 7, 3, 6, 0, 0, 0, zone).toInstant().toEpochMilli()
        val delay = ScheduleMath.initialDelayMillis(now, zone, hour = 9, minute = 0)
        assertEquals(3 * 60 * 60 * 1000L, delay) // 3 hours
    }

    @Test
    fun pastTimeSchedulesNextDay() {
        val now = ZonedDateTime.of(2026, 7, 3, 10, 0, 0, 0, zone).toInstant().toEpochMilli()
        val delay = ScheduleMath.initialDelayMillis(now, zone, hour = 9, minute = 0)
        assertEquals(23 * 60 * 60 * 1000L, delay) // next day 09:00
    }

    @Test
    fun delayIsNeverNegative() {
        val now = ZonedDateTime.of(2026, 7, 3, 9, 0, 0, 0, zone).toInstant().toEpochMilli()
        val delay = ScheduleMath.initialDelayMillis(now, zone, hour = 9, minute = 0)
        assertTrue(delay in 0..(24 * 60 * 60 * 1000L))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :apps:finance:feature:currency:testDebugUnitTest --tests "com.dhruv.finance.currency.notify.ScheduleMathTest"`
Expected: FAIL — `ScheduleMath` unresolved.

- [ ] **Step 3: Add catalog + module deps**

In `gradle/libs.versions.toml` `[versions]` add:
```toml
workRuntime = "2.9.1"
```
In `[libraries]` add:
```toml
androidx-work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "workRuntime" }
koin-workmanager = { group = "io.insert-koin", name = "koin-androidx-workmanager", version.ref = "koin" }
```
In `apps/finance/feature/currency/build.gradle.kts` `dependencies {}` add:
```kotlin
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.koin.workmanager)
```

- [ ] **Step 4: Implement `ScheduleMath.kt`**

```kotlin
package com.dhruv.finance.currency.notify

import java.time.Instant
import java.time.ZoneId

object ScheduleMath {
    /** Millis from [nowMillis] until the next [hour]:[minute] in [zone]. Always in [0, 24h). */
    fun initialDelayMillis(nowMillis: Long, zone: ZoneId, hour: Int, minute: Int): Long {
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return next.toInstant().toEpochMilli() - nowMillis
    }
}
```

- [ ] **Step 5: Implement `DailyRateScheduler.kt`**

```kotlin
package com.dhruv.finance.currency.notify

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class DailyRateScheduler(private val context: Context) {
    fun schedule(hour: Int, minute: Int) {
        val delay = ScheduleMath.initialDelayMillis(System.currentTimeMillis(), ZoneId.systemDefault(), hour, minute)
        val request =
            PeriodicWorkRequestBuilder<DailyRateWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    companion object {
        const val WORK_NAME = "dhruv_daily_rate_notification"
    }
}
```
> `DailyRateWorker` is created in Task A7; this file will not compile until then — implement A7 immediately after and run the module build once at A7 Step 6.

- [ ] **Step 6: Run the pure test to verify it passes**

Run: `./gradlew :apps:finance:feature:currency:testDebugUnitTest --tests "com.dhruv.finance.currency.notify.ScheduleMathTest"`
Expected: PASS (3 tests). (Module compile of `DailyRateScheduler` completes after A7.)

- [ ] **Step 7: Commit**

```bash
git add gradle/libs.versions.toml apps/finance/feature/currency/build.gradle.kts \
        apps/finance/feature/currency/src/main/java/com/dhruv/finance/currency/notify/ScheduleMath.kt \
        apps/finance/feature/currency/src/main/java/com/dhruv/finance/currency/notify/DailyRateScheduler.kt \
        apps/finance/feature/currency/src/test/java/com/dhruv/finance/currency/notify/ScheduleMathTest.kt
git commit -m "feat(currency): schedule math + WorkManager daily scheduler + deps"
```

---

## Task A7: Notifier + use-case + worker + Koin/manifest wiring

**Files:**
- Create: `apps/finance/feature/currency/src/main/java/com/dhruv/finance/currency/notify/RateNotifier.kt`
- Create: `apps/finance/feature/currency/src/main/java/com/dhruv/finance/currency/notify/DailyRateUseCase.kt`
- Create: `apps/finance/feature/currency/src/main/java/com/dhruv/finance/currency/notify/DailyRateWorker.kt`
- Modify: `apps/finance/feature/currency/src/main/java/com/dhruv/finance/currency/di/CurrencyModule.kt`
- Modify: `apps/finance/app/src/main/java/com/dhruv/finance/app/CalculatorApplication.kt`
- Modify: `apps/finance/app/src/main/AndroidManifest.xml`
- Test: `apps/finance/feature/currency/src/test/java/com/dhruv/finance/currency/notify/DailyRateUseCaseTest.kt`

**Interfaces:**
- Consumes: `ICurrencyRepository.getDailyReport()` (A4), `DailyRateNotificationFormatter` (A5), `SettingsRepository.observe()` (`:libs:settings`, fields added in A8).
- Produces: `class DailyRateUseCase(repo, notifier, settings)` with `suspend fun run(): Result<Unit>`; `class RateNotifier(context)` with `fun notify(text: NotificationText)`; `class DailyRateWorker(appContext, params, useCase) : CoroutineWorker`.
- Note: `DailyRateUseCase` reads notify prefs from `SettingsRepository.observe().first()`. Task A8 adds `dailyRateNotificationEnabled`, `showGoldCard`, `showSilverCard` to `AppSettings`. **Do A8 before this task's Step 3 compiles**, or stub reads behind `try`. Recommended order: A8 → A7. (Listed A7 here to keep notify code together; execute A8 first.)

- [ ] **Step 1: Write the failing test (use-case, fakes only)**

```kotlin
package com.dhruv.finance.currency.notify

import com.dhruv.finance.data.Delta
import com.dhruv.finance.data.ICurrencyRepository
import com.dhruv.finance.data.model.CurrencyResult
import com.dhruv.finance.data.model.DailyRateReport
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyRateUseCaseTest {
    private class FakeRepo(val report: Result<DailyRateReport>) : ICurrencyRepository {
        override suspend fun getAllRates() = emptyList<com.dhruv.finance.data.CurrencyRateEntity>()
        override suspend fun getRate(code: String): Double? = null
        override suspend fun fetchAndCacheLatestRates(baseCurrency: String) = Result.success(emptyMap<String, Double>())
        override suspend fun fetchLatest(baseCurrency: String) =
            Result.success(CurrencyResult(emptyMap(), com.dhruv.finance.data.RateMeta("t", null, 0L)))
        override suspend fun buildDailyReport(result: CurrencyResult) = report.getOrThrow()
        override suspend fun getDailyReport(baseCurrency: String) = report
    }

    private class RecordingNotifier : RateNotifier(null) {
        var last: NotificationText? = null
        override fun notify(text: NotificationText) { last = text }
    }

    @Test
    fun notifiesWhenReportSucceeds() = runTest {
        val notifier = RecordingNotifier()
        val useCase = DailyRateUseCase(
            repo = FakeRepo(Result.success(DailyRateReport(Delta(95.4, 95.2, 0.2, 0.21), null, null, null))),
            notifier = notifier,
            enabledProvider = { Triple(true, false, false) }, // enabled, showGold, showSilver
        )
        val result = useCase.run()
        assertTrue(result.isSuccess)
        assertTrue(notifier.last!!.title.contains("₹95.40"))
    }

    @Test
    fun failsWhenReportFails() = runTest {
        val useCase = DailyRateUseCase(
            repo = FakeRepo(Result.failure(RuntimeException("net"))),
            notifier = RecordingNotifier(),
            enabledProvider = { Triple(true, false, false) },
        )
        assertTrue(useCase.run().isFailure)
    }
}
```
> To keep the use-case testable without `SettingsRepository`, inject an `enabledProvider: suspend () -> Triple<Boolean, Boolean, Boolean>` (enabled, showGold, showSilver). Production supplies one that reads `SettingsRepository.observe().first()`. `RateNotifier` takes a nullable `Context?` and its `notify` is `open` so the test can subclass without Android.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :apps:finance:feature:currency:testDebugUnitTest --tests "com.dhruv.finance.currency.notify.DailyRateUseCaseTest"`
Expected: FAIL — `DailyRateUseCase`/`RateNotifier` unresolved.

- [ ] **Step 3: Implement `RateNotifier.kt`**

```kotlin
package com.dhruv.finance.currency.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

open class RateNotifier(private val context: Context?) {
    open fun notify(text: NotificationText) {
        val ctx = context ?: return
        val mgr = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Daily rates", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Daily USD→INR and gold/silver update"
            },
        )
        val notification =
            NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(text.title)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text.body))
                .setAutoCancel(true)
                .build()
        try {
            NotificationManagerCompat.from(ctx).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted — no-op (opt-in flow requests it; guard here for safety).
        }
    }

    companion object {
        const val CHANNEL_ID = "dhruv_daily_rates"
        const val NOTIFICATION_ID = 4201
    }
}
```
> Add `implementation(libs.androidx.core.ktx)` is already present; `NotificationCompat` comes from `androidx.core`. Replace the placeholder small icon with an existing drawable during the ui-ux-pro-max pass (Task A10/B4) — a mono notification icon.

- [ ] **Step 4: Implement `DailyRateUseCase.kt`**

```kotlin
package com.dhruv.finance.currency.notify

import com.dhruv.finance.data.ICurrencyRepository

class DailyRateUseCase(
    private val repo: ICurrencyRepository,
    private val notifier: RateNotifier,
    private val enabledProvider: suspend () -> Triple<Boolean, Boolean, Boolean>,
) {
    /** Returns failure so the worker can retry; success even when disabled (nothing to do). */
    suspend fun run(): Result<Unit> {
        val (enabled, showGold, showSilver) = enabledProvider()
        if (!enabled) return Result.success(Unit)
        return repo.getDailyReport().map { report ->
            notifier.notify(DailyRateNotificationFormatter.format(report, showGold, showSilver))
        }
    }
}
```

- [ ] **Step 5: Implement `DailyRateWorker.kt`**

```kotlin
package com.dhruv.finance.currency.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class DailyRateWorker(
    appContext: Context,
    params: WorkerParameters,
    private val useCase: DailyRateUseCase,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result =
        useCase.run().fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
}
```

- [ ] **Step 6: Bind in `di/CurrencyModule.kt`**

```kotlin
package com.dhruv.finance.currency.di

import com.dhruv.finance.currency.CurrencyViewModel
import com.dhruv.finance.currency.notify.DailyRateScheduler
import com.dhruv.finance.currency.notify.DailyRateUseCase
import com.dhruv.finance.currency.notify.DailyRateWorker
import com.dhruv.finance.currency.notify.RateNotifier
import com.dhruv.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.workmanager.dsl.worker
import org.koin.dsl.module

val currencyModule =
    module {
        viewModel { CurrencyViewModel(get(), get(), get()) }
        single { RateNotifier(androidContext()) }
        single { DailyRateScheduler(androidContext()) }
        factory {
            val settings: SettingsRepository = get()
            DailyRateUseCase(
                repo = get(),
                notifier = get(),
                enabledProvider = {
                    val s = settings.observe().first()
                    Triple(s.dailyRateNotificationEnabled, s.showGoldCard, s.showSilverCard)
                },
            )
        }
        worker { DailyRateWorker(androidContext(), get(), get()) }
    }
```

- [ ] **Step 7: Wire Koin WorkManager factory in `CalculatorApplication.kt`**

Add import `import org.koin.androidx.workmanager.koin.workManagerFactory` and call it inside `startKoin { ... }` before `modules(...)`:
```kotlin
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@CalculatorApplication)
            workManagerFactory()
            modules(
                // ... unchanged list ...
            )
        }
```

- [ ] **Step 8: Manifest — permission + remove default WM initializer**

In `apps/finance/app/src/main/AndroidManifest.xml` add under the existing `<uses-permission>`:
```xml
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```
And inside `<application>` add the provider-merge removal (Koin initializes WorkManager itself via `workManagerFactory()`):
```xml
        <provider
            android:name="androidx.startup.InitializationProvider"
            android:authorities="${applicationId}.androidx-startup"
            android:exported="false"
            tools:node="merge">
            <meta-data
                android:name="androidx.work.WorkManagerInitializer"
                android:value="androidx.startup"
                tools:node="remove" />
        </provider>
```
(`xmlns:tools` is already declared on `<manifest>`.)

- [ ] **Step 9: Run tests + module build**

Run: `./gradlew :apps:finance:feature:currency:testDebugUnitTest`
Expected: PASS (use-case + earlier currency tests).
Run: `./gradlew :apps:finance:app:assembleDebug`
Expected: BUILD SUCCESSFUL (verifies Koin-WM wiring + manifest merge).

- [ ] **Step 10: Commit**

```bash
git add apps/finance/feature/currency/src/main/java/com/dhruv/finance/currency/notify/RateNotifier.kt \
        apps/finance/feature/currency/src/main/java/com/dhruv/finance/currency/notify/DailyRateUseCase.kt \
        apps/finance/feature/currency/src/main/java/com/dhruv/finance/currency/notify/DailyRateWorker.kt \
        apps/finance/feature/currency/src/main/java/com/dhruv/finance/currency/di/CurrencyModule.kt \
        apps/finance/app/src/main/java/com/dhruv/finance/app/CalculatorApplication.kt \
        apps/finance/app/src/main/AndroidManifest.xml \
        apps/finance/feature/currency/src/test/java/com/dhruv/finance/currency/notify/DailyRateUseCaseTest.kt
git commit -m "feat(currency): notifier + use-case + worker + Koin WorkManager wiring"
```

---

## Task A8: Settings — notification prefs (enabled, hour, minute, showGold, showSilver)

> **Execute before Task A7 Step 3** (A7's use-case reads these fields).

**Files:**
- Modify: `libs/settings/src/main/java/com/dhruv/settings/AppSettings.kt`
- Modify: `libs/settings/src/main/java/com/dhruv/settings/SettingsKeys.kt`
- Modify: `libs/settings/src/main/java/com/dhruv/settings/SettingsRepositoryImpl.kt`
- Test: `libs/settings/src/test/java/com/dhruv/settings/SettingsNotificationPrefsTest.kt` (create test dir if absent)

**Interfaces:**
- Produces on `AppSettings`: `dailyRateNotificationEnabled: Boolean = false`, `dailyRateNotificationHour: Int = 9`, `dailyRateNotificationMinute: Int = 0`, `showGoldCard: Boolean = true`, `showSilverCard: Boolean = true`.

- [ ] **Step 1: Write the failing test** (Robolectric — matches app test setup; settings already uses DataStore)

```kotlin
package com.dhruv.settings

import androidx.test.core.app.ApplicationProvider
import com.dhruv.core.observability.NoOpCrashReporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsNotificationPrefsTest {
    private fun repo() = SettingsRepositoryImpl(ApplicationProvider.getApplicationContext(), NoOpCrashReporter)

    @Test
    fun defaultsMatchSpec() = runTest {
        val s = repo().observe().first()
        assertFalse(s.dailyRateNotificationEnabled)
        assertEquals(9, s.dailyRateNotificationHour)
        assertEquals(0, s.dailyRateNotificationMinute)
        assertTrue(s.showGoldCard)
        assertTrue(s.showSilverCard)
    }

    @Test
    fun updatePersistsEnabledAndTime() = runTest {
        val r = repo()
        r.update { copy(dailyRateNotificationEnabled = true, dailyRateNotificationHour = 21, dailyRateNotificationMinute = 30) }
        val s = r.observe().first()
        assertTrue(s.dailyRateNotificationEnabled)
        assertEquals(21, s.dailyRateNotificationHour)
        assertEquals(30, s.dailyRateNotificationMinute)
    }
}
```
> If `:libs:settings` has no `testImplementation(libs.robolectric)` / `androidx.core` / `kotlinx-coroutines-test`, add them in this step (mirror `apps/finance/app/build.gradle.kts` test deps). Confirm `isIncludeAndroidResources = true` in the module `testOptions`.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :libs:settings:testDebugUnitTest --tests "com.dhruv.settings.SettingsNotificationPrefsTest"`
Expected: FAIL — new `AppSettings` fields unresolved.

- [ ] **Step 3: Add fields to `AppSettings.kt`**

```kotlin
data class AppSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    val accentColorHex: String = "#D4AF37",
    val fontFamily: DhruvFont = DhruvFont.DEFAULT,
    val biometricEnabled: Boolean = false,
    val syncEnabled: Boolean = false,
    val geminiApiKey: String? = null,
    val dailyRateNotificationEnabled: Boolean = false,
    val dailyRateNotificationHour: Int = 9,
    val dailyRateNotificationMinute: Int = 0,
    val showGoldCard: Boolean = true,
    val showSilverCard: Boolean = true,
)
```

- [ ] **Step 4: Add keys to `SettingsKeys.kt`**

```kotlin
    val DAILY_RATE_NOTIFICATION_ENABLED = booleanPreferencesKey("daily_rate_notification_enabled")
    val DAILY_RATE_NOTIFICATION_HOUR = intPreferencesKey("daily_rate_notification_hour")
    val DAILY_RATE_NOTIFICATION_MINUTE = intPreferencesKey("daily_rate_notification_minute")
    val SHOW_GOLD_CARD = booleanPreferencesKey("show_gold_card")
    val SHOW_SILVER_CARD = booleanPreferencesKey("show_silver_card")
```

- [ ] **Step 5: Map + persist in `SettingsRepositoryImpl.kt`**

In `observe()`'s `AppSettings(...)` builder add:
```kotlin
                dailyRateNotificationEnabled = plain[SettingsKeys.DAILY_RATE_NOTIFICATION_ENABLED] ?: false,
                dailyRateNotificationHour = plain[SettingsKeys.DAILY_RATE_NOTIFICATION_HOUR] ?: 9,
                dailyRateNotificationMinute = plain[SettingsKeys.DAILY_RATE_NOTIFICATION_MINUTE] ?: 0,
                showGoldCard = plain[SettingsKeys.SHOW_GOLD_CARD] ?: true,
                showSilverCard = plain[SettingsKeys.SHOW_SILVER_CARD] ?: true,
```
In `update()`'s plain `edit { }` block add:
```kotlin
                if (current.dailyRateNotificationEnabled != updated.dailyRateNotificationEnabled) {
                    prefs[SettingsKeys.DAILY_RATE_NOTIFICATION_ENABLED] = updated.dailyRateNotificationEnabled
                }
                if (current.dailyRateNotificationHour != updated.dailyRateNotificationHour) {
                    prefs[SettingsKeys.DAILY_RATE_NOTIFICATION_HOUR] = updated.dailyRateNotificationHour
                }
                if (current.dailyRateNotificationMinute != updated.dailyRateNotificationMinute) {
                    prefs[SettingsKeys.DAILY_RATE_NOTIFICATION_MINUTE] = updated.dailyRateNotificationMinute
                }
                if (current.showGoldCard != updated.showGoldCard) {
                    prefs[SettingsKeys.SHOW_GOLD_CARD] = updated.showGoldCard
                }
                if (current.showSilverCard != updated.showSilverCard) {
                    prefs[SettingsKeys.SHOW_SILVER_CARD] = updated.showSilverCard
                }
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `./gradlew :libs:settings:testDebugUnitTest --tests "com.dhruv.settings.SettingsNotificationPrefsTest"`
Expected: PASS (2 tests).

- [ ] **Step 7: Commit**

```bash
git add libs/settings/src/main/java/com/dhruv/settings/AppSettings.kt \
        libs/settings/src/main/java/com/dhruv/settings/SettingsKeys.kt \
        libs/settings/src/main/java/com/dhruv/settings/SettingsRepositoryImpl.kt \
        libs/settings/src/test/java/com/dhruv/settings/SettingsNotificationPrefsTest.kt \
        libs/settings/build.gradle.kts
git commit -m "feat(settings): daily-rate notification prefs (enabled/time/metal visibility)"
```

---

## Task A9: PlatformModule / dataModule wiring for new API client + snapshot DAO

**Files:**
- Modify: `apps/finance/data/src/main/java/com/dhruv/finance/data/di/DataModule.kt`
- Modify: `apps/finance/app/src/main/java/com/dhruv/finance/app/di/PlatformModule.kt`

**Interfaces:**
- Consumes: `CurrencyApiClient` (A3), `RateSnapshotDao` (A1), `CurrencyRepository` new ctor (A4).
- Produces: Koin graph resolves `ICurrencyRepository` with the 5-arg ctor; `RateSnapshotDao` bound.

- [ ] **Step 1: Bind snapshot DAO + repository seams in `DataModule.kt`**

```kotlin
val dataModule =
    module {
        single { get<AppDatabase>().historyDao() }
        single { get<AppDatabase>().currencyRateDao() }
        single { get<AppDatabase>().rateSnapshotDao() }

        single<ICurrencyRepository> {
            val client: com.dhruv.finance.data.api.CurrencyApiClient = get()
            CurrencyRepository(
                currencyRateDao = get(),
                snapshotDao = get(),
                fxFetcher = { base ->
                    runCatching {
                        val r = client.frankfurter.latest(base)
                        r.rates to java.time.LocalDate.parse(r.date)
                    }.recoverCatching {
                        val f = client.fawaz.usd()
                        // Fawaz uses lowercase ISO codes -> uppercase for the app's rate map.
                        f.usd.mapKeys { it.key.uppercase() } to java.time.LocalDate.parse(f.date)
                    }
                },
                metalFetcher = { Result.success(null) }, // Phase A: metals wired in Task B2
            )
        }
    }
```

- [ ] **Step 2: Update `CurrencyApiClient` construction in `PlatformModule.kt`**

Replace the existing `CurrencyApiClient(...)` single with the new positional args:
```kotlin
        single {
            CurrencyApiClient(
                frankfurterBaseUrl = BuildConfig.CURRENCY_API_BASE_URL,
                fawazBaseUrl = BuildConfig.CURRENCY_API_FALLBACK_BASE_URL,
                timeoutSeconds = BuildConfig.CURRENCY_API_TIMEOUT_SECONDS,
                userAgent = BuildConfig.CURRENCY_API_USER_AGENT,
            )
        }
```

- [ ] **Step 3: Build to verify the graph resolves**

Run: `./gradlew :apps:finance:app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add apps/finance/data/src/main/java/com/dhruv/finance/data/di/DataModule.kt \
        apps/finance/app/src/main/java/com/dhruv/finance/app/di/PlatformModule.kt
git commit -m "feat(di): wire keyless API client + snapshot DAO into Koin graph"
```

---

## Task A10: Settings UI (toggle + time picker) + permission + scheduler wiring + VM freshness

**Files:**
- Modify: `apps/finance/feature/currency/src/main/java/com/dhruv/finance/currency/CurrencyViewModel.kt`
- Modify: `apps/finance/feature/currency/src/main/java/com/dhruv/finance/currency/CurrencyScreen.kt`
- Modify (app Settings): the Settings screen composables under `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/settings/` + `SettingsViewModel.kt`
- Modify: `apps/finance/app/src/main/java/com/dhruv/finance/app/MainActivity.kt`

**Interfaces:**
- Consumes: `SettingsRepository` (A8), `DailyRateScheduler` (A6), `ICurrencyRepository.fetchLatest`/`buildDailyReport` (A4).
- Produces: VM exposes `rateMeta: StateFlow<RateMeta?>`; Settings exposes a notification section calling `scheduler.schedule/cancel` + requesting `POST_NOTIFICATIONS`.

> **UI design step (spec §12, D11):** invoke the **ui-ux-pro-max** skill to design the currency screen refresh (source/freshness affordance, state treatments) and the Settings notification row/time-picker, following the Compose Screen→UiState→Content pattern and the Dhruv design tokens. The code below is the behavioral contract the design must satisfy; final visuals come from that pass. Then review with **dhruv-ui-review**.

- [ ] **Step 1: VM — expose `rateMeta` and use `fetchLatest`**

In `CurrencyViewModel.kt` add:
```kotlin
    private val _rateMeta = MutableStateFlow<com.dhruv.finance.data.RateMeta?>(null)
    val rateMeta = _rateMeta.asStateFlow()
```
In `syncCurrencyRates()` replace the `fetchAndCacheLatestRates("USD")` call with:
```kotlin
            val result = currencyRepository.fetchLatest("USD")
            result
                .onSuccess { cr ->
                    _ratesMap.value = cr.rates
                    _rateMeta.value = cr.meta
                    _lastUpdatedTime.value = cr.meta.fetchedAt
                    _currencyStatus.value = CurrencyStatus.Success(isOffline = cr.meta.sourceName == "Cache")
                    recalculateCurrency()
                }.onFailure { err ->
                    crashReporter.recordException(err)
                    _currencyStatus.value = CurrencyStatus.Error("No cached or live rates found.")
                }
```
(Existing offline-cache branch is now handled inside `fetchLatest`'s `recoverCatching`; the VM maps `sourceName == "Cache"` to offline.)

- [ ] **Step 2: VM behavior test**

Add to `CurrencyViewModelTest.kt`:
```kotlin
    @Test
    fun exposesRateMetaAfterSync() =
        runTest(dispatcher) {
            val vm = viewModel()
            advanceUntilIdle()
            org.junit.Assert.assertEquals("Test", vm.rateMeta.value?.sourceName)
        }
```
Run: `./gradlew :apps:finance:feature:currency:testDebugUnitTest --tests "com.dhruv.finance.currency.CurrencyViewModelTest"`
Expected: PASS (existing + new).

- [ ] **Step 3: Screen — freshness/source line (ui-ux-pro-max design)**

In `CurrencyScreen.kt`, collect `val rateMeta by viewModel.rateMeta.collectAsState()` and, inside the Success card `Column`, add a source line (design/refine via ui-ux-pro-max, tokens only):
```kotlin
                            rateMeta?.let { meta ->
                                Text(
                                    text = buildString {
                                        append("Source: ${meta.sourceName}")
                                        meta.rateDate?.let { append(" · rates as of $it") }
                                        meta.quotaLimit?.let { lim ->
                                            append(" · quota ${meta.quotaRemaining ?: "?"}/$lim")
                                        }
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    modifier = Modifier.testTag("currency_source_line"),
                                )
                            }
```

- [ ] **Step 4: Settings section — notification toggle + time picker + permission + scheduler**

Add a notification section to the app Settings UI following the existing section pattern (mirror `SyncSection`/`AiAssistantSection` from `:libs:settings/ui`, or the app's `SettingsRows`). Behavioral contract (design via ui-ux-pro-max):
```kotlin
// In SettingsViewModel (app): expose current prefs from SettingsRepository.observe() and:
fun setDailyRateEnabled(enabled: Boolean) = viewModelScope.launch {
    settingsRepository.update { copy(dailyRateNotificationEnabled = enabled) }
    if (enabled) scheduler.schedule(hour, minute) else scheduler.cancel()
}
fun setDailyRateTime(h: Int, m: Int) = viewModelScope.launch {
    settingsRepository.update { copy(dailyRateNotificationHour = h, dailyRateNotificationMinute = m) }
    if (enabledNow) scheduler.schedule(h, m)
}
```
`DailyRateScheduler` is injected into `SettingsViewModel` via Koin (`get()`), which is allowed (app → feature). In the composable, when the switch is turned on, request `POST_NOTIFICATIONS` on `Build.VERSION.SDK_INT >= 33` using an `ActivityResultContracts.RequestPermission()` launcher; on denial, revert the switch and show a rationale `Snackbar`. Show the Material `TimePicker` (or `TimePickerDialog`) only when enabled; default to the stored hour/minute.

- [ ] **Step 5: MainActivity — re-ensure schedule on start**

In `MainActivity.onCreate` (after Koin is up), re-ensure the schedule matches persisted prefs:
```kotlin
        val settings: SettingsRepository = get()
        val scheduler: DailyRateScheduler = get()
        lifecycleScope.launch {
            val s = settings.observe().first()
            if (s.dailyRateNotificationEnabled) scheduler.schedule(s.dailyRateNotificationHour, s.dailyRateNotificationMinute)
        }
```
(Imports: `org.koin.android.ext.android.get`, `androidx.lifecycle.lifecycleScope`, `kotlinx.coroutines.flow.first`, `kotlinx.coroutines.launch`.)

- [ ] **Step 6: Run currency + app tests and build**

Run: `./gradlew :apps:finance:feature:currency:testDebugUnitTest :apps:finance:app:assembleDebug`
Expected: PASS + BUILD SUCCESSFUL.

- [ ] **Step 7: ui-ux-pro-max design pass + dhruv-ui-review**

Invoke ui-ux-pro-max to finalize the currency screen refresh (Phase A elements: converter hierarchy, freshness affordance, loading/offline/error states) and the Settings notification row/time picker. Then run `dhruv-ui-review`. Apply feedback. Verify light+dark with a non-default accent.

- [ ] **Step 8: Commit**

```bash
git add apps/finance/feature/currency/src/main/java/com/dhruv/finance/currency/CurrencyViewModel.kt \
        apps/finance/feature/currency/src/main/java/com/dhruv/finance/currency/CurrencyScreen.kt \
        apps/finance/feature/currency/src/test/java/com/dhruv/finance/currency/CurrencyViewModelTest.kt \
        apps/finance/app/src/main/java/com/dhruv/finance/app/ui/settings/ \
        apps/finance/app/src/main/java/com/dhruv/finance/app/MainActivity.kt
git commit -m "feat(currency): freshness line + Settings notification toggle/time-picker + UI refresh (Phase A)"
```

- [ ] **Step 9: Phase A gate**

Run: `./gradlew regressionCheck`
Expected: all unit tests + ArchUnit pass; merged coverage ≥ current floor.

---

# PHASE B — Gold & silver

## Task B1: Pure metal spot pricing

**Files:**
- Create: `apps/finance/data/src/main/java/com/dhruv/finance/data/MetalPricing.kt`
- Test: `apps/finance/data/src/test/java/com/dhruv/finance/data/MetalPricingTest.kt`

**Interfaces:**
- Produces: `object MetalPricing { const val GRAMS_PER_TROY_OZ = 31.1034768; fun spot(unitsPerUsd: Double, usdInr: Double): MetalSpot }` returning `MetalSpot` (A2 model).

- [ ] **Step 1: Write the failing test**

```kotlin
package com.dhruv.finance.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MetalPricingTest {
    @Test
    fun goldSpotFromXauPerUsd() {
        // usd.xau = 0.0004 troy-oz gold per USD -> 2500 USD/oz; usdInr = 95.0
        val s = MetalPricing.spot(unitsPerUsd = 0.0004, usdInr = 95.0)
        assertEquals(2500.0, s.perOzUsd, 1e-6)
        assertEquals(2500.0 * 95.0, s.perOzInr, 1e-6)
        assertEquals(2500.0 * 95.0 / 31.1034768, s.perGramInr, 1e-6)
        assertEquals(s.perGramInr * 10.0, s.per10gInr, 1e-6)
    }

    @Test
    fun zeroUnitsPerUsdYieldsZeroSpot() {
        val s = MetalPricing.spot(unitsPerUsd = 0.0, usdInr = 95.0)
        assertEquals(0.0, s.perOzUsd, 0.0)
        assertEquals(0.0, s.per10gInr, 0.0)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :apps:finance:data:testDebugUnitTest --tests "com.dhruv.finance.data.MetalPricingTest"`
Expected: FAIL — `MetalPricing` unresolved.

- [ ] **Step 3: Implement `MetalPricing.kt`**

```kotlin
package com.dhruv.finance.data

import com.dhruv.finance.data.model.MetalSpot

object MetalPricing {
    const val GRAMS_PER_TROY_OZ = 31.1034768

    /** [unitsPerUsd] = troy-oz of metal per 1 USD (Fawaz usd.xau / usd.xag). 0 -> zero spot. */
    fun spot(unitsPerUsd: Double, usdInr: Double): MetalSpot {
        val perOzUsd = if (unitsPerUsd == 0.0) 0.0 else 1.0 / unitsPerUsd
        val perOzInr = perOzUsd * usdInr
        val perGramInr = perOzInr / GRAMS_PER_TROY_OZ
        return MetalSpot(perGramInr = perGramInr, per10gInr = perGramInr * 10.0, perOzUsd = perOzUsd, perOzInr = perOzInr)
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :apps:finance:data:testDebugUnitTest --tests "com.dhruv.finance.data.MetalPricingTest"`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add apps/finance/data/src/main/java/com/dhruv/finance/data/MetalPricing.kt \
        apps/finance/data/src/test/java/com/dhruv/finance/data/MetalPricingTest.kt
git commit -m "feat(data): pure gold/silver spot pricing (troy-oz -> INR per gram/10g)"
```

---

## Task B2: Metals in the repository report + XAU/XAG snapshots

**Files:**
- Modify: `apps/finance/data/src/main/java/com/dhruv/finance/data/CurrencyRepository.kt`
- Modify: `apps/finance/data/src/main/java/com/dhruv/finance/data/di/DataModule.kt` (real `metalFetcher`)
- Test: `apps/finance/data/src/test/java/com/dhruv/finance/data/CurrencyReportMetalsTest.kt`

**Interfaces:**
- Consumes: `MetalPricing` (B1), `MetalSpots` (A4), snapshot DAO (A1).
- Produces: `buildDailyReport` populates `gold`/`silver` `MetalQuote` with correct per-10g deltas (previous = per-10g reconstructed from the stored per-oz-USD snapshot × current usdInr — see note) and writes `XAU_USD_OZ`/`XAG_USD_OZ` snapshots.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.dhruv.finance.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDate

class CurrencyReportMetalsTest {
    private fun repo(snap: FakeRateSnapshotDao) = CurrencyRepository(
        currencyRateDao = FakeCurrencyRateDao(),
        snapshotDao = snap,
        fxFetcher = { Result.success(mapOf("INR" to 95.0) to LocalDate.parse("2026-07-02")) },
        metalFetcher = {
            Result.success(
                MetalSpots(
                    gold = MetalPricing.spot(unitsPerUsd = 0.0004, usdInr = 95.0),   // 2500 USD/oz
                    silver = MetalPricing.spot(unitsPerUsd = 0.033, usdInr = 95.0),
                ),
            )
        },
        clock = { 1_720_000_000_000L },
    )

    @Test
    fun populatesGoldAndSilverAndWritesOzSnapshots() = runTest {
        val snap = FakeRateSnapshotDao()
        val r = repo(snap)
        val report = r.buildDailyReport(r.fetchLatest("USD").getOrThrow())
        assertNotNull(report.gold)
        assertEquals(2500.0, report.gold!!.spot.perOzUsd, 1e-6)
        val today = java.time.Instant.ofEpochMilli(1_720_000_000_000L).atZone(java.time.ZoneOffset.UTC).toLocalDate().toString()
        assertEquals(2500.0, snap.getByDate(RateSeries.XAU_USD_OZ, today)!!.value, 1e-6)
    }

    @Test
    fun goldPer10gDeltaUsesPreviousOzSnapshot() = runTest {
        val snap = FakeRateSnapshotDao()
        // yesterday gold 2400 USD/oz -> per10g INR at today's usdInr(95) = 2400*95/31.10347*10
        snap.upsert(listOf(RateSnapshotEntity("2026-07-01", RateSeries.XAU_USD_OZ, 2400.0)))
        val report = repo(snap).buildDailyReport(repo(snap).fetchLatest("USD").getOrThrow())
        val prev10g = MetalPricing.spot(unitsPerUsd = 1.0 / 2400.0, usdInr = 95.0).per10gInr
        assertEquals(prev10g, report.gold!!.per10gDelta.previous!!, 1e-6)
        assertEquals(report.gold!!.spot.per10gInr - prev10g, report.gold!!.per10gDelta.absChange!!, 1e-6)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :apps:finance:data:testDebugUnitTest --tests "com.dhruv.finance.data.CurrencyReportMetalsTest"`
Expected: FAIL — metal branches return placeholder deltas.

- [ ] **Step 3: Replace the placeholder metal branches in `buildDailyReport`**

In `CurrencyRepository.buildDailyReport`, replace the `gold`/`silver` blocks from Task A4 with correct per-10g delta logic. Snapshots store **per-oz-USD** (stable, FX-independent); reconstruct the previous per-10g INR at *today's* usdInr so the delta reflects metal movement, not FX noise:
```kotlin
            val usdInr = usdInrNow
            fun metalQuote(spot: com.dhruv.finance.data.model.MetalSpot, series: String): com.dhruv.finance.data.model.MetalQuote {
                val prevOzUsd = snapshotDao.latestBefore(series, today)?.value
                toWrite += RateSnapshotEntity(today, series, spot.perOzUsd)
                val prev10g = prevOzUsd?.let { MetalPricing.spot(unitsPerUsd = if (it == 0.0) 0.0 else 1.0 / it, usdInr = usdInr).per10gInr }
                return com.dhruv.finance.data.model.MetalQuote(spot, computeDelta(spot.per10gInr, prev10g))
            }
            val gold = metals?.let { metalQuote(it.gold, RateSeries.XAU_USD_OZ) }
            val silver = metals?.let { metalQuote(it.silver, RateSeries.XAG_USD_OZ) }
```

- [ ] **Step 4: Wire the real `metalFetcher` in `DataModule.kt`**

Replace `metalFetcher = { Result.success(null) }` with a Fawaz-backed fetch:
```kotlin
                metalFetcher = {
                    runCatching {
                        val f = client.fawaz.usd()
                        val usdInr = f.usd["inr"] ?: error("no inr in fawaz")
                        val xau = f.usd["xau"] ?: error("no xau")
                        val xag = f.usd["xag"] ?: error("no xag")
                        com.dhruv.finance.data.MetalSpots(
                            gold = com.dhruv.finance.data.MetalPricing.spot(xau, usdInr),
                            silver = com.dhruv.finance.data.MetalPricing.spot(xag, usdInr),
                        )
                    }.fold({ Result.success(it) }, { Result.success(null) }) // metals optional; null hides cards
                },
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :apps:finance:data:testDebugUnitTest`
Expected: PASS (metals tests + all prior data tests).

- [ ] **Step 6: Commit**

```bash
git add apps/finance/data/src/main/java/com/dhruv/finance/data/CurrencyRepository.kt \
        apps/finance/data/src/main/java/com/dhruv/finance/data/di/DataModule.kt \
        apps/finance/data/src/test/java/com/dhruv/finance/data/CurrencyReportMetalsTest.kt
git commit -m "feat(data): gold/silver spot in daily report + Fawaz metal fetcher"
```

---

## Task B3: Metals in the notification (formatter already supports it; verify end-to-end)

**Files:**
- Test: `apps/finance/feature/currency/src/test/java/com/dhruv/finance/currency/notify/DailyRateUseCaseMetalsTest.kt`

**Interfaces:** Consumes A5 formatter (already handles metals) + A7 use-case. No production change expected unless a gap surfaces.

- [ ] **Step 1: Write the failing/《confirming》test**

```kotlin
package com.dhruv.finance.currency.notify

import com.dhruv.finance.data.Delta
import com.dhruv.finance.data.ICurrencyRepository
import com.dhruv.finance.data.model.CurrencyResult
import com.dhruv.finance.data.model.DailyRateReport
import com.dhruv.finance.data.model.MetalQuote
import com.dhruv.finance.data.model.MetalSpot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyRateUseCaseMetalsTest {
    private val gold = MetalQuote(MetalSpot(7000.0, 70000.0, 2500.0, 217650.0), Delta(70000.0, 69800.0, 200.0, 0.286))
    private val report = DailyRateReport(Delta(95.4, 95.2, 0.2, 0.21), gold, null, null)

    private class FakeRepo(val r: DailyRateReport) : ICurrencyRepository {
        override suspend fun getAllRates() = emptyList<com.dhruv.finance.data.CurrencyRateEntity>()
        override suspend fun getRate(code: String): Double? = null
        override suspend fun fetchAndCacheLatestRates(baseCurrency: String) = Result.success(emptyMap<String, Double>())
        override suspend fun fetchLatest(baseCurrency: String) = Result.success(CurrencyResult(emptyMap(), com.dhruv.finance.data.RateMeta("t", null, 0L)))
        override suspend fun buildDailyReport(result: CurrencyResult) = r
        override suspend fun getDailyReport(baseCurrency: String) = Result.success(r)
    }

    @Test
    fun notificationIncludesGoldWhenShowGoldEnabled() = runTest {
        val notifier = object : RateNotifier(null) { var last: NotificationText? = null; override fun notify(text: NotificationText) { last = text } }
        DailyRateUseCase(FakeRepo(report), notifier, enabledProvider = { Triple(true, true, false) }).run()
        assertTrue(notifier.last!!.body.contains("Gold"))
    }

    @Test
    fun notificationOmitsGoldWhenShowGoldDisabled() = runTest {
        val notifier = object : RateNotifier(null) { var last: NotificationText? = null; override fun notify(text: NotificationText) { last = text } }
        DailyRateUseCase(FakeRepo(report), notifier, enabledProvider = { Triple(true, false, false) }).run()
        assertTrue(!notifier.last!!.body.contains("Gold"))
    }
}
```

- [ ] **Step 2: Run tests**

Run: `./gradlew :apps:finance:feature:currency:testDebugUnitTest --tests "com.dhruv.finance.currency.notify.DailyRateUseCaseMetalsTest"`
Expected: PASS (2 tests). If a gap surfaces, fix the formatter/use-case, re-run.

- [ ] **Step 3: Commit**

```bash
git add apps/finance/feature/currency/src/test/java/com/dhruv/finance/currency/notify/DailyRateUseCaseMetalsTest.kt
git commit -m "test(currency): verify metals appear in daily notification per prefs"
```

---

## Task B4: Gold/silver cards + investor-basics detail sheet + visibility toggles (ui-ux-pro-max)

**Files:**
- Modify: `apps/finance/feature/currency/src/main/java/com/dhruv/finance/currency/CurrencyViewModel.kt`
- Modify: `apps/finance/feature/currency/src/main/java/com/dhruv/finance/currency/CurrencyScreen.kt`
- Create: `apps/finance/feature/currency/src/main/java/com/dhruv/finance/currency/MetalDetailSheet.kt`
- Modify: app Settings UI — add "Show gold" / "Show silver" switches (bound to `showGoldCard`/`showSilverCard`).
- Test: `apps/finance/feature/currency/src/test/java/com/dhruv/finance/currency/CurrencyViewModelMetalsTest.kt`

**Interfaces:**
- Consumes: `ICurrencyRepository.buildDailyReport` (B2), `MetalQuote`/`MetalSpot` (A2), `SettingsRepository` visibility flags (A8).
- Produces: VM `gold: StateFlow<MetalQuote?>`, `silver: StateFlow<MetalQuote?>`, `selectedMetal: StateFlow<SelectedMetal?>`, `showGold`/`showSilver` flags; `openMetal(m)` / `closeMetal()`.

> **UI design step (spec §12, D11):** invoke **ui-ux-pro-max** to design the gold/silver summary cards and the investor-basics detail bottom sheet (24K per 10g & gram INR, per troy oz USD & INR, day-change ▲▼, and the "spot ≠ local retail: duty/GST/making charges; gold-as-hedge" note, clearly labelled international spot). Tokens/`DhruvFont`/accent only; metals may use tasteful gold/silver accents derived from theme with dark-mode contrast. Then run **dhruv-ui-review**.

- [ ] **Step 1: VM — expose metals + selection (write test first)**

`CurrencyViewModelMetalsTest.kt`:
```kotlin
package com.dhruv.finance.currency

import com.dhruv.core.observability.NoOpCrashReporter
import com.dhruv.core.observability.NoOpPerformanceTracer
import com.dhruv.finance.data.CurrencyRateEntity
import com.dhruv.finance.data.Delta
import com.dhruv.finance.data.ICurrencyRepository
import com.dhruv.finance.data.model.CurrencyResult
import com.dhruv.finance.data.model.DailyRateReport
import com.dhruv.finance.data.model.MetalQuote
import com.dhruv.finance.data.model.MetalSpot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CurrencyViewModelMetalsTest {
    private val dispatcher = StandardTestDispatcher()
    private val gold = MetalQuote(MetalSpot(7000.0, 70000.0, 2500.0, 217650.0), Delta(70000.0, 69800.0, 200.0, 0.286))

    private class FakeRepo(val gold: MetalQuote) : ICurrencyRepository {
        override suspend fun getAllRates() = listOf(CurrencyRateEntity("USD", 1.0, 0L), CurrencyRateEntity("INR", 95.0, 0L))
        override suspend fun getRate(code: String): Double? = null
        override suspend fun fetchAndCacheLatestRates(baseCurrency: String) = Result.success(mapOf("USD" to 1.0, "INR" to 95.0))
        override suspend fun fetchLatest(baseCurrency: String) = Result.success(CurrencyResult(mapOf("USD" to 1.0, "INR" to 95.0), com.dhruv.finance.data.RateMeta("t", null, 0L)))
        override suspend fun buildDailyReport(result: CurrencyResult) = DailyRateReport(Delta(95.0, null, null, null), gold, null, null)
        override suspend fun getDailyReport(baseCurrency: String) = Result.success(buildDailyReport(fetchLatest(baseCurrency).getOrThrow()))
    }

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun exposesGoldQuoteAfterSync() = runTest(dispatcher) {
        val vm = CurrencyViewModel(FakeRepo(gold), NoOpCrashReporter, NoOpPerformanceTracer)
        advanceUntilIdle()
        assertEquals(70000.0, vm.gold.value!!.spot.per10gInr, 1e-6)
    }

    @Test
    fun openAndCloseMetalSelection() = runTest(dispatcher) {
        val vm = CurrencyViewModel(FakeRepo(gold), NoOpCrashReporter, NoOpPerformanceTracer)
        advanceUntilIdle()
        vm.openMetal(CurrencyViewModel.SelectedMetal.GOLD)
        assertEquals(CurrencyViewModel.SelectedMetal.GOLD, vm.selectedMetal.value)
        vm.closeMetal()
        assertEquals(null, vm.selectedMetal.value)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :apps:finance:feature:currency:testDebugUnitTest --tests "com.dhruv.finance.currency.CurrencyViewModelMetalsTest"`
Expected: FAIL — `vm.gold`/`openMetal`/`SelectedMetal` unresolved.

- [ ] **Step 3: VM — add metals state + selection + report call**

In `CurrencyViewModel.kt` add:
```kotlin
    enum class SelectedMetal { GOLD, SILVER }

    private val _gold = MutableStateFlow<com.dhruv.finance.data.model.MetalQuote?>(null)
    val gold = _gold.asStateFlow()
    private val _silver = MutableStateFlow<com.dhruv.finance.data.model.MetalQuote?>(null)
    val silver = _silver.asStateFlow()
    private val _selectedMetal = MutableStateFlow<SelectedMetal?>(null)
    val selectedMetal = _selectedMetal.asStateFlow()

    fun openMetal(m: SelectedMetal) { _selectedMetal.value = m }
    fun closeMetal() { _selectedMetal.value = null }
```
In `syncCurrencyRates()`'s `onSuccess { cr -> ... }` (from A10 Step 1), after `recalculateCurrency()`, build the report for metals + usdInr delta:
```kotlin
                    val report = currencyRepository.buildDailyReport(cr)
                    _gold.value = report.gold
                    _silver.value = report.silver
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :apps:finance:feature:currency:testDebugUnitTest --tests "com.dhruv.finance.currency.CurrencyViewModelMetalsTest"`
Expected: PASS (2 tests).

- [ ] **Step 5: Screen + detail sheet (ui-ux-pro-max) + Settings visibility switches**

- Add gold/silver summary cards to `CurrencyScreen.kt` (shown per `showGoldCard`/`showSilverCard`, collected from settings via the VM or passed in), each `Modifier.testTag("metal_card_gold")` / `"metal_card_silver"`, `onClick = { viewModel.openMetal(...) }`.
- Create `MetalDetailSheet.kt` — a `ModalBottomSheet` shown when `selectedMetal != null`, rendering the investor-basics content (§12) with `testTag("metal_detail_sheet")`.
- In the app Settings notification section, add "Show gold card" / "Show silver card" switches bound to `settingsRepository.update { copy(showGoldCard = …) }` etc.
- Design all of the above with **ui-ux-pro-max**; tokens/accent/`DhruvFont` only.

- [ ] **Step 6: Build + module tests + UI review**

Run: `./gradlew :apps:finance:feature:currency:testDebugUnitTest :apps:finance:app:assembleDebug`
Expected: PASS + BUILD SUCCESSFUL. Then run `dhruv-ui-review`; verify light+dark with a non-default accent.

- [ ] **Step 7: Commit**

```bash
git add apps/finance/feature/currency/src/main/java/com/dhruv/finance/currency/CurrencyViewModel.kt \
        apps/finance/feature/currency/src/main/java/com/dhruv/finance/currency/CurrencyScreen.kt \
        apps/finance/feature/currency/src/main/java/com/dhruv/finance/currency/MetalDetailSheet.kt \
        apps/finance/feature/currency/src/test/java/com/dhruv/finance/currency/CurrencyViewModelMetalsTest.kt \
        apps/finance/app/src/main/java/com/dhruv/finance/app/ui/settings/
git commit -m "feat(currency): gold/silver cards + investor-basics detail sheet + visibility toggles"
```

---

## Task B5: Full pre-merge gate + architecture/compliance review

**Files:** none (verification only).

- [ ] **Step 1: Run the full regression gate**

Run: `./gradlew regressionCheck`
Expected: all unit tests + ArchUnit pass; merged coverage ≥ floor. If coverage dipped below the floor, add tests for any uncovered new branch (e.g., `fetchLatest` cache-fallback path) — do **not** lower the floor.

- [ ] **Step 2: ArchUnit boundary check**

Run: `./gradlew :apps:finance:app:testDebugUnitTest --tests "com.dhruv.finance.app.arch.DependencyRulesTest"`
Expected: PASS — confirm `feature:currency` still imports only `:data`/`:libs:core`/`:libs:settings` (no other feature). WorkManager/Koin-WM are external libs, allowed.

- [ ] **Step 3: Security + DPDP review**

Invoke the `dhruv-security` skill (or `dhruv-security-compliance-reviewer` agent). Confirm: no keys added, `POST_NOTIFICATIONS` is the only new permission (requested at opt-in), no user PII leaves the device, no new consent gate required (public GET + local notification).

- [ ] **Step 4: Pre-merge audit**

Invoke `dhruv-pre-merge` (runs the three review agents + regression). Address any FAIL. When green, the branch is ready for a PR to `develop`.

---

## Self-Review (author check against spec)

**Spec coverage:**
- §1 accurate source → A3 (Frankfurter/Fawaz) + A4 (`fetchLatest`). ✅
- §2/D2 freshness/limits line → A3 (`RateMeta`) + A10 Step 3 (`currency_source_line`). ✅
- §1.3 gold/silver cards + detail → B4; keyless metals → B1/B2. ✅
- §1.4 daily notification, configurable time, metals + delta → A5–A8, A10, B2/B3. ✅
- §1.5 UI/UX upgrade within app rules → A10 Step 7 + B4 Step 5 (ui-ux-pro-max + dhruv-ui-review) + Global Constraints. ✅
- D4 opt-in default OFF + time picker → A8 defaults + A10 Step 4. ✅
- D5 WorkManager next-time scheduling → A6. ✅
- D6 no consent gate → B5 Step 3 + Global Constraints. ✅
- D7/D10 metals cards + bottom-sheet detail, no new route → B4. ✅
- D9 keyed snapshots for FX+metals → A1 + A4 + B2. ✅
- §6 edge cases (first-run null delta, metals-unavailable, permission denied, retry) → A2/A5/A7/A10 tests. ✅
- §7 tests via fakes, ArchUnit re-run → all data/currency tasks + B5. ✅
- §10 phasing (UI folded into each phase) → Phase A/B structure. ✅

**Placeholder scan:** the only intentional forward-references are A4's `/* placeholder */` metal branches (explicitly completed in B2) and `MetalSpots` (defined A4, populated B2); both are called out. No "TBD"/"add error handling"/"write tests for the above". UI visual detail is deliberately delegated to the ui-ux-pro-max steps per spec §12/D11 — the behavioral contract + testTags are concrete.

**Type consistency:** `fetchLatest`/`buildDailyReport`/`getDailyReport`, `CurrencyResult`, `RateMeta`, `Delta`, `MetalSpot`, `MetalQuote`, `DailyRateReport`, `MetalSpots`, `computeDelta`, `MetalPricing.spot`, `DailyRateNotificationFormatter.format`, `RateNotifier.notify`, `DailyRateUseCase(run/enabledProvider)`, `DailyRateWorker`, `ScheduleMath.initialDelayMillis`, `DailyRateScheduler.schedule/cancel/WORK_NAME`, `RateSeries.*`, `RateSnapshotDao.upsert/latestBefore/getByDate`, and the five `AppSettings` fields are used identically across tasks.

**Known risk flagged for the implementer:** the Koin-WorkManager init (`workManagerFactory()` + manifest `WorkManagerInitializer` removal) is the one integration-sensitive spot; A7 Step 9 builds the app specifically to catch a double-init or missing-factory error early. If Koin's `workManagerFactory()` API differs in koin `3.5.6`, fall back to a `Configuration.Provider` on `CalculatorApplication` with a hand-rolled `WorkerFactory` resolving `DailyRateUseCase` from the global Koin context.
