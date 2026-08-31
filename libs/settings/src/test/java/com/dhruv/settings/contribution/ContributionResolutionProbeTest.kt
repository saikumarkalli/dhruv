package com.dhruv.settings.contribution

import org.junit.Assert.assertEquals
import org.junit.Test
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/**
 * Research R1's one mechanism assumption worth proving before anything is built on top of it.
 *
 * **Finding (recorded here, not just in research.md): an unqualified `single { }` collides.**
 * Koin 3.5.6 keys its definition registry by `(type, qualifier)`. Two modules each declaring
 * `single { SettingsContribution(...) }` with no qualifier both resolve to the same key
 * `(SettingsContribution, null)` — the second registration silently overwrites the first, and
 * `getAll<SettingsContribution>()` then returns only one entry. This was caught by this exact test
 * failing first (`expected:[moduleA, moduleB] but was:[moduleB]`) before any contribution shipped.
 *
 * The fix — and the convention every real contribution (T033–T035 onward) MUST follow — is R1's
 * documented fallback: **qualify each contribution's `single` by its own `moduleKey`**
 * (`named(moduleKey)`). `getAll<T>()` does not filter by qualifier, so every qualified definition
 * of the same type is still returned; only the *unqualified* slot collides.
 */
class ContributionResolutionProbeTest {
    @Test
    fun `getAll resolves every qualified SettingsContribution across all loaded Koin modules`() {
        val fromModuleA = SettingsContribution(moduleKey = "moduleA", title = 1, summary = 2, order = 0, groups = emptyList())
        val fromModuleB = SettingsContribution(moduleKey = "moduleB", title = 3, summary = 4, order = 1, groups = emptyList())

        val moduleA = module { single(qualifier = named("moduleA")) { fromModuleA } }
        val moduleB = module { single(qualifier = named("moduleB")) { fromModuleB } }

        val app = koinApplication { modules(moduleA, moduleB) }
        try {
            val resolved = app.koin.getAll<SettingsContribution>()
            assertEquals(setOf("moduleA", "moduleB"), resolved.map { it.moduleKey }.toSet())
        } finally {
            app.close()
        }
    }
}
