package com.dhruv.core.flags

/**
 * Minimal, lenient semantic-version holder used for feature-flag `minVersion` gating.
 *
 * Only the numeric `MAJOR[.MINOR[.PATCH…]]` head is compared; any pre-release/build suffix
 * (e.g. `-beta`, `+ci`) on a component is ignored, and missing components are treated as 0 so
 * `"1.2"` compares equal to `"1.2.0"`. Garbage components fall back to 0 rather than throwing —
 * a flag must never crash the app over a malformed version string.
 */
class SemVer private constructor(private val parts: List<Int>) : Comparable<SemVer> {

    override fun compareTo(other: SemVer): Int {
        val size = maxOf(parts.size, other.parts.size)
        for (i in 0 until size) {
            val cmp = parts.getOrElse(i) { 0 }.compareTo(other.parts.getOrElse(i) { 0 })
            if (cmp != 0) return cmp
        }
        return 0
    }

    companion object {
        /** Parses [raw] leniently; each component keeps only its leading digits, defaulting to 0. */
        fun parse(raw: String): SemVer = SemVer(
            raw.trim().split('.')
                .map { component -> component.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
                .ifEmpty { listOf(0) }
        )
    }
}
