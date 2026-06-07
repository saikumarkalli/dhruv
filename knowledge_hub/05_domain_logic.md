# Domain Logic & Mathematical Mechanics

This document outlines the core algorithms, parsers, and specialized logic that power the Dhruv Calculator & Conversions application.

## 1. Custom Mathematical Parser

The `CalculatorEngine` does not rely on Android's basic evaluation libraries; instead, it implements a custom **Recursive Descent Top-Down Parser**. This allows for strict evaluation rules, domain validations, and support for complex functions.

### Parsing Logic
- **Tokenization**: The engine reads characters sequentially, parsing strings into distinct numbers, operators, and functions.
- **Operator Precedence**: Strict enforcement of BODMAS/PEMDAS. Multiplication/Division binds tighter than Addition/Subtraction. Exponentiation (`^`) binds tighter than all standard operators.
- **Implicit Multiplication**: The engine detects patterns like `2(3)` or `2sin(45)` and automatically injects multiplication operators (`2*(3)`).
- **Trigonometric Modes**: Toggles between Radian and Degree evaluation. Converts inputs via `Math.toRadians()` when operating in Degree mode.

### Domain Interception & Error Handling
Mathematical domain violations are preemptively caught and throw `IllegalArgumentException` with user-friendly strings:
- Division by Zero: `2 / 0` -> `Error: Division by Zero`.
- Square Root of Negatives: `sqrt(-4)` -> `Error: Real numbers only`.
- Invalid Arc-trig Domains: `asin(2)` -> `Error: Domain is [-1, 1]`.

## 2. API Self-Healing Mechanism

The Currency Converter implements a "graceful degradation" pipeline to guarantee continuous operability.

1. **Primary Network Call**: Attempts to hit the primary Exchange Rate API endpoint.
2. **Fallback Network Call**: If a timeout, HTTP 500, or parsing failure occurs, the engine automatically routes the request to a secondary, pre-configured API endpoint.
3. **Database Cache**: If the device is completely offline or both APIs fail, the repository pulls the latest known values from the local SQLite (`currency_rates` table).
4. **Validation**: Emits `Success`, `OfflineCached`, or `Error` states to the ViewModel based on how the data was acquired.

## 3. Financial Formulas

The Finance Planner module implements standard accounting formulas heavily utilizing Java's `Math.pow()` for exponentiation:

- **Loan EMI**: 
  - Formula: `[P x R x (1+R)^N]/[(1+R)^N-1]`
  - Where `P` = Principal, `R` = Monthly Interest Rate (`AnnualRate / 12 / 100`), `N` = Tenure in Months.
- **Compound Interest**: 
  - Formula: `A = P (1 + r/n)^(nt)`
- **SIP (Systematic Investment Plan) Growth**: 
  - Formula: `M = P × ({[1 + i]^n - 1} / i) × (1 + i)`
  - Calculates future valuation based on monthly compounded investments.

## 4. Date & Time Engine

The Date & Time planners utilize `java.util.Calendar` and `java.text.SimpleDateFormat`.

- **Working Days Calculation**: Iterates linearly through dates, checking the `Calendar.DAY_OF_WEEK`. Bypasses `Calendar.SATURDAY` and `Calendar.SUNDAY`.
- **Time Zone Offsets**: Relies on `TimeZone.getTimeZone("id")` mappings (e.g., `Asia/Calcutta`, `America/New_York`) to add/subtract hours directly relative to the standard UTC baseline.
