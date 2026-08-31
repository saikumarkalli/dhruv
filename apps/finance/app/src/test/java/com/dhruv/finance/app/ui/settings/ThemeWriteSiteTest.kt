package com.dhruv.finance.app.ui.settings

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * FR-020 (analysis finding C1, previously uncovered): Settings MUST remain the only place the
 * app's theme is chosen. Scans every `.kt` file under `src/main` for an actual theme-write
 * mechanism — `setDarkModePreference(` (the DataStore write) or `copy(theme = ` (an `AppSettings`
 * mutation) — and fails if one is found outside `ui/settings/`. Deliberately narrower than "any
 * `theme =`": reading and applying the current theme (e.g. `DhruvTheme(theme = appSettings.theme)`
 * in `MainActivity`) is not a write and must not be flagged.
 */
class ThemeWriteSiteTest {
    @Test
    fun `no theme write call exists outside the settings package`() {
        val mainSrc = File("src/main/java/com/dhruv/finance/app")
        assertTrue("expected ${mainSrc.absolutePath} to exist", mainSrc.exists())

        val offenders =
            mainSrc
                .walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .filterNot { it.path.replace('\\', '/').contains("/ui/settings/") }
                .filter { file ->
                    val text = file.readText()
                    text.contains("setDarkModePreference(") || text.contains("copy(theme = ")
                }.map { it.relativeTo(mainSrc) }
                .toList()

        assertTrue("theme write call(s) found outside ui/settings/: $offenders", offenders.isEmpty())
    }
}
