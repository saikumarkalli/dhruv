# Third-party fonts

Dhruv bundles nine font binaries inside `:libs:core`. All three families are licensed under the
**SIL Open Font License, Version 1.1** (OFL 1.1), which requires the license text to accompany the
fonts whenever the Font Software is redistributed. Publishing this repository is redistribution, so
this directory carries that text: one `OFL.txt` per family, copied verbatim from each family's own
upstream repository.

This directory covers **only what this repository actually redistributes** — the checked-in `.ttf`
binaries. Gradle and npm dependencies are resolved at build time and are not shipped from here, so
they are deliberately not listed.

The design contract that specifies these three families is
[`platform/DESIGN-SYSTEM.md`](../../platform/DESIGN-SYSTEM.md) §2; the Compose font families are
declared in
[`libs/core/src/main/kotlin/com/dhruv/core/ui/theme/DhruvFont.kt`](../../libs/core/src/main/kotlin/com/dhruv/core/ui/theme/DhruvFont.kt).

---

## Provenance

License text was retrieved on 2026-08-18 from each family's canonical upstream repository — not from
a mirror, and not from a shared OFL template. The commit named below is the most recent commit
touching that license file at the time of retrieval.

| Family | Upstream project | License file sourced from | At commit | Local copy |
|---|---|---|---|---|
| Inter | [github.com/rsms/inter](https://github.com/rsms/inter) | [`LICENSE.txt`](https://github.com/rsms/inter/blob/master/LICENSE.txt) | `3ac1bd32a473ea60d40d8f444820247e96dd7e70` (2023-04-08) | [`inter/OFL.txt`](inter/OFL.txt) |
| Space Grotesk | [github.com/floriankarsten/space-grotesk](https://github.com/floriankarsten/space-grotesk) | [`OFL.txt`](https://github.com/floriankarsten/space-grotesk/blob/master/OFL.txt) | `4a44bc96691ab8f0fd06e3da65224a2ab30afe23` (2020-05-14) | [`space-grotesk/OFL.txt`](space-grotesk/OFL.txt) |
| JetBrains Mono | [github.com/JetBrains/JetBrainsMono](https://github.com/JetBrains/JetBrainsMono) | [`OFL.txt`](https://github.com/JetBrains/JetBrainsMono/blob/master/OFL.txt) | `61cf0cedc2d9d29efcab968e97707d6899133e68` (2024-08-08) | [`jetbrains-mono/OFL.txt`](jetbrains-mono/OFL.txt) |

Each file is the complete OFL 1.1 text — preamble, definitions, permission and conditions,
termination and disclaimer — reproduced byte-for-byte, with its own upstream copyright header intact.

---

## Copyright lines

Each family's own copyright notice, exactly as it appears on the first line of its `OFL.txt`:

| Family | Copyright line | Reserved Font Name |
|---|---|---|
| Inter | `Copyright (c) 2016 The Inter Project Authors (https://github.com/rsms/inter)` | none declared |
| Space Grotesk | `Copyright 2020 The Space Grotesk Project Authors (https://github.com/floriankarsten/space-grotesk)` | none declared |
| JetBrains Mono | `Copyright 2020 The JetBrains Mono Project Authors (https://github.com/JetBrains/JetBrainsMono)` | none declared |

**No Reserved Font Name is declared by any of the three families.** OFL 1.1 §DEFINITIONS explains
what a Reserved Font Name is, and every copy here contains that explanatory sentence, but none of
the three copyright lines carries a `with Reserved Font Name "..."` clause. In practice this means
OFL clause 3 imposes no naming restriction on a modified version of these fonts. Dhruv does not
modify them, so the point is informational rather than operative — it is recorded here so a future
reader does not have to re-derive it.

Note on Inter: the copy in the Google Fonts repository (`ofl/inter/OFL.txt`) reads
`Copyright 2020 The Inter Project Authors`, while the author's own repository reads
`Copyright (c) 2016`. The upstream author's text is the one reproduced here.

---

## Bundled binaries

All nine files live in one directory,
[`libs/core/src/main/res/font/`](../../libs/core/src/main/res/font/).

| Family | Files |
|---|---|
| Inter | [`inter_regular.ttf`](../../libs/core/src/main/res/font/inter_regular.ttf) · [`inter_medium.ttf`](../../libs/core/src/main/res/font/inter_medium.ttf) · [`inter_semibold.ttf`](../../libs/core/src/main/res/font/inter_semibold.ttf) |
| Space Grotesk | [`space_grotesk_regular.ttf`](../../libs/core/src/main/res/font/space_grotesk_regular.ttf) · [`space_grotesk_medium.ttf`](../../libs/core/src/main/res/font/space_grotesk_medium.ttf) · [`space_grotesk_semibold.ttf`](../../libs/core/src/main/res/font/space_grotesk_semibold.ttf) · [`space_grotesk_bold.ttf`](../../libs/core/src/main/res/font/space_grotesk_bold.ttf) |
| JetBrains Mono | [`jetbrains_mono_regular.ttf`](../../libs/core/src/main/res/font/jetbrains_mono_regular.ttf) · [`jetbrains_mono_medium.ttf`](../../libs/core/src/main/res/font/jetbrains_mono_medium.ttf) |

The bundled `.ttf` binaries were **not** byte-verified against a specific upstream release — the
license text above was sourced from each project's canonical repository, but no checksum comparison
was performed between the checked-in files and any published upstream artifact, so their exact
upstream version is assumed rather than proven.

---

## Rules when changing fonts

- Adding a weight of an existing family needs no change here — the family's `OFL.txt` already
  covers it.
- Adding a **new family** means adding its own `third_party/fonts/<family>/OFL.txt` (or whatever
  license the family actually carries), a row in each table above, and a line in the root
  [`NOTICE`](../../NOTICE) — in the same change that adds the binary, not afterwards.
- Never edit the text inside an `OFL.txt`. It is a license notice; reflowing or trimming it breaks
  the attribution requirement it exists to satisfy. Replace it wholesale from upstream instead.
- Removing a family's last binary means removing its directory and its rows here and in `NOTICE`.

The project's own source is licensed separately — see [`LICENSE`](../../LICENSE) at the repository
root. The OFL applies to the font binaries only and does not extend to the rest of the repository.