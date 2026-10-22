# BibliQ — Asset Pack v1

All assets from `assets-list.md`, ready to drop into the Android project.
Start with `previews/00_home_mock.png`, which shows the assets working together on the home screen.

---

## Folder structure

```
bibliq-assets/
├── android/res/                 ← copy into app/src/main/res/
│   ├── drawable/                  Vector Drawables (.xml): icons, power-ups, nodes, badges, launcher layers
│   ├── drawable-nodpi/            WebP images: decorations, World 1 backgrounds, oli_idle
│   ├── mipmap-anydpi-v26/         Adaptive launcher icon (Android 8+, with themed icon)
│   └── mipmap-mdpi … xxxhdpi/     Launcher PNGs for Android 7 (minSdk 24)
├── oli/                         ← Oli master file for animation (Lottie)
├── play-store/                  ← 512 × 512 Play Store icon
├── source-svg/                  ← SVG of every asset (open in Illustrator to edit)
├── previews/                    ← contact sheets for review
└── tools/                       ← Python scripts that generated everything
```

---

## 1. Icons → `drawable/`

| File | Notes |
|---|---|
| `ic_lamp_lit.xml`, `ic_lamp_out.xml` | Lives. Same position in both, so they can be swapped in place |
| `ic_talent.xml` | Currency coin with olive sprig |
| `ic_star_filled.xml`, `ic_star_empty.xml` | Level stars |
| `ic_seal.xml`, `ic_seal_broken.xml` | Locked / unlock animation |
| `ic_settings.xml` | **White**, for the blue top bar |
| `ic_nav_map.xml`, `ic_nav_endless.xml`, `ic_nav_badges.xml`, `ic_nav_profile.xml` | **Single colour (Ink), tint in code**: blue when active, grey when inactive |

Tinting nav icons in Compose:

```kotlin
Icon(
    painter = painterResource(R.drawable.ic_nav_map),
    contentDescription = stringResource(R.string.nav_map),
    tint = if (selected) BibliQColors.RoyalBlue else Color(0xFF9A96A0)
)
```

Use `Image(...)` (not `Icon`) for the coloured icons, so their colours are kept.

## 2. Power-ups → `drawable/`

`ic_power_staff.xml` (50/50) · `ic_power_sundial.xml` (extra time) · `ic_power_chariot.xml` (skip) · `ic_power_scroll.xml` (reveal verse). Default size 40 dp.

## 3. Level nodes → `drawable/`

| File | Default size | Notes |
|---|---|---|
| `node_stone_done.xml` | 64 dp | Blank face; draw the level number on top in code |
| `node_stone_current.xml` | 64 dp | Blue + gold ring; add the pulse/glow in code |
| `node_scroll_locked.xml` | 64 dp | Sealed scroll |
| `node_stone_boss.xml` | 80 dp | Gold ring, laurels, star |
| `path_footprint.xml` | 16 dp | **Left** sandal print pointing up. Mirror it for the right foot and rotate it along the path in code |

## 4. Badges → `drawable/`

- Ranks (blue medallion): `badge_rank_1_sandals` … `badge_rank_6_crown`
- Achievements (cream medallion + ribbon): `badge_ark`, `badge_sling`, `badge_trumpet`, `badge_manna`

Default size 48 dp.

## 5. Decorations → `drawable-nodpi/` (WebP, transparent)

Sized in pixels for a **1080 px wide** map:

| File | Size (px) |
|---|---|
| `ribbon_world.webp` | 900 × 230 (**empty**: world name drawn in code) |
| `sign_wood.webp` | 300 × 360 (**empty**) |
| `gate_walls.webp` | 1080 × 560 (full width, end of map) |
| `chest_treasure_closed.webp`, `chest_treasure_open.webp` | 340 × 300 |
| `deco_tent.webp` | 400 × 300 |
| `deco_palm.webp` | 400 × 500 |
| `deco_rock.webp` | 260 × 180 |
| `deco_cloud_1/2/3.webp` | 380 × 170, 300 × 140, 440 × 190 |

