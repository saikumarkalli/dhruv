# Dhruv Brand Assets

Official brand palette and assets for all Dhruv apps.

## Color Palette
| Token            | Hex       | Use                                    |
|------------------|-----------|----------------------------------------|
| DhruvNavy        | `#0D1B2A` | Primary background, icon bg, badges    |
| DhruvNavyElevated| `#132B4D` | Elevated navy surface                  |
| DhruvBlue        | `#1E3A6D` | Mid navy / accents                     |
| DhruvSilver      | `#C0C6D1` | Wordmark / text on dark bg             |
| DhruvSilverLight | `#E6E9EF` | Compass star highlight                 |
| DhruvSteel       | `#8E97A6` | Orbital rings                          |
| DhruvAccent      | `#3FA7FF` | Accent blue (CTAs, selected states)    |

## Typography
- **Wordmark**: Tont Serif (fallback: Cormorant Garamond via Google Fonts)
- **UI Body**: Roboto / system sans-serif

## Logo Variants

| Variant               | File                              | Use                                    |
|-----------------------|-----------------------------------|----------------------------------------|
| Full-color crest      | `svg/ic_dhruv_crest.svg`          | Dark-theme screens, cards              |
| Navy crest            | `svg/ic_dhruv_crest_navy.svg`     | Light-theme, export, print             |
| White silhouette      | `svg/ic_dhruv_crest_white.svg`    | Notification icons                     |
| Horizontal wordmark   | `svg/ic_dhruv_wordmark_h.svg`     | Splash, onboarding header, about page  |
| Vertical stack        | `svg/ic_dhruv_wordmark_v.svg`     | Settings header, empty states          |

## Master Art
High-resolution source art from the Master Brand Kit lives in `master/`:
- `master/dhruv_icon_master.png` — the compass-star icon mark (square)
- `master/dhruv_wordmark_master.png` — the "dhruv" wordmark
- `master/dhruv_logo_horizontal.png` — crest + wordmark lockup

These are the canonical raster sources. The Android launcher PNGs below are generated from
`master/dhruv_icon_master.png` (1024² master), trimmed to content and composited on the
`#0D1B2A` (DhruvNavy) app-icon background.

## App Icon Sizes (Android)
mdpi:    48×48 px   → `png/ic_launcher_mdpi.png`
hdpi:    72×72 px   → `png/ic_launcher_hdpi.png`
xhdpi:   96×96 px   → `png/ic_launcher_xhdpi.png`
xxhdpi:  144×144 px → `png/ic_launcher_xxhdpi.png`
xxxhdpi: 192×192 px → `png/ic_launcher_xxxhdpi.png`
Play Store: 512×512 px → `png/ic_launcher_play.png`

The finance app ships these as an **adaptive icon** (`mipmap-anydpi-v26/ic_launcher.xml`):
foreground = the compass art (≈0.66 of the 108dp canvas, mask-safe), background = `#0D1B2A`
(DhruvNavy), monochrome = `ic_dhruv_crest` (themed icons). The `png/` files here are the
legacy/fallback raster (compass on navy) + the 512² Play listing icon.

## What NOT to Do
- ❌ Stretch or skew the crest
- ❌ Use non-brand colors on logo elements
- ❌ Add glow, drop shadow, or filters to the crest
- ❌ Place the wordmark in sans-serif or ALL CAPS
- ❌ Put the crest on a busy background without a solid Navy container

## Source Files
Drop AI / EPS / SVG source files into `svg/` when available.
PNG rasterized exports go into `png/`.
