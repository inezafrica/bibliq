# BibliQ — Visual Style Guide

How BibliQ looks: style, colors, fonts and UI rules. Read this together with `mascot-oli.md`.

---

## 1. Style direction: "Warm Illuminated Storybook"

A bright, friendly mobile game look, with a few gold touches that recall old illuminated Bibles.

- **Joyful and modern** for kids and teens.
- **Respectful and warm** for parents, pastors and church groups.

### Key principles

| Principle | What it means |
|---|---|
| **Flat 2D, soft shading** | Same style as Oli. No realistic textures or 3D renders. |
| **Rounded everywhere** | Cards and buttons have 16–24 dp corners. No sharp edges. |
| **Chunky, pressable buttons** | A darker bottom edge (4 dp) that "presses down" on tap. The game should feel physical and fun. |
| **Parchment cards** | Questions sit on a warm cream card, like an old page, inside a colorful game world. |
| **Gold is precious** | Gold is used **only for rewards**: stars, Talents, chests, boss frames. Keeping it rare keeps it special. |
| **Each world has its own colors** | The map changes tint per world, so progress is visible. |

### Avoid

- ❌ Dark, gothic or somber religious art
- ❌ Realistic paintings in the interface (public-domain art is fine *inside* image questions)
- ❌ Neon colors and heavy gradients
- ❌ Script or blackletter "Bible-style" fonts for game text

---

## 2. Color palette

### Core colors

| Role | Name | Hex | Used for |
|---|---|---|---|
| Primary | Royal Blue | `#2B4C9B` | Headers, main buttons, map path |
| Background | Sky | `#8EC5FF` | Backgrounds, menus |
| Reward | Gold | `#F5B82E` | Stars, Talents, chests, boss frames |
| Correct | Olive Green | `#4E9A2E` | Correct answer, Oli's branch, "Next" button |
| Wrong | Soft Coral | `#F2665A` | Wrong answer, lost heart |
| Surface | Parchment | `#FFF6E5` | Question cards, popups |
| Text | Ink | `#2D2A32` | All main text |
| Base | White | `#FFFFFF` | Oli, text on dark buttons |

### Button edge colors (pressed / bottom shadow)

| Button | Face | Bottom edge |
|---|---|---|
| Primary | `#2B4C9B` | `#1E3670` |
| Correct | `#4E9A2E` | `#3D7F22` |
| Wrong | `#F2665A` | `#D9483D` |
| Reward | `#F5B82E` | `#D99A12` |

### Why coral instead of red?

Coral still clearly says "wrong", but it feels gentler. It matches Oli's kind, encouraging tone: a mistake is part of the journey, not a punishment.

### Contrast rules (readability)

Measured contrast ratios (WCAG):

| Text on background | Ratio | Rule |
|---|---|---|
| White on Royal Blue | 8.0 : 1 | ✅ Any text size |
| Ink on Parchment | 13.1 : 1 | ✅ Any text size |
| Ink on Gold | 7.9 : 1 | ✅ Any text size |
| Ink on Sky | 7.8 : 1 | ✅ Any text size |
| Ink on Coral | 4.6 : 1 | ✅ Any text size, so **use Ink text on coral** |
| White on Olive Green | 3.5 : 1 | ⚠️ Only **bold, 18 sp or larger** |
| White on Coral | 3.1 : 1 | ❌ Avoid |
| Gold on White | 1.8 : 1 | ❌ Never for text, icons only |

**Simple rule:** gold and light blue are for icons and backgrounds, never for text on white.

---

## 3. World colors (level map)

| World | Main tint | Hex |
|---|---|---|
| 1. Creation & the Garden | Fresh green | `#7CC46A` |
| 2. The Patriarchs | Sunset orange | `#F29E4C` |
| 3. Exodus & the Desert | Sand + Red Sea blue | `#E8C27A` |
| 4. The Promised Land | Hill green | `#9BBF5A` |
| 5. Kings & Kingdoms | Royal purple + gold | `#7A4FA3` |
| 6. Poets & Prophets | Night blue | `#2E3A6E` |
| 7. The Gospels | Lake turquoise | `#3FB6B2` |
| 8. The Early Church | Mediterranean blue | `#3A7BD5` |
| 9. Revelation | Cloud white + gold glow | `#FFFFFF` + `#F5B82E` |

The world tint is used for map backgrounds, the world banner and level nodes. The core colors (buttons, answers, rewards) **never change** between worlds, so players always know what green and coral mean.