## 6. World 1 backgrounds → `drawable-nodpi/`

Four 1080 × 1920 segments, each under 75 KB. They were painted as **one tall image and sliced**, so the seams match exactly.

Stacking order on the map (bottom → top):

```
bg_transition_1_2.webp   ← top: garden turns into desert
bg_world1_03.webp
bg_world1_02.webp
bg_world1_01.webp        ← bottom: start of the game
```

- The centre column (about x = 300–780 px) is kept calm, so the footprint path and level nodes stay readable.
- The Eden stream runs along the left edge and leaves the screen inside the transition segment.
- **World 2 must start with sand `#EDC688`** at its bottom edge, to continue the top of `bg_transition_1_2`.
- The full strip is in `source-svg/bg_world1_full_strip.svg`.

## 7. Oli → `oli/` and `drawable-nodpi/oli_idle.webp`

`oli/oli_master.svg` is a 512 × 512 file with **one `<g>` group per animation layer**, named as in `mascot-oli.md`:

| Layer | Visible at start | Pivot (anchor point) |
|---|---|---|
| `shadow`, `feet` | ✔ | — |
| `body` | ✔ | 256, 452 (feet) |
| `head` (includes cheeks + head tuft) | ✔ | 262, 300 (neck) |
| `wing_left`, `wing_right` | ✔ | 170, 278 / 342, 278 (shoulders) |
| `eyes_open` | ✔ | — |
| `eyes_happy` | hidden | — |
| `eyes_soft` (kind, smiling eyes for the wrong-answer reaction) | hidden | — |
| `brows_soft` | hidden | — |
| `branch` | ✔ | 262, 252 (beak) |
| `beak_closed` | ✔ | — |
| `beak_open` | hidden | — |
| `smile` | hidden | — |
| `sparkle_1`, `sparkle_2`, `sparkle_3` | hidden | — |

Hidden layers use `display="none"`. If your animation tool doesn't respect that, use the ready-made poses instead: `oli_pose_idle.svg`, `oli_pose_correct.svg` and `oli_pose_wrong.svg`, each with PNG previews.

When you animate in LottieFiles or After Effects, set each layer's anchor to the pivot above, so wings rotate from the shoulder and the head tilts from the neck.

I left out the tail: from the front view it looked like extra feet. It can come back as a small layer if a side pose needs it.

## 8. App icon

| File | Use |
|---|---|
| `mipmap-anydpi-v26/ic_launcher.xml`, `ic_launcher_round.xml` | Adaptive icon (background + foreground + themed monochrome) |
| `drawable/ic_launcher_background.xml` | Royal blue with light rays |
| `drawable/ic_launcher_foreground.xml` | Oli, inside the safe zone |
| `drawable/ic_launcher_monochrome.xml` | Android 13+ themed icon |
| `mipmap-*/ic_launcher.png`, `ic_launcher_round.png` | Android 7 (API 24–25) fallback |
| `play-store/ic_play_store_512.png` | Play Console upload |

In `AndroidManifest.xml`:

```xml
android:icon="@mipmap/ic_launcher"
android:roundIcon="@mipmap/ic_launcher_round"
```

---

## Notes

- **Colours** come from `style-guide.md`. Change them in one place, `tools/lib.py` (palette at the top), and re-run the scripts.
- **Regenerating:** `pip install cairosvg pillow`, set `ROOT` in `tools/lib.py`, then run `icons.py`, `nodes_badges.py`, `deco.py`, `bg.py`, `oli.py`, `mock.py`.
- **Not included:** the Lottie animations (`oli_correct.lottie` etc.), which are made from `oli_master.svg` in your animation tool, and Worlds 2–9 backgrounds.
- **Ownership:** these drawings were generated for BibliQ as a starting point. Polish them in Illustrator, especially Oli, so the final character is clearly your team's own work before you register the brand.
