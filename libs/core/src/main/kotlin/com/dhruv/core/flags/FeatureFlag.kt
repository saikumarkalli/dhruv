package com.dhruv.core.flags

/**
 * Declarative description of a feature flag, mirroring one entry in
 * `platform/feature-flags/<app>.json`.
 *
 * @property enabled         master on/off switch for the feature.
 * @property minVersion      lowest app version (semver) at which the feature may surface. A flag may
 *                           be [enabled] yet stay hidden until the running app reaches this version —
 *                           this lets a flag ship "on" ahead of the release that actually exposes it.
 *                           Defaults to "0.0.0" (no version gate).
 * @property requiresConsent true when the feature sends data off-device, so a DPDP consent gate must
 *                           precede any such call (PLATFORM.md §8). Informational at the flag layer;
 *                           the consent UI itself lives in the feature screen.
 */
data class FeatureFlag(
    val enabled: Boolean,
    val minVersion: String = "0.0.0",
    val requiresConsent: Boolean = false,
)
