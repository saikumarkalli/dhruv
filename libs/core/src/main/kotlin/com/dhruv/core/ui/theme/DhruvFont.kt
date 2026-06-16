package com.dhruv.core.ui.theme

/** Global font-family preference stored in DataStore key "font_family". */
enum class DhruvFont {
    /** System default font family. */
    DEFAULT,

    /**
     * Rounded font (placeholder: maps to SansSerif until a bundled font ships).
     * Follow-up: bundle a real rounded typeface as a raw resource.
     */
    ROUNDED,

    /** Monospace font family. */
    MONO
}
