# Sovereign Multi-Mode Calculator, Converters & Planners Suite
[![Android Platform](https://img.shields.io/badge/Platform-Android-green.svg?style=flat-square)](#)
[![Kotlin Compose](https://img.shields.io/badge/Kotlin-Compose%20M3-purple.svg?style=flat-square)](#)
[![Build Status](https://img.shields.io/badge/Build-Production%20Ready-blue.svg?style=flat-square)](#)

Welcome to the **Sovereign Multi-Mode Calculator, Unit/Currency Converter, and Financial Planning App**—an advanced, enterprise-grade Android application built using **Jetpack Compose (Material 3)**, **Kotlin**, and the **Room Database**.

This application is designed from the ground up prioritizing modular, decoupled Clean Architecture guidelines (**MVVM**), modern gestural navigation patterns, robust local replication engines, offline data capabilities, and a fully **Responsive & Adaptive Device Metric Layer** for screens ranging from compact mobile devices to foldables and large high-density tablets.

---

## 📱 Live Feature Highlights

The suite is segmented into five major functional layers, accessible instantly via a fluid, gesture-friendly slider system (`HorizontalPager`):

### 1. High-Precision Scientific Calculator
*   **Expression Parser**: Powered by a custom tokenizing top-down parser supporting parentheses nesting, operator hierarchy modeling, and floating-point constraints.
*   **Scientific Core**: Standard and advanced operators including `sin`, `cos`, `tan`, `asin`, `acos`, `atan`, `log`, `ln`, `sqrt`, factorial expansion `!`, PI `π`, and Euler's constant `e`.
*   **Swipe-to-History Peek**: Double-tap or swipe left over the calculations display terminal to slide in a date-grouped historical tape history log.
*   **Interactive History Replication**: Tap on any past equation tape to instantly re-populate it into the main formula field for further calculation.

### 2. Multi-Physical & Currency Converter
*   **Sovereign Measurement Conversion**: Standard-compliant ratios for mass, length, temperature, and specific volumes.
*   **Offline-First Currency Calculator**: Caches and parses active global foreign exchange rates locally using Room to enable internet-resilient, offline conversions.
*   **Instant Result Rendering**: Live outputs update instantly on keypress with custom accessibility tags.

### 3. Date & Time Calculation Suite (7 Pro Planners)
*   **Date Difference**: Determine exact duration in years, months, days, hours, and minutes between two calendars.
*   **Add & Subtract Days**: Project calendar timeline dates forwards or backwards.
*   **Age Calculator**: High-precision age representation down to weeks and days alongside countdown times to upcoming birthdays.
*   **Countdown Tracker**: Live time counter towards strategic goals or milestone dates.
*   **Time Zone Converter**: Interactively pair timezones and inspect corresponding local offsets across coordinates.
*   **Business Working Days**: Counts weekdays by excluding standard Saturdays and Sundays.
*   **Unix Epoch Converter**: Seamlessly convert epoch integer seconds into structured UTC date configurations.

### 4. Advanced Finance Planner (10 Integrated Tools)
Includes tailored planners complete with custom-rendered canvas visualizers representing interest/amortization structures:
*   `Loan EMI` | `Simple & Compound Interest` | `SIP Growth Plan` | `ROI / CAGR Rate Calculator` | `GST / Tax Apportioner` | `Discount & Markup Marginator` | `Tip & Bill Splitter` | `Salary CTC Breakup` | `Inflation Adjusted Value` | `FD/RD Maturity Scheduler`.

### 5. Unified Responsive Theming & Custom Settings
*   **Adaptive Metrics Framework**: Spacing, text scales, grid columns, and physical keys dynamically resize based on device bounds (supporting compact mobile, landscape modes, and landscape tablets/foldables).
*   **Dynamic Accent Selector**: Switch colorways globally (e.g., *Neon Cyan*, *Nebula Purple*, *Aurora Coral*, *Cosmic Amber*).
*   **Decimal Rounding Preferences**: Dynamically choose and enforce custom floating-point precision layers from settings (persisted across restarts).
*   **Secure History Pin Lock**: Password-protect your personal calculation and financial audit log history sheets.

---

## 🏛️ Architectural Representation
The development structure utilizes a strictly decoupled, highly modular **MVVM** pattern combined with **Clean Repository abstractions**:

```
 ┌─────────────────────────────────────────────────────────┐
 │                       VIEWS (UI)                        │
 │  Jetpack Compose, Responsive Dimensions, Material 3      │
 └────────────────────────────┬────────────────────────────┘
                              │
          Observes state      │  Dispatches click/input events
          & UI Flows          ▼
 ┌─────────────────────────────────────────────────────────┐
 │                       VIEWMODELS                        │
 │  State Management, Coroutines, Flow Trigger Streams     │
 └────────────────────────────┬────────────────────────────┘
                              │
          Request Data        │  Expose Data Models / Entities
          Operations          ▼
 ┌─────────────────────────────────────────────────────────┐
 │               REPOSITORIES (Abstractions)               │
 │  Single Source of Truth, SharedPreferences Delegates    │
 └────────────────────────────┬────────────────────────────┘
                              │
               ┌──────────────┴──────────────┐
               ▼                             ▼
 ┌───────────────────────────┐ ┌───────────────────────────┐
 │        LOCAL DATA         │ │        REMOTE API         │
 │  Room Database, SQLite    │ │  Retrofit Rate Sync       │
 └───────────────────────────┘ └───────────────────────────┘
```

---

## 📁 Source Layout
```
/app/src/main/java/com/example
├── CalculatorApplication.kt     # Declares and builds the Room Database on startup
├── MainActivity.kt              # App Entry Point, handles View Pager transitions
├── data
│   ├── AppDatabase.kt           # Room Database schema definition
│   ├── CurrencyRateEntity.kt    # Exchange rate storage model
│   ├── HistoryDao.kt            # Room DAO for historical calculations
│   ├── HistoryEntity.kt         # Database model storing calculation logs
│   ├── HistoryRepository.kt     # Orchestrates historical queries
│   └── SettingsRepository.kt    # SharedPreferences wrapper (preserves decimals, locks, pins)
├── ui
│   ├── calculator               # UI rendering & state loops for calculator
│   ├── converter                # Unit converters (length, mass, and currency rates)
│   ├── date                     # 7 advanced calendar and timezone sub-calculators
│   ├── finance                  # 10 comprehensive investment and EMI solvers with custom charts
│   ├── settings                 # Screen settings, themes, pinning options
│   └── theme                    # Centralized theme definitions & Adaptive sizing metric providers
└── util
    ├── CalculatorEngine.kt      # Mathematical formula parsing & scientific evaluation
    └── UnitConverter.kt         # Physical unit conversion functions
```

---

## 🚀 Local Developer Setup

To set up the project locally on your machine, follow these steps:

### Prerequisites
*   **Android Studio** (Koala / Ladybug or newer recommended)
*   **Java Development Kit (JDK)**: Minimum JDK 17 (Java 17) configured in system environment and Android Studio.
*   **Gradle**: Configured automatically via Gradle wrapper.

### Step 1: Clone the Repository
```bash
git clone https://github.com/your-username/aistudio-calculator-suite.git
cd aistudio-calculator-suite
```

### Step 2: Open and Sync Android Studio
1.  Open Android Studio.
2.  Choose **File > Open**, select the root directory of the project, and click **Open**.
3.  Let Android Studio resolve dependencies and perform the initial Gradle project synchronization.

### Step 3: Configure Environment Keys (Optional)
The application handles remote currency conversion smoothly. If you have active API keys, configure them via the secure `.env` file referencing `.env.example`.

---

## 🛠️ Compilation, Testing and Build Commands

Automate builds, code sanitation checks, and unit test suites from your terminal:

### Compilation & Syntactic Checks
Verify if all Jetpack Compose screens compiles successfully without warnings:
```bash
gradle compileDebugKotlin
```

### Run Local JVM Unit Tests
Run standard and Roborazzi tests to ensure mathematical operations remain flawless:
```bash
gradle :app:testDebugUnitTest
```

### Build Production APK / AAB
Generate a signed, performance-optimized release application installer package:
```bash
# To compile the debug installer package:
gradle assembleDebug

# To compile a production release package (AAB):
gradle bundleRelease
```
Once completed, the compiled outputs are located in:
`app/build/outputs/apk/debug/` and `app/build/outputs/bundle/release/`.

---

## 🔐 Advanced Device and Testing Safety
1.  **Strict Security Practices**: Secrets are completely decoupled from source control. Zero hardcoded keys exist in code.
2.  **Graceful Degrades**: If zero network access is detected during currency conversion, local Room database tables automatically fallback to the last successfully cached currency rate exchange list.
3.  **Arithmetic Resilience**: All core functions catch syntax, floating anomalies, zero division, and domain violations dynamically, displaying visual "Error" states without terminating the user's view context.

## 📄 License
This application is distributed under the standard corporate MIT License. Check details corresponding with engineering guidelines.
