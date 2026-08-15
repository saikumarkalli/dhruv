# Web App SDD (04)

> **Status:** ACTIVE
> **Scope:** Technical design for the Vite + React SPA web application.

## 1. Technology Stack

- **Core**: React 19, TypeScript 5.x, Vite 6
- **Routing**: React Router v7
- **State Management**: React Query (Server state), Context + useReducer (UI state)
- **API/Auth**: `@supabase/supabase-js`
- **Styling**: Vanilla CSS with CSS Custom Properties (porting Dhruv design tokens)
- **PWA**: `vite-plugin-pwa`
- **i18n**: `react-intl`

## 2. Project Structure

```
web/
├── src/
│   ├── apps/
│   │   ├── finance/        # V1: Tracker and calculators
│   │   ├── tools/          # Scaffolded for V2
│   │   ├── vault/          # Scaffolded for V3
│   │   ├── health/         # Scaffolded
│   │   └── relationship/   # Scaffolded
│   ├── shared/
│   │   ├── components/     # Design system (FeatureHost, BentoCard, etc.)
│   │   ├── hooks/          # useAuth, useFeatureFlag, useSupabase
│   │   ├── i18n/           # en.json
│   │   ├── lib/            # paise utils, validation logic
│   │   ├── styles/         # tokens.css, globals.css
│   │   └── types/          # database.ts (Supabase generated)
│   ├── main.tsx
│   └── router.tsx          # Lazy-loaded app routes
```

## 3. Auth & State Machine

Uses the PKCE OAuth flow for SPA security.

**Auth States**:
- `not-configured`: Checking session / initializing.
- `consent-needed`: Valid session, but DPDP consent missing.
- `signed-out`: No session found.
- `loading`: In-flight auth request.
- `signed-in`: Session valid and consent granted.

## 4. Feature Isolation (Error Boundaries)

Mimicking Android's `FeatureHost`, the web app wraps modules in an `<ErrorBoundary>` and checks the feature flag via `<FeatureFlagProvider>`.

```tsx
<FeatureHost featureKey="networth">
   <Dashboard />
</FeatureHost>
```

## 5. Styling & Design System

- **Zero dependencies**: No Tailwind, no CSS modules.
- **Tokens**: `tokens.css` defines `--color-navy`, `--spacing-4`, etc.
- **Dark/Light Mode**: Toggled via `data-theme="dark"` on the HTML root, swapping CSS variable definitions.

## 6. PWA & Notifications

- **PWA**: Offline shell and installability via `vite-plugin-pwa`. Tracker data requires network.
- **Notifications (V2)**: Handled via Web Push API. Push logic resides in Supabase Edge Functions. V1 relies on manual refresh.
