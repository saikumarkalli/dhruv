# Database Architecture & Schema

The Dhruv Calculator & Conversions application utilizes **Room**, Android's SQLite abstraction library, to handle robust, local data persistence. The database is initialized via `AppDatabase.kt`.

## 1. The `calculation_history` Table

This table logs users' mathematical calculations. It allows users to review past work, star favorites, and categorize data.

### Schema (`HistoryEntity.kt`)
| Column Name | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `Int` | Primary Key, Auto-Increment | Unique identifier for each log entry. |
| `expression` | `String` | Non-Null | The input mathematical formula (e.g., `2+2`). |
| `result` | `String` | Non-Null | The evaluated output (e.g., `4.0`). |
| `timestamp` | `Long` | Non-Null | Unix epoch time when the calculation occurred. |
| `isFavorite` | `Boolean` | Default: `false` | Indicates if the user starred this entry. |
| `tag` | `String?` | Nullable | An optional category tag assigned by the user. |
| `note` | `String?` | Nullable | An optional text description. |
| `isDeleted` | `Boolean` | Default: `false` | Soft-delete flag moving it to the Recycle Bin. |
| `deletedAt` | `Long?` | Nullable | Unix epoch time when soft-deleted (used for auto-pruning). |

### Operations & Behaviors (`HistoryDao.kt`)
- **Queries**: Retrieves history filtered by active items (`isDeleted = false`) and recycle bin items (`isDeleted = true`). Supports full-text search across tags, notes, and expressions.
- **Recycle Bin**: Instead of hard-deleting, items are flagged `isDeleted = true` and populate a separate Recycle Bin view.
- **Auto-Pruning mechanism**: A periodic database cleaning function permanently removes entries where `isDeleted = true` AND `deletedAt` is older than 30 days.

## 2. The `currency_rates` Table

This table caches the latest exchange rates retrieved from external APIs. It ensures the Currency Converter functions reliably even when the device is offline.

### Schema (`CurrencyRateEntity.kt`)
| Column Name | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `currencyCode` | `String` | Primary Key | Standard 3-letter ISO code (e.g., `USD`, `INR`). |
| `rate` | `Double` | Non-Null | Exchange rate relative to a base currency (usually USD). |
| `lastUpdated` | `Long` | Non-Null | Unix epoch time of the last successful API fetch. |

### Operations & Behaviors (`CurrencyDao.kt`)
- **Upsert Strategy**: Utilizes `OnConflictStrategy.REPLACE` to continually overwrite older rates with fresh data pulled from the API.
- **Validation**: The `lastUpdated` timestamp is actively checked by the Repository layer. If the cached data exceeds a predetermined age threshold (e.g., 24 hours), the application forces a fresh network synchronization.

## 3. SharedPreferences Configuration

While not a relational SQL table, the application extensively uses SharedPreferences to persist non-relational configurations:
- **`PREF_THEME_MODE`**: (Int) Stores the user's dark/light mode preference.
- **`PREF_DECIMAL_PLACES`**: (Int) Configures global rounding logic (defaults to 4).
- **`PREF_PIN_LOCK`**: (String) Stores a hashed 4-digit PIN for history access.
- **`PREF_ACCENT_COLOR`**: (String/Hex) Configures UI dynamic color overrides.
