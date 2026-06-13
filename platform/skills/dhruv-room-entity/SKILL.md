---
name: dhruv-room-entity
description: Create a Room entity with full data layer for the Dhruv platform. Use whenever the user needs to persist data, create a database entity, add a Room table, create a DAO, repository, or data layer for any Dhruv feature. Also triggers on "store data", "save to database", "add persistence", "create entity", "data class for Room". Creates the entity (implementing DhruvEntity with HLC), DAO, Repository interface + impl, Hilt wiring, Room migration, and test stubs — all compliant with platform contracts. Always use this instead of creating Room entities manually.
---

# Dhruv Room Entity

Creates a complete, contract-compliant Room data layer for a Dhruv feature.

## Before you start

1. Read `platform/contracts/DhruvEntity.kt` — your entity MUST implement this.
2. Identify: which app, which feature, what data fields.
3. If vault entity: do NOT implement DhruvEntity (vault entities are separate).

## What gets created

For an entity `{Name}` in app `{app}`, feature `{feature}`:

```
apps/{app}/feature/{feature}/src/main/java/com/dhruv/{app}/feature/{feature}/
├── data/
│   ├── {Name}Entity.kt          # Room entity implementing DhruvEntity
│   ├── {Name}Dao.kt             # Room DAO
│   ├── {Name}Repository.kt      # Interface (feature depends on this)
│   └── {Name}RepositoryImpl.kt  # Implementation (Hilt-bound)
```

Plus updates to:
- `AppDatabase.kt` — add entity + DAO abstract method
- Room migration if DB already exists

## Templates

### {Name}Entity.kt (standard — implements DhruvEntity)
```kotlin
package com.dhruv.{app}.feature.{feature}.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dhruv.core.data.DhruvEntity
import java.util.UUID

@Entity(
    tableName = "{name}s",
    indices = [Index(value = ["userId"])]  // Indexed for future Dhruv ID migration
)
data class {Name}Entity(
    @PrimaryKey
    override val id: String = UUID.randomUUID().toString(),
    override val userId: String = "local",
    override val createdAt: Long = System.currentTimeMillis(),
    override val updatedAt: Long = System.currentTimeMillis(),
    override val hlc: String = "",  // Set by HlcClock from :libs:core
    override val isSynced: Boolean = false,
    override val isDeleted: Boolean = false,
    // --- Feature-specific fields below ---
    // val title: String = "",
    // val content: String = "",
) : DhruvEntity
```

### {Name}Entity.kt (VAULT variant — does NOT implement DhruvEntity)
```kotlin
package com.dhruv.vault.feature.{feature}.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "{name}s")
data class {Name}Entity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // --- Feature-specific fields below ---
    // val title: String = "",
)
// NOTE: Vault entities are NEVER synced, NEVER implement DhruvEntity
```

### {Name}Dao.kt
```kotlin
package com.dhruv.{app}.feature.{feature}.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface {Name}Dao {

    @Query("SELECT * FROM {name}s WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<{Name}Entity>>

    @Query("SELECT * FROM {name}s WHERE id = :id")
    suspend fun getById(id: String): {Name}Entity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: {Name}Entity)

    @Query("UPDATE {name}s SET isDeleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long = System.currentTimeMillis())

    // Hard delete — only called by tombstone GC or DPDP erasure, never by UI
    @Query("DELETE FROM {name}s WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("SELECT * FROM {name}s WHERE isDeleted = 1 AND updatedAt < :cutoff")
    suspend fun getTombstonesBefore(cutoff: Long): List<{Name}Entity>
}
```

### {Name}Repository.kt (interface)
```kotlin
package com.dhruv.{app}.feature.{feature}.data

import kotlinx.coroutines.flow.Flow

interface {Name}Repository {
    fun observeAll(): Flow<List<{Name}Entity>>
    suspend fun getById(id: String): {Name}Entity?
    suspend fun save(entity: {Name}Entity)
    suspend fun delete(id: String)
}
```

### {Name}RepositoryImpl.kt
```kotlin
package com.dhruv.{app}.feature.{feature}.data

import com.dhruv.core.time.HlcClock
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class {Name}RepositoryImpl @Inject constructor(
    private val dao: {Name}Dao,
    private val hlcClock: HlcClock
) : {Name}Repository {

    override fun observeAll(): Flow<List<{Name}Entity>> = dao.observeAll()

    override suspend fun getById(id: String): {Name}Entity? = dao.getById(id)

    override suspend fun save(entity: {Name}Entity) {
        val stamped = entity.copy(
            updatedAt = System.currentTimeMillis(),
            hlc = hlcClock.now()
        )
        dao.upsert(stamped)
    }

    override suspend fun delete(id: String) = dao.softDelete(id)
}
```

### Hilt wiring (add to feature's di/ module or app-level)
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class {Name}DataModule {
    @Binds
    abstract fun bind{Name}Repository(
        impl: {Name}RepositoryImpl
    ): {Name}Repository
}
```

### AppDatabase update
```kotlin
// Add to @Database entities list:
@Database(entities = [..., {Name}Entity::class], version = N+1)

// Add abstract DAO method:
abstract fun {name}Dao(): {Name}Dao
```

### Room migration
```kotlin
val MIGRATION_N_N1 = object : Migration(N, N+1) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `{name}s` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `userId` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `hlc` TEXT NOT NULL,
                `isSynced` INTEGER NOT NULL DEFAULT 0,
                `isDeleted` INTEGER NOT NULL DEFAULT 0
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_{name}s_userId` ON `{name}s` (`userId`)")
    }
}
// RULE: addColumn only, never dropTable in upgrades
```

### Test stub
```kotlin
@RunWith(AndroidJUnit4::class)
class {Name}DaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: {Name}Dao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        dao = db.{name}Dao()
    }

    @After
    fun teardown() = db.close()

    @Test
    fun `upsert and observe`() = runTest {
        val entity = {Name}Entity(/* ... */)
        dao.upsert(entity)
        val result = dao.observeAll().first()
        assertEquals(1, result.size)
    }

    @Test
    fun `soft delete excludes from observeAll`() = runTest {
        val entity = {Name}Entity(id = "test-1")
        dao.upsert(entity)
        dao.softDelete("test-1")
        val result = dao.observeAll().first()
        assertTrue(result.isEmpty())
    }
}
```

## Rules

- `userId` is ALWAYS indexed (future Dhruv ID migration).
- `hlc` is set by `HlcClock` on every write, never by the caller.
- Soft-delete is the default; hard-delete only for tombstone GC / DPDP erasure.
- Features access data ONLY through the Repository interface (ArchUnit enforced).
- Vault entities: no DhruvEntity, no userId, no hlc, no isSynced.
- Migration: addColumn only, never dropTable.
