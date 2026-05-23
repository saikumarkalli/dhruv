# Calculator & Unit Converter App — Codebase Documentation

An elegant, high-performance Android application built with **Kotlin, Jetpack Compose, Room Database, and Material 3 design principles**. The application boasts a sophisticated dark theme, offline history management, complex unit/currency conversion, and an intuitive, gesture-driven layout.

---

## 1. Architectural Blueprint
The architecture follows a strict, highly decoupled **MVVM (Model-View-ViewModel)** blueprint adhering to **Clean Architecture** guidelines:

```
[ UI / Jetpack Compose ] (Views)
          │▲
          ││ State Flow / Events
          ▼│
     [ ViewModels ] (CalculatorViewModel, ConverterViewModel)
          │▲
          ││ Repository Pattern / Query Execution
          ▼│
     [ Repositories ] (Settings, Historical logs, API sync)
          │▲
          ││ Data Mapping / Storage Interfaces
          ▼│
  ┌───────┴──────┐
  ▼              ▼
[ Room Db ]    [ Retrofit Api ] (Local/Remote)
```

---

## 2. Core Modules & Directory Layout

### `com.example`
- **`CalculatorApplication.kt`**: Application configuration class initializing the centralized Room Database and repositories.
- **`MainActivity.kt`**: App Entry Point. Houses the minimalist, gesture-oriented `HorizontalPager` enabling seamless swiping transitions between **Calculator**, **Converter**, and **Settings** screens.

### `com.example.data`
- **`AppDatabase.kt`**: Central relational database (powered by Room) declaring entities and direct access objects.
- **`HistoryEntity.kt`**: Relational Data Model storing all calculation expressions, resulting string, UNIX timestamps, and scientific categorization flag.
- **`HistoryDao.kt`**: Direct data operations mapper translating Room transactions to standard SQL commands.
- **`HistoryRepository.kt`**: Clean, non-blocking single-source-of-truth service injecting Room transaction records as reactive Coroutine Flows.
- **`SettingsRepository.kt`**: Data Persistence Service handling system preference options (such as angle measurement types RAD/DEG and floating-point decimal precision levels).
- **`CurrencyRateEntity.kt` / `CurrencyRepository.kt`**: Database configurations caching global exchange rates for robust, network-resilient offline calculations.

### `com.example.util`
- **`CalculatorEngine.kt`**: Custom tokenizer and expression parser supporting parenthesis nesting, operator hierarchy modeling, trigonometric utilities (sin, cos, tan, arc-functions) in both standard radian and degree spaces, log bases, and floating decimals.

### `com.example.ui.calculator`
- **`CalculatorScreen.kt`**: Prime interactive screen rendering. Implements custom drag gestures (swiping up/down to seamlessly reveal the extra scientific key suite, swiping left over the calculations console to transition into the scrolling history view).
- **`CalculatorViewModel.kt`**: Main calculation event loop. Manages logical state chaining (preventing double entries or appending equations, continuing expressions by clicking interactive mathematical operator keys on previously calculated numbers).

---

## 3. Advanced Gestural Navigation

We designed and incorporated a highly fluid, gestural-input scheme allowing convenient single-handed access across complex functions:

1. **Horizontal View Switching**: Swipe left/right over main screens to transition from the core calculator to unit/currency converter blocks.
2. **Interactive Scientific Drawer**:
   - Swiping **UP** on the keyboard panel expands the hidden scientific keys.
   - Swiping **DOWN** on the keyboard panel securely retracts it.
3. **Calculation History Pull-Sheet**:
   - Double-tap or Swipe **LEFT** on the calculated results terminal to pull up the complete full-screen Date-Wise history list.
   - Swipe **RIGHT** or drag the structural overhead grab-bar **DOWN** to quickly dismiss and return to the main console.

---

## 4. Key APIs and Data Structures

### `CalculatorEngine.evaluate(expr: String, isDegree: Boolean): Double`
High-performance parser utilizing custom tokenizers to validate, compute, and deliver mathematical answers.

**Parameters**:
- `expr`: Mathematical formulation text typed by user.
- `isDegree`: Calculation orientation flag (Degree-based vs. Radians).

---

## 5. Development Details and Code Examples

### Query Grouping (Date-Wise History Mapping)
History records are dynamically parsed and grouped into friendly localized buckets (*"Today"*, *"Yesterday"*, or formatted dates) using:

```kotlin
private fun formatHistoryHeader(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val sdf = java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault())
    val todaySdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    val itemDateStr = todaySdf.format(date)
    val todayDateStr = todaySdf.format(java.util.Date())
    val yesterdayDateStr = todaySdf.format(java.util.Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000))
    
    return when (itemDateStr) {
        todayDateStr -> "Today"
        yesterdayDateStr -> "Yesterday"
        else -> sdf.format(date)
    }
}
```

### Sophisticated Dark System Color Palette (`Color.kt`)
The application is wrapped in eye-safe, professional shades:
- `SophAppBg`: `#111315` (Deep Charcoal Slate)
- `SophOpKeyBg`: `#00457C` (Deep Ocean Blue)
- `SophEqualsBg`: `#D3E3FD` (Electric Blue highlight)
- `SophNumKeyBg`: `#2D3035` (Muted mid-grey keypad numbers)