---

## 4. Fonts

All three are **free Google Fonts under the SIL Open Font License**: they can be bundled in the app for commercial use, and they fully support French and Spanish characters (é, è, ç, à, ñ, ¿, ¡).

| Use | Font | Weights | Why |
|---|---|---|---|
| Titles, level numbers, buttons, scores | **Baloo 2** | Bold, ExtraBold | Round, playful, game-like; matches Oli |
| Questions, answers, menus | **Nunito** | Regular, Bold | Very readable on small screens, soft and friendly |
| Verse references and quotes | **Lora** | Italic | Classic serif with a "scripture" feel |

### Type scale

| Element | Font | Size | Weight |
|---|---|---|---|
| Screen title | Baloo 2 | 28 sp | ExtraBold |
| Level number (map) | Baloo 2 | 22 sp | Bold |
| Score / big numbers | Baloo 2 | 32 sp | ExtraBold |
| Question | Nunito | 20–22 sp | Bold |
| Answer option | Nunito | 18 sp | Regular |
| Button label | Baloo 2 | 18 sp | Bold |
| Body / menus | Nunito | 16 sp | Regular |
| Verse reference | Lora | 16 sp | Italic |
| Small labels | Nunito | 13 sp | Bold |

### Language note

French and Spanish texts are often **20–30% longer** than English. Design every button and card for the longest language, and test all screens in French and Spanish, not only English.

---

## 5. UI components

### Question card
- Parchment background `#FFF6E5`, 24 dp corners, soft shadow.
- Question text in Nunito Bold, Ink.
- Image questions: image on top, with 16 dp corners.

### Answer buttons
- Default: white face, Ink text, light grey bottom edge.
- Correct: Olive Green face, white **bold** text, short bounce.
- Wrong: Coral face, **Ink** text, short shake.
- Minimum height **56 dp**, full width, 12 dp spacing.

### Verse reference card
- Small parchment chip with 📖 icon, Lora Italic: *Genesis 6:14*.

### Stars
- Gold `#F5B82E` with a white highlight and a darker gold outline.
- Empty stars: light grey outline.

### Hearts and lives
- Coral hearts, white highlight.
- Lost heart: grey, with a small "break" animation.

---

## 6. Spacing, shapes and motion

| Token | Value |
|---|---|
| Base spacing unit | 8 dp (use 8 / 16 / 24 / 32) |
| Card corners | 24 dp |
| Button corners | 16 dp |
| Button bottom edge | 4 dp |
| Minimum touch target | 48 dp |
| Short animations | 150–250 ms |
| Celebrations | 600–1000 ms |

Motion style: **bouncy and playful** (overshoot easing), never slow. A reaction should never delay the next question.

---

## 7. Developer tokens (Jetpack Compose)

```kotlin
object BibliQColors {
    val RoyalBlue     = Color(0xFF2B4C9B)
    val RoyalBlueDark = Color(0xFF1E3670)
    val Sky           = Color(0xFF8EC5FF)
    val Gold          = Color(0xFFF5B82E)
    val GoldDark      = Color(0xFFD99A12)
    val OliveGreen    = Color(0xFF4E9A2E)
    val OliveDark     = Color(0xFF3D7F22)
    val Coral         = Color(0xFFF2665A)
    val CoralDark     = Color(0xFFD9483D)
    val Parchment     = Color(0xFFFFF6E5)
    val Ink           = Color(0xFF2D2A32)
    val White         = Color(0xFFFFFFFF)
}

val Baloo2 = FontFamily(
    Font(R.font.baloo2_bold, FontWeight.Bold),
    Font(R.font.baloo2_extrabold, FontWeight.ExtraBold)
)
val Nunito = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_bold, FontWeight.Bold)
)
val Lora = FontFamily(
    Font(R.font.lora_italic, FontWeight.Normal, FontStyle.Italic)
)
```

Bundle the font files in `res/font/` rather than downloading them at runtime, so the app looks right offline from the first launch.

---

## 8. Checklist before shipping a screen

- [ ] Uses only palette colors
- [ ] Gold used only for rewards
- [ ] Text contrast follows the rules in section 2
- [ ] Tested in English, French and Spanish (longest text fits)
- [ ] Tested on a small, low-end phone
- [ ] Touch targets at least 48 dp
- [ ] Animations are short and never block the next question
