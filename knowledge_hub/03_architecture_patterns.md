# Architectural Patterns & Design

The Dhruv Calculator & Conversions application is structured primarily around the **Model-View-ViewModel (MVVM)** architectural pattern, combined with the **Spec-Driven Development (SDD)** contract model. This ensures a clean separation of concerns, testability, and modularity.

## 1. MVVM Architecture

The application cleanly separates the UI layer from the business logic layer across its core features:

### The View Layer (Jetpack Compose)
- **Role**: Responsible exclusively for observing state and rendering the user interface. It captures user intent and delegates actions to the ViewModel.
- **Implementation**: Written in declarative Kotlin using Jetpack Compose (e.g., `CalculatorScreen.kt`, `ConverterScreen.kt`).
- **Data Binding**: Views observe `StateFlow` structures exposed by the ViewModel. When the state changes, Compose automatically triggers a recomposition of the UI elements.

### The ViewModel Layer
- **Role**: Acts as the intermediary between the View and the Data/Domain layer. It manages UI state, survives configuration changes, and executes core business logic formatting.
- **Implementation**: Classes extending Android's `ViewModel` (e.g., `CalculatorViewModel.kt`, `ConverterViewModel.kt`).
- **Concurrency**: Leverages `viewModelScope` to launch Coroutines for background tasks, such as database inserts or network calls, without blocking the main UI thread.

### The Model (Data/Repository) Layer
- **Role**: Responsible for data fetching, caching, persistence, and complex mathematical evaluations.
- **Implementation**: Repositories (e.g., `HistoryRepository`, `CurrencyRepository`) that abstract away the data sources (Room DB, Network, SharedPreferences).

## 2. Spec-Driven Development (SDD) Contracts

A strict interface-driven approach is enforced across the application's core logic engines to guarantee behavior aligns with documented specifications.

### Interface Boundaries
The codebase defines explicit interfaces to establish strict contracts:
- `ICalculatorEngine`: Dictates mathematical evaluation and factorial handling.
- `IUnitConverter`: Manages physical conversions (Length, Mass).
- `ICurrencyRepository`: Outlines the contracts for fetching, caching, and retrieving currency exchange rates.

### Dependency Injection (DI)
ViewModels are injected with these interfaces rather than concrete class implementations. For instance, `ConverterViewModel` depends on `ICurrencyRepository`. This ensures that the UI logic remains decoupled from the specific implementations of network fetching or local SQLite caching.

## 3. The Repository Pattern

The application uses the Repository pattern to centralize data access and provide a clean API to the ViewModels.

- **`HistoryRepository`**: Manages local calculation logs. It abstracts the `HistoryDao` and provides Flow streams of active and recycled history items.
- **`CurrencyRepository`**: Implements a self-healing strategy. It attempts to fetch rates from a primary API; if it fails, it tries a fallback API; if both fail, it loads cached data from the local SQLite database.
- **`SettingsRepository`**: Abstracts the interactions with Android's `SharedPreferences`, exposing configurations (themes, rounding precision, security PINs) as reactive `StateFlow` streams.

## 4. Current Architectural Gaps

While the Calculator and Converter modules follow a strict MVVM pattern, the **Date & Time** and **Finance** modules currently implement all mathematical logic and state management inline within their respective Composable screens (`DateScreen.kt`, `FinanceScreen.kt`). 
- **Future Refactoring**: These screens are prime candidates for future refactoring to extract their logic into dedicated `DateViewModel` and `FinanceViewModel` classes to achieve full architectural consistency.
