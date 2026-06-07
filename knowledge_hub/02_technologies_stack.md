# Technology Stack & Frameworks

The Dhruv Calculator & Conversions application is built using modern Android development practices. This document outlines the primary languages, frameworks, libraries, and tools utilized in the codebase.

## 1. Core Languages
- **Kotlin**: The primary programming language used across the entire application. It heavily leverages Kotlin's concise syntax, null safety, and functional programming features.
- **Java (Interoperability)**: Standard Java libraries (e.g., `java.time`, `java.util.Calendar`, `java.text.DecimalFormat`) are utilized for complex date, time, and formatting calculations.

## 2. User Interface (UI)
- **Jetpack Compose**: The modern, declarative UI toolkit used for building the application's interface natively in Kotlin.
  - No XML layouts are used; the entire UI is defined via `@Composable` functions.
- **Material Design 3 (Material You)**: The application utilizes the `androidx.compose.material3` libraries to provide a premium, dynamic, and responsive visual aesthetic. It makes heavy use of:
  - `Card`, `Scaffold`, `TextField`, and `Button` components.
  - Custom theming palettes with support for Dark Mode toggles.
  - Fluid canvas drawing (e.g., the hollow ring Canvas pie chart in the Loan EMI calculator).

## 3. Data Persistence & Caching
- **Room Database**: The abstraction layer over SQLite used for persistent local data storage.
  - Used for storing calculation logs (`HistoryEntity`).
  - Used for caching offline currency exchange rates (`CurrencyRateEntity`).
  - Implements DAO (Data Access Object) interfaces and asynchronous flow returns.
- **SharedPreferences**: Used for storing lightweight user configuration settings (e.g., decimal precision, active theme colors, history PIN locks).

## 4. Networking & API Integration
- **Retrofit**: A type-safe HTTP client for Android and Java used to define the API interface.
- **OkHttp**: The underlying HTTP client handling connection pooling, timeouts (configured to 15 seconds), and transparent GZIP.
- **Moshi**: A modern JSON library for Kotlin and Java used in conjunction with Retrofit (`MoshiConverterFactory`) to parse the incoming API JSON responses into Kotlin data objects.

## 5. Concurrency & Asynchronous Programming
- **Kotlin Coroutines**: Used extensively for managing background threads and asynchronous operations (e.g., database queries, network requests). Operations are primarily shifted to `Dispatchers.IO`.
- **StateFlow & MutableStateFlow**: The core state-holder observable flow mechanisms used within the `ViewModel` layer to expose reactive data streams to the Compose UI.

## 6. Testing
- **JUnit 4**: The testing framework used for writing local, JVM-based unit tests.
- **Spec-First Unit Testing**: A rigorous unit testing methodology enforcing behavior coverage (e.g., parser validation, domain error checking) prior to implementation. Tests are named using the `[Method]_[Condition]_[ExpectedResult]` convention.
