# Dhruv App — UI/UX Design System

This document outlines the design system specifications extracted from the Android Jetpack Compose codebase. It serves as a reference for creating accurate UI mocks in design tools (e.g., Sketch, Figma, Snitch).

## 1. Brand Colors
These core colors define the Dhruv brand identity (Navy + Silver).

| Color Name | Hex Code | Usage |
| :--- | :--- | :--- |
| **Navy (Primary)** | `#0D1B2A` | Primary background / icon background |
| **Elevated Navy** | `#132B4D` | Elevated navy surface |
| **Blue (Mid)** | `#1E3A6D` | Mid navy / accents |
| **Silver (Primary)** | `#C0C6D1` | Primary silver |
| **Silver Light** | `#E6E9EF` | Bright silver highlight |
| **Steel (Muted)** | `#8E97A6` | Muted steel (orbital rings) |
| **Accent Blue** | `#3FA7FF` | Accent blue |
| **Logo Background** | `#F4F6FA` | Light tint behind full-color logo / app icon |

---

## 2. Theme Palettes (Material 3)

### Light Theme
Inspired by a clean, MIUI-like aesthetic.

- **Primary:** `#F05A28` *(MIUI Orange)*
- **Secondary:** `#455A64` *(Slate Blue)*
- **Tertiary:** `#00B0FF` *(Vivid Cyan)*
- **Background:** `#F9F9F9` *(Clean white/grey)*
- **Surface:** `#FFFFFF`
- **Surface Variant:** `#E5E7EB`
- **On Primary / Secondary / Tertiary:** `#FFFFFF` *(White)*
- **On Background / Surface:** `#111827` *(Dark Gray)*
- **On Surface Variant:** `#374151` *(Medium Gray)*

### Sophisticated Dark Theme
- **Primary:** `#FFFF6D3B` *(Bright MIUI Orange)*
- **Secondary:** `#CFD8DC` *(Muted Soft Gray)*
- **Tertiary:** `#80D8FF` *(Sky Blue)*
- **Background:** `#0A0A0A` *(Pitch Black)*
- **Surface:** `#1E1E1E` *(Slate neutral dark)*
- **Surface Variant:** `#2C2C2C`
- **On Primary:** `#003258`
- **On Secondary:** `#161C24`
- **On Tertiary:** `#211047`
- **On Background / Surface:** `#F5F5F5` *(Warm White text)*
- **On Surface Variant:** `#9E9E9E` *(Subtitle text)*

### Calculator Keypad Specific Colors
- **Numeric Keys:** `#FFFFFF` (Light) / `#1E1E1E` (Dark), Text: `#212121` (Light)
- **Modifier Keys:** `#EFEFEF` (Light) / `#2C2C2C` (Dark), Text: `#455A64` (Light)
- **Operator Keys:** `#F05A28` (Background), `#FFFFFF` (Text)
- **Soft Operator (Light):** `#FFF1ED` (Background), `#F05A28` (Text)
- **Equals Key:** `#F05A28` (Background), `#FFFFFF` (Text)
- **Science Keys:** `#F5F5F5` (Background), `#455A64` (Text), `#E0E0E0` (Border), `#CFD8DC` (Active Bg)

---

## 3. Typography

- **Primary Font Family:** System Default (Roboto/Inter). Optional overrides for Monospace and SansSerif (Rounded).
- **Brand Font (Wordmark):** Serif. Used at `28sp` with `0.5sp` letter spacing.
- **Base Body Text (`bodyLarge`):** `16sp`, `24sp` line height, `0.5sp` letter spacing, Regular weight.

---

## 4. UI Components & Effects

### Background Gradients
The app utilizes a subtle linear gradient background to give depth.
- **Light Mode Gradient:** `Primary (5% opacity)` → `Background` → `Background`
- **Dark Mode Gradient:** `Primary (12% opacity)` → `Background (98% opacity)` → `Background (100% opacity)`

### Glassmorphism Cards
Used for distinct UI elements requiring depth and translucency.
- **Background:** `Surface` color at **12% opacity** (`0.12 alpha`).
- **Corner Radius:** `16dp`.
- **Blur Radius:** `20px` (background blur).
- **Border (Shimmer):** White (`#FFFFFF`) at **20% opacity** (`0.20 alpha`), `1dp` width.
- **Content Padding:** `16dp`.

### Brand Assets & Lockups
- **Wordmark Aspect Ratio:** `944 : 256` (Width : Height).
- **Horizontal Lockup (App Bar):** Logo (`32dp`) + Text.
- **Vertical Hero Lockup (Splash/Settings):** Logo (`96dp`) + Text below (spaced by `12dp`).
- **Crest / Notification:** Silhouette crest, tinted `Silver Light` (`#E6E9EF`) on dark/navy surfaces.
