# Dhruv Finance — Privacy Policy

**Policy version:** 2.0
**Effective:** 2026-07-16
**Contact:** kallileelasaikumar@gmail.com

Dhruv Finance is a personal-use financial tracking ecosystem accessible via the Android application and the Web application (hosted on Vercel). This policy describes what data the applications handle. By using either application, you consent to these terms as required by the India Digital Personal Data Protection (DPDP) Rules.

## What is stored on your device

### Android App
- **Calculator history** (expressions, results, optional tags and notes) — stored in a local database. You can export it, clear it, and deleted entries are purged after 30 days.
- **Settings and preferences** (theme, colors, number format, feature toggles) — stored locally.
- **Your own API keys, if you add them** (for example a Gemini API key) — stored in encrypted Keystore storage. They are never transmitted anywhere except to the service they belong to.

### Web App
- **Session token** — stored in your browser's local storage to keep you signed in. Cleared when you sign out. Never transmitted to anyone other than Supabase.
- **Calculator session history** — stored in memory only; cleared when you close the tab.
- **Theme preference** — stored in local storage for your convenience.

None of this local data is transmitted to the developer. There are no ads and no third-party marketing SDKs.

## What leaves your device, and when

| Data | Recipient | When | Consent |
|---|---|---|---|
| Text you type into the **AI assistant** | Google (Gemini API) | Only after you accept the in-app consent screen, and only when you invoke an AI action | Explicit, shown before first use; revocable |
| **Currency-rate requests** | Public exchange-rate APIs | When the currency converter refreshes rates | No personal data is sent — only the currency pair |
| **Crash reports & performance traces** (Android) | Firebase | When enabled in release builds | Tagged by module, contains no financial values or PII |
| **Page views & web vitals** (Web) | Vercel Analytics | Automatically on the web app | First-party, no tracking cookies, no PII |
| **Tracker Data** (Assets, Liabilities, Transactions, etc.) | Supabase (PostgreSQL) | When using the Net Worth Tracker or related cloud features | Explicit consent before sign in. Encured via RLS (Row Level Security). |

The applications make **no other network calls**.

## Your rights (DPDP)

- **Consent first:** nothing you enter leaves the device without a prior, explicit consent screen.
- **Withdraw consent:** AI and tracker features re-gate when consent is withdrawn.
- **Erasure:** "Delete my data" removes all your tracker rows from the cloud. "Delete my account" hard-deletes your account and all data within 7 days.
- **Questions or requests:** email the contact address above.

## Data security

Local preferences and keys (Android) are stored using encrypted storage. Network calls use HTTPS only; cleartext traffic is disabled. Tracker data stored in Supabase is secured by Row Level Security (RLS) ensuring that only your authenticated user ID can read or modify your data.

## Changes

Material changes increase the policy version and are listed here:

| Version | Date | Change |
|---|---|---|
| 2.0 | 2026-07-16 | Added coverage for the Web Application and Supabase cloud tracker data. |
| 1.0 | 2026-07-12 | Initial policy: local-first calculators, consent-gated AI, keyless currency rates |
