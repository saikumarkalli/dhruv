# Auth & Security SDD (06)

> **Status:** ACTIVE
> **Scope:** Defines authentication flows, DPDP compliance, and multi-platform security boundaries.

## 1. Authentication Flow

**Provider**: Google Identity via Supabase Auth (GoTrue).

### Android
- Leverages Android Credential Manager.
- Retrieves Google ID Token.
- Sends ID token to Supabase `signInWithIdToken()`.

### Web
- Uses OAuth PKCE Redirect flow.
- Redirects to Google, returns to app with an auth code.
- Supabase-js automatically exchanges the code for a JWT.

## 2. Session & Token Storage

| Platform | Storage Mechanism | Security |
|---|---|---|
| **Android** | `EncryptedDataStore` | Hardware Keystore encryption. |
| **Web** | `localStorage` | Mitigated against XSS via strict CSP. No financial data stored here. |

## 3. Web Security Headers

Applied via `vercel.json` and `<meta>` tags:
- **Content-Security-Policy**: Restricts `connect-src` to Supabase and FX API.
- **X-Content-Type-Options**: `nosniff`.
- **Referrer-Policy**: `strict-origin-when-cross-origin`.
- **X-Frame-Options**: `DENY`.

## 4. DPDP Compliance

- **Consent Gate**: Blocks access to the app until the user agrees to the privacy policy. Tracked per device (Android: DataStore, Web: localStorage).
- **Delete My Data**: Soft deletes all rows for the user.
- **Delete My Account**: Executes `delete_my_account()` Supabase RPC which hard-deletes all data and the Auth user.

## 5. Rate Limiting

- **V1**: Relies on Supabase free tier limits (1000 req/sec) and Vercel Edge Firewall.
- **V2**: Cloudflare integration for strict rate-limiting rules if abuse occurs.
