# BibliQ — Assets to Draw

The full list of drawings for BibliQ, with file names and formats. Read together with `style-guide.md` and `mascot-oli.md`.

---

## Golden rule: no text inside drawings

The app shows every word and number in **English, French and Spanish**. Ribbons, signs, buttons and level nodes are drawn **empty**, and the app adds the text on top.

## Naming rule

Android file names must be **lowercase with underscores only**: `ic_lamp_lit.xml`, not `Ic-Lamp-Lit.xml`.

## Formats at a glance

| Type of drawing | Format | Why |
|---|---|---|
| Icons, nodes, badges (simple shapes) | **SVG → Android Vector Drawable (.xml)** | Sharp at any size, tiny files. Import with Android Studio: *New → Vector Asset* |
| Decorations (detailed, transparent) | **WebP with transparency** | Detailed drawings stay light |
| Backgrounds (full screen) | **WebP** | Too detailed for vectors |
| Oli animations | **Lottie (.lottie)** | Animated, transparent, tiny |

---

## 1. Icons → SVG → Vector Drawable

### Map-screen icons

| Drawing | File name | States / notes | What it means in the game |
|---|---|---|---|
| Oil lamp (lives) | `ic_lamp_lit.xml`, `ic_lamp_out.xml` | Lit with flame / out with a small smoke curl | **Lives.** The player has 5. Failing a level puts one lamp out, and one comes back every 30 minutes (the countdown shows next to the lamp). With 0 lives the lamp is out and the player can't start a level until one regenerates. The unlit lamp also appears on the result screen as "You lost a life". Inspired by the wise virgins keeping their lamps lit (Matthew 25:1–13). |
| Talent coin (currency) | `ic_talent.xml` | Ancient silver-gold coin | **Talents, the in-game money.** Players start with 240 and earn 10 for every new star on a level (replaying for the same stars earns nothing). They're spent on power-ups, and later on refilling lives. Named after the parable of the talents (Matthew 25:14–30). |
| Star | `ic_star_filled.xml`, `ic_star_empty.xml` | Gold / grey outline | **Stars, the level score.** 7/10 correct = 1 star (pass), 9/10 = 2 stars, 10/10 = 3 stars. Gold = earned, grey = not yet. Shown under each finished level on the map and on the result screen. The best result is kept, and at least 1 star unlocks the next level. |
| Wax seal (locked) | `ic_seal.xml`, `ic_seal_broken.xml` | Red-brown seal + broken version for the unlock animation | **Unlock animation.** The seal on a locked level breaks when it opens. *Drawn but not used in the app yet.* |
| Settings gear | `ic_settings.xml` | Standard, white | **Settings** (top right of the map). *The settings screen isn't built yet.* |

### Bottom navigation

| Drawing | File name | States / notes | What it means in the game |
|---|---|---|---|
| Map | `ic_nav_map.xml` | Active (blue) / inactive (grey) | **The level map.** The main screen, with the path of levels. |
| Endless | `ic_nav_endless.xml` | Active / inactive | **Endless mode.** Keep answering questions with no level end. *Not built yet.* |
| Badges | `ic_nav_badges.xml` | Active / inactive | **Badges collection** (the achievement badges in section 4). *Not built yet.* |
| Profile | `ic_nav_profile.xml` | Active / inactive | **Player profile**: rank badge, stars and stats. *Not built yet.* |

### Question-screen icons

| Drawing | File name | What it means in the game | Bible link |
|---|---|---|---|
| Olive leaf | `ic_leaf_chance.xml` | **One chance inside a level.** Every level starts with 3 leaves at the top of the question screen. They replace the usual hearts. | The dove's olive leaf (Genesis 8:11) |
| Dry olive leaf | `ic_leaf_lost.xml` | **A lost chance.** A wrong answer, or running out of time, turns one green leaf into a dry one. Losing all 3 ends the level as failed. | — |
| Small sundial | `ic_timer_sundial.xml` | **The question timer.** 20 seconds per question. Answering faster gives bonus points. | The sundial of Ahaz (2 Kings 20:9–11) |
| Open scroll | `ic_verse_scroll.xml` | **The verse reference.** Shown after each answer (e.g. "Genesis 6:14"), so the game teaches as well as tests. | — |

**Chances vs lives:** 🌿 olive leaves are the 3 chances inside one level; 🪔 oil lamps are the 5 lives on the map. Losing all 3 leaves fails the level and costs 1 lamp.

## 2. Power-ups → SVG → Vector Drawable

The 4 buttons at the bottom of the question screen. The number on each is its price in Talents. Each one can be used **once per question**, and only if the player has enough Talents. Prices are set in `QuizEngine.kt` (`PowerUpType`).

| Drawing | Power-up | Price | File name | What it does | Bible story |
|---|---|---|---|---|---|
| Moses' staff | 50/50 | **30** | `ic_power_staff.xml` | Removes two wrong answers, leaving 2 to choose from. Not available on True/False questions, which only have 2 answers. | Parting the Red Sea (Exodus 14:16) |
| Sundial | Extra time | **20** | `ic_power_sundial.xml` | Adds 10 seconds to the timer. | The shadow went back (2 Kings 20:9–11) |
| Elijah's chariot | Skip | **50** | `ic_power_chariot.xml` | Skips the question and counts it as correct, without the speed bonus. The most expensive because it's a guaranteed point. | Taken up to heaven (2 Kings 2:11) |
| Scroll | Reveal the verse | **25** | `ic_power_scroll.xml` | Shows the verse reference before answering, as a hint. | — |

## 3. Level nodes → SVG → Vector Drawable

The stops along the path on the map, one per level.

| Drawing | File name | Notes | What it means in the game |
|---|---|---|---|
| Memorial stone (completed) | `node_stone_done.xml` | Smooth stone, blank face; number added in code | **A finished level** (at least 1 star). Its stars show underneath. Tap it to replay for more stars. Inspired by the memorial stones of Joshua 4. |
| Memorial stone (current) | `node_stone_current.xml` | Brighter, blue ring; glow and pulse in code | **The next level to play.** The **Play** button starts this one. |
| Sealed scroll (locked) | `node_scroll_locked.xml` | Rolled scroll with wax seal | **A locked level.** Opens once the previous level has at least 1 star. Tapping it shows "Finish level X first". |
| Boss stone | `node_stone_boss.xml` | Bigger, golden frame | **A boss level** at the end of a world. Longer (15 questions) and harder, with a special reward. |
| Sandal footprint (path) | `path_footprint.xml` | One footprint, repeated along the path in code | **The path** connecting the levels, showing the journey through the Bible. Decoration only. |

## 4. Badges → SVG → Vector Drawable

### Rank badges

The player's rank, shown in the top bar of the map. The bar under it fills up as the player collects stars; every **30 stars** moves the player up one rank.

| Rank | Drawing | File name | Bible meaning |
|---|---|---|---|
| Seeker | Sandals | `badge_rank_1_sandals.xml` | Starting the journey |
| Disciple | Fishing net | `badge_rank_2_net.xml` | "Follow me, and I will make you fishers of men" (Matthew 4:19) |
| Scribe | Quill and scroll | `badge_rank_3_quill.xml` | Knows and writes the Scriptures |
| Prophet | Staff and mantle | `badge_rank_4_staff.xml` | Elijah's mantle passed to Elisha (2 Kings 2:13) |
| Apostle | Fishing boat | `badge_rank_5_boat.xml` | Sent out to share the message |
| Elder | Crown of life | `badge_rank_6_crown.xml` | The highest rank (Revelation 2:10) |

### Achievement badges

Earned once for a special milestone, then kept in the Badges tab. *Drawn but not used in the app yet.*

| Badge | Drawing | File name | How it will be earned |
|---|---|---|---|
| Ark Builder | Ark | `badge_ark.xml` | Finishing the Noah levels |
| Giant Slayer | Sling and stone | `badge_sling.xml` | Beating the Goliath boss |
| Jericho world | Ram's horn trumpet | `badge_trumpet.xml` | Finishing the Promised Land world |
| Daily Challenge (later) | Basket of manna | `badge_manna.xml` | Playing the Daily Verse Challenge |

## 5. Map decorations → WebP with transparency

| Drawing | File name | Notes |
|---|---|---|
| World ribbon banner | `ribbon_world.webp` | **Empty** |
| Wooden signpost | `sign_wood.webp` | **Empty** |
| City walls with closed gate (end of map) | `gate_walls.webp` | Jericho-style walls |
| Buried treasure (reward chest) | `chest_treasure_closed.webp`, `chest_treasure_open.webp` | Closed / open with glow |
| Tent | `deco_tent.webp` | Reusable |
| Palm tree | `deco_palm.webp` | Reusable |
| Rock | `deco_rock.webp` | Reusable |
| Clouds | `deco_cloud_1.webp` … `deco_cloud_3.webp` | 2–3 variants |

## 6. World backgrounds → WebP

| Drawing | File name | Size |
|---|---|---|
| World 1 segments | `bg_world1_01.webp`, `bg_world1_02.webp`, `bg_world1_03.webp` | 1080 × 1920 px, under 200 KB each |
| World 1 → 2 transition | `bg_transition_1_2.webp` | Same |
| Other worlds (later) | `bg_world2_01.webp` … | Same |

- Segments must stack **seamlessly** top to bottom.
- Do **not** draw the path in the background; it's drawn in code so it always connects to the level nodes.
- Export quality around 80%.

## 7. Oli

| Drawing | Format | File name |
|---|---|---|
| Animations (idle, reactions) | Lottie | `oli_idle.lottie`, `oli_correct.lottie`, … (full list in `mascot-oli.md`) |
| Static fallback | WebP with transparency | `oli_idle.webp` |
| Source | Layered Illustrator file | `oli_master.ai` |

## 8. App icon

| Drawing | Format |
|---|---|
| Adaptive icon (Oli foreground + background) | 2 separate SVGs → Android Studio *Image Asset* |
| Play Store icon | 512 × 512 PNG |

---

## Built in code (nothing to draw)

- Top bar, the lamp and Talent "pills", XP bar
- **Play** button
- Bottom navigation bar
- All text and numbers
- Glow, pulse and bounce effects

---

## Bible objects: care rules

- **Sacred objects stay respectful.** The cross, the Ark of the Covenant, the communion bread and cup, the menorah and the Torah scroll are **never** used as currency, power-ups, or things that break or get lost.
- **Standard icons stay standard.** Settings, back, profile and sound use universal icons so players find them instantly.
- **Test recognition.** In the playtest, check that players understand the lamp = lives within a few seconds. If not, add a number label (🪔 5) or switch back to a heart.

---

## Priority for the first playable version

- [ ] 1. Oli (static + `oli_correct`)
- [ ] 2. Lamp, Talent and star icons
- [ ] 3. The 4 node types + footprint
- [ ] 4. Bottom nav icons + settings
- [ ] 5. World 1 backgrounds (2–3 segments)
- [ ] 6. Ribbon + signpost + gate

**Later:** power-ups, rank badges, achievement badges, reward chest, other worlds.

---

## Source files

Keep the Illustrator sources organized:

```
design/
├── icons/
├── powerups/
├── nodes/
├── badges/
├── decorations/
├── worlds/
│   ├── world1/
│   └── world2/
├── oli/
└── app-icon/
```

One `.ai` file per group, each artboard named exactly like its export file.