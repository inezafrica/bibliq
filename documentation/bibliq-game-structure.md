# BibliQ — Game Structure

*Working name: **BibliQ** ("biblique" in French / "Bible IQ" in English / close to "bíblico" in Spanish)*
*Store subtitles: **BibliQ – Bible Trivia** (EN) · **BibliQ – Quiz Biblique** (FR) · **BibliQ – Trivia Bíblica** (ES)*
*Platform: Android (Kotlin) · Languages: English, French & Spanish*

---

## 1. Concept

A multilingual game available in **English, French and Spanish** from launch. The player goes on a **journey through the Bible, from Genesis to Revelation**, on a Candy Crush–style level map. Levels start very easy, mix question types (including image questions), and **new levels are added every week**.

---

## 2. Core Loop

**Play a level (60–90 sec) → earn stars, XP & Talents → unlock the next level → reach the end of the map → come back for the weekly drop**

Short levels make "just one more" easy to say yes to.

---

## 3. The Level Map

- One continuous path of numbered levels (1, 2, 3… forever), grouped into **worlds** that follow the Bible story.
- Each level shows **0–3 stars**, so players can see what to replay.
- Every 10–15 levels: a **boss level**, then a new world opens with a new background and color theme.
- At the end of the map: a locked gate —
  - EN: *"Coming soon: 15 new levels on Friday 🔒"*
  - FR: *"Bientôt : 15 nouveaux niveaux vendredi 🔒"*
  - ES: *"Próximamente: 15 niveles nuevos el viernes 🔒"*
- Players who catch up can replay levels for 3 stars or play Endless mode while they wait.

### Worlds

| World | Books covered | Boss level |
|---|---|---|
| 1. Creation & the Garden | Genesis 1–11 | The Serpent |
| 2. The Patriarchs | Genesis 12–50 | Joseph's Brothers |
| 3. Exodus & the Desert | Exodus–Deuteronomy | Pharaoh |
| 4. The Promised Land | Joshua, Judges, Ruth | The Walls of Jericho |
| 5. Kings & Kingdoms | Samuel, Kings, Chronicles | Goliath |
| 6. Poets & Prophets | Psalms, Proverbs, Isaiah, Daniel… | The Lions' Den |
| 7. The Gospels | Matthew–John | The Storm at Sea |
| 8. The Early Church | Acts & Letters | The Shipwreck |
| 9. Revelation | Revelation | The Final Seal |

*Open decision: include the deuterocanonical books (Tobit, Judith, Maccabees, Wisdom, Sirach…) as a bonus world or a settings toggle?*

---

## 4. Level Rules

- **10 questions** per level.
- **Stars:** 1 star = 7/10 (pass) · 2 stars = 9/10 · 3 stars = 10/10.
- **Timer:** 15–20 seconds per question, bonus points for fast answers.
- After every answer, show the **verse reference** (e.g. *1 Samuel 17:49*), so the game teaches as well as tests.
- **Boss levels:** 15 questions, must score high to pass, special reward chest.

### Lives (replaces the daily streak)

- **5 lives**; failing a level costs one.
- One life regenerates every **20–30 minutes**.
- Refill with a rewarded ad or with Talents.

This spreads play across the day without punishing anyone for missing a day.

---

## 5. Difficulty Curve

| Levels | Difficulty | Question types available |
|---|---|---|
| 1–10 | Very easy — famous stories | True/False, simple MCQ, image questions |
| 11–30 | Easy | + "Who said it?", "Which book?" |
| 31–60 | Medium | + "Complete the verse" |
| 61+ | Mixed, rising slowly | + "Put in order" |

**Pattern inside a world:** easy, easy, medium, easy, medium, hard, easy… The easy level right after a hard one is where players feel clever and keep going.

---

## 6. Question Types

| Type | Example |
|---|---|
| Multiple choice (4 options) | "Who built the ark?" |
| True / False | "Jonah was swallowed by a great fish." (True/False · Vrai/Faux · Verdadero/Falso) |
| Image | "Which story is this?" (picture of an ark with animals) |
| Who said it? | "Here I am, send me." → Isaiah |
| Which book? | "In which book do we find the Ten Commandments?" |
| Complete the verse | "For God so loved the ___." |
| Put in order | Drag events of the Exodus into sequence |

**New type introduction:** the first time a type appears, use a short tutorial level ("New! Complete the verse" / "Nouveau ! Complète le verset" / "¡Nuevo! Completa el versículo").

### Level Template (10 questions)

- 4 × multiple choice
- 2 × true/false
- 2 × image questions
- 1 × "Who said it?" or "Which book?"
- 1 × harder bonus question at the end (for the 3rd star)

### Image Questions

**Formats:** "Who is this?", "What happens next?", "Which story is this?", objects and symbols (sling, stone, basket of loaves).

**Sources (legal):**
- ✅ Your own illustrations (Illustrator) — unique look for the game
- ✅ Public-domain art: Gustave Doré's Bible engravings, classic paintings from before ~1900
- ❌ Film stills, Google Images, modern illustrated Bibles (copyrighted)

**Format:** WebP, ~800px wide, so levels load fast on cheaper phones and slow data.

**No text inside images**, so the same image works in all three languages.

---

## 7. Progression & Economy

### XP and Ranks

Seeker → Disciple → Scribe → Prophet → Apostle → Elder

A visible rank badge on the profile gives status and something to show off.

### Talents (coins, from the parable)

Spent on power-ups:
- **50/50** — removes two wrong answers
- **Extra time**
- **Skip question**
- **Reveal the verse** — shows the passage as a hint
- **Refill lives**

---

## 8. Engagement Hooks

