package com.dhruv.finance.onboarding

/**
 * Carries the Google Sign-In Web client id (Credential Manager's `GetGoogleIdOption.serverClientId`
 * / `clientId`) into [SignInScreen] without that screen — or this module — ever reading
 * `com.dhruv.finance.app.BuildConfig` directly. `:apps:finance:feature:onboarding:onboarding`
 * cannot depend on `:apps:finance:app` (the app depends on features, never the reverse), so the
 * value is sourced from `BuildConfig.GOOGLE_WEB_CLIENT_ID` in the app module's `platformModule`
 * (Koin) and handed down as a plain constructor arg — the same shape `PlatformModule.kt` already
 * uses for `GeminiRepository(BuildConfig.GEMINI_API_KEY)` and `SupabaseClientFactory`'s URL/key.
 */
data class GoogleSignInConfig(
    val webClientId: String,
)
