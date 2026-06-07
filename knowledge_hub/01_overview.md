# Application Overview & Functionality

**Dhruv Calculator & Conversions** is a feature-rich, multi-mode calculation suite designed for Android. It operates as an all-in-one productivity and financial tool, providing far more capability than a standard calculator. 

## Core Capabilities

The application is divided into five major functionality domains:

### 1. Scientific Calculator
- **Mathematical Evaluation**: A custom-built recursive top-down parser handles complex mathematical formulas.
- **Operations**: Supports standard arithmetic, exponentiation (`^`), factorials (`!`), square roots (`sqrt`), logarithms (`log`, `ln`), and trigonometric functions (`sin`, `cos`, `tan`, `asin`, `acos`, `atan`).
- **Degree/Radian Modes**: Seamless toggling between radians and degrees for angle-based calculations.
- **Error Handling**: Graceful interception of domain violations (e.g., division by zero, square root of negative numbers, arc-sine domain errors).

### 2. Conversions (Physical & Currency)
- **Physical Metrics**: High-precision conversions for Length (Meters, Kilometers, Inches, Feet, etc.) and Mass (Kilograms, Grams, Pounds, Ounces, etc.).
- **Currency Exchange**: Real-time foreign exchange conversions with offline resilience. The app fetches rates dynamically and caches them, ensuring usability even without an internet connection. Includes fallback API layers.

### 3. Date & Time Planners
A comprehensive suite of 7 specialized date and time tools:
- **Date Difference**: Calculates duration between dates (years, months, weeks, days).
- **Add/Subtract Days**: Calculates a target date based on a starting date and day offset.
- **Age Calculator**: Detailed age breakdown and countdown to next birthday.
- **Countdown Tracker**: Live countdown to specific calendar events.
- **Time Zone Converter**: Converts times across major global zones (UTC, Calcutta, New York, London, Tokyo, Sydney).
- **Business Working Days**: Evaluates the number of weekdays between dates, excluding weekends.
- **Unix Epoch Converter**: Translates between human-readable dates and Unix timestamp integers.

### 4. Financial Investment Suite
A collection of 10 professional-grade financial calculators:
- **Loan EMI**: Computes monthly installments and provides a visual breakdown of principal vs. interest.
- **Simple & Compound Interest**: Side-by-side comparison of simple vs. compound yields.
- **SIP Growth**: Tracks future wealth based on monthly mutual fund contributions.
- **ROI / CAGR**: Calculates absolute return ratios and Compounded Annual Growth Rates.
- **GST / Tax**: Add or remove GST percentages from a base sum.
- **Discount & Markup**: Adjusts retail pricing for discounts or wholesale markups.
- **Tip & Bill Split**: Apportions dining bills and gratuity among a group.
- **Salary Breakup**: Evaluates gross CTC into estimated monthly take-home pay, factoring in basic PF and estimated taxes.
- **Inflation Adjusted**: Calculates future purchasing power based on inflation rates.
- **FD / RD Maturity**: Projects payouts for Fixed and Recurring Deposits.

### 5. Time Tools Suite
A collection of 3 productivity tracking tools:
- **Stopwatch**: Precision millisecond stopwatch with lap delta tracking.
- **Timer**: Multiple concurrent countdown timers with beautiful progress animations.
- **Alarm**: Device-level background alarms with optional math-equation dismissal puzzles.

### 6. Settings, Personalization & Security
- **Theming**: Selectable interface styles (System, Always Dark, Always Light) and section-specific accent color palettes.
- **Precision Control**: Adjustable decimal rounding parameters.
- **Calculation History**: Automatic logging of calculations with options to star favorites, add custom tags, and write notes. Features a soft-delete "Recycle Bin" that auto-prunes entries older than 30 days.
- **Security PIN**: Optional 4-digit PIN lock to secure access to the calculation history logs, protecting sensitive financial data.