- **Weekly level drops** + push notification: *"🔓 15 new levels are here!" / "🔓 15 nouveaux niveaux sont là !" / "🔓 ¡Ya llegaron 15 niveles nuevos!"*
- **Daily Verse Challenge** — one question per day, same for everyone in their own language, shareable result (Wordle-style) for WhatsApp and Telegram.
- **Reward chests** after bosses and milestones.
- **Badges** — "Read all 4 Gospels", "10 perfect levels", "Defeated Goliath"…
- **Weekly leagues** — compete with ~30 players of similar level; top players move up.
- **Review mode** — missed questions come back later (spaced repetition).

---

## 9. Game Modes

1. **Journey** — the main level map
2. **Endless / Survival** — play until 3 mistakes; personal best + leaderboard
3. **Daily Challenge** — the shareable question of the day
4. **Practice by Book** — pick any book and train on it
5. **Duel** *(version 2)* — real-time 1v1 against friends or random players

---

## 10. Technical Architecture

### Levels are downloaded, not hard-coded

If levels ship inside the APK, every weekly drop needs a Play Store update. Instead, host level packs online (S3 + CloudFront, or Firebase):

```
levels/
  manifest.json      → { "latestLevel": 45, "nextDropDate": "2026-10-02" }
  pack_001.json      → levels 1–15
  pack_002.json      → levels 16–30
  pack_003.json      → levels 31–45
images/
  q_0042.webp
```

- On launch, the app checks `manifest.json` and downloads any new packs.
- Packs and images are **cached for offline play**.
- **Pack 1 is bundled in the APK** so new players can start instantly without internet.
- `nextDropDate` powers the "Coming soon" gate at the end of the map.

### Languages

- Supported languages are defined in **one place**: `SUPPORTED_LANGUAGES = listOf("en", "fr", "es")`
- All translatable content uses **maps keyed by language code**, so adding a new language later (Portuguese, Kinyarwanda, Swahili…) needs content only, no model changes.
- **Fallback:** if a translation is missing, English is shown.
- The app follows the phone's language by default, with a language switch in settings.

### Data Model (Kotlin)

```kotlin
val SUPPORTED_LANGUAGES = listOf("en", "fr", "es")

enum class QuestionType {
    MCQ, TRUE_FALSE, IMAGE, WHO_SAID, WHICH_BOOK, COMPLETE_VERSE, ORDER
}

@Serializable
data class Level(
    val number: Int,
    val world: Int,
    val isBoss: Boolean,
    val starThresholds: List<Int>,          // e.g. [7, 9, 10]
    val questionIds: List<String>
)

@Serializable
data class Question(
    val id: String,                         // "q_0001"
    val type: QuestionType,
    val difficulty: Int,                    // 1–5
    val text: Map<String, String>,          // {"en": "...", "fr": "...", "es": "..."}
    val options: Map<String, List<String>>, // same order in every language
    val correctIndex: Int,                  // valid for all languages
    val imageUrl: String? = null,           // only for IMAGE questions
    val reference: Map<String, String>      // {"en": "Genesis 6:14", "fr": "Genèse 6:14", "es": "Génesis 6:14"}
)

@Serializable
data class LevelPack(
    val packId: Int,
    val levels: List<Level>,
    val questions: List<Question>
)

// Used by the UI: one language, English fallback applied
fun Question.localize(language: String): LocalizedQuestion
```

### Validation (unit tests on every pack)

- Every level has exactly 10 questions, all ids exist, no duplicates
- `text`, `options` and `reference` contain every supported language
- Options have the same count in every language; `correctIndex` is in range
- MCQ has 4 options, TRUE_FALSE has 2; only IMAGE questions have an `imageUrl`
- No blank text, option or reference

### Animations

- **Lottie** (`lottie-android`) is the animation format for in-app motion: correct/incorrect answer feedback, star reveal, level-complete celebration, boss victory, lives/streak UI.
- Animation `.json` files ship as bundled assets (like Pack 1), so they work offline; later ones can be hosted alongside level packs if per-world themed animations are added.
- Lottie files are vector-based and carry no text, so one animation asset works across all three languages with no localization cost.

---

## 11. Content Production

**Weekly target:** 15 levels = **150 questions**, each in English, French and Spanish.

**Launch buffer:** have 60–100 levels ready before launch, so weekly drops never slip.

**Suggested pipeline (n8n):**
1. Claude drafts questions + English/French/Spanish translations by world and difficulty, in the same step.
2. Questions land in a **Google Sheet** review queue, with **one review column per language**.
3. Every question is checked against the Bible text, and each language is approved separately (ideally a native speaker reviews Spanish and French).
4. Approved rows are exported automatically to JSON level packs.
5. Validation tests run on the pack; it is only uploaded if all languages pass.
6. Packs are uploaded to hosting and `manifest.json` is updated.
7. Push notification announces the drop in each player's language.

---

## 12. Monetization (family-friendly)

- ✅ Rewarded ads for extra lives or Talents
- ✅ Small one-time premium unlock (no ads + bonus Talents)
- ❌ No pay-to-win, no paid loot boxes

The audience includes kids, church groups, and families. Pastors and parents are the best marketers, so keep the game something they're happy to recommend.

---

## 13. Open Decisions

- [ ] Confirm the name **BibliQ** (store, trademark, domain & social handles check)
- [ ] Include deuterocanonical books?
- [ ] Number of levels ready at launch
- [ ] Hosting: S3 + CloudFront or Firebase
- [ ] Bible translations used for verse references (EN / FR / ES)
- [ ] Spanish reviewer for question content
- [ ] Spanish-speaking markets to target first