# BibliQ Mascot — Oli the Dove

## Who is Oli?

**Oli** is a small, round, white dove carrying an olive branch. He is the mascot of BibliQ and appears throughout the game to congratulate and encourage players.

---

## Why a dove?

**He comes straight from the Bible.**
In Noah's story (Genesis 8), the dove brings back an olive leaf as the sign that the flood is over. That story is in **World 1**, so players meet Oli in the very first levels, and his story is the game's story: **good news after a hard time.**

**The name works in all three languages.**
"Oli" is short and comes from *olive* (English), *olivier* (French) and *olivo* (Spanish).

**The olive branch doubles as a reward icon.**
For example, a branch drops onto each star the player earns.

**He is respected across churches.**
The dove is a respected symbol for both Catholic and Protestant churches, which helps with pastors and parents, our best promoters.

**He is great to animate.**
Wings give us very expressive movements: flapping, hugging, pointing, celebrating.

---

## What Oli does in the game

| Moment | Oli's reaction |
|---|---|
| ✅ Correct answer | A quick flap and a happy chirp |
| 🔥 Several correct in a row | A loop in the air, leaving a trail of leaves |
| ❌ Wrong answer | A small head tilt and a gentle *"You'll get it next time!"* |
| 💔 Level failed | He lands next to the player with the branch: *"Let's try again together."* |
| ⭐⭐⭐ 3 stars | A celebration dance with the branch |
| ⚔️ Boss level | He looks brave and ready, or slightly nervous for fun |
| 😴 Out of lives | Asleep in a nest, with a countdown above him |
| 📜 New levels on Friday | He flies in carrying a scroll |

### Tone rule

Oli is **always kind**. When the player is wrong or fails, he encourages and never mocks or shames. He is a friend on the journey, not a judge.

---

## Design guidelines

- **Shape:** small, round, chubby body; big head; large expressive dark eyes.
- **Colors:** white body with soft cream shading, golden-yellow beak, golden accents on the wing tips, green olive branch.
- **Style:** clean 2D vector, flat colors, soft shading, bold dark warm-brown outlines.
- **Signature:** the olive branch is always with him.
- **Keep him distinct** from other app mascots, especially Duolingo's owl: white and gold, round dove shape, olive branch.
- **Original artwork only.** AI images may be used as concept references, but the final Oli is redrawn by our team in Illustrator, so the character is fully ours and can be protected with the BibliQ brand.

---

## Animation production

- **Tool chain:** Illustrator (layered SVG) → After Effects or Lottie Creator → **Lottie / dotLottie**.
- **Layers:** `body`, `wing_left`, `wing_right`, `eyes_open`, `eyes_happy`, `beak_closed`, `beak_open`, `branch`, sparkles.
- **Background:** transparent.
- **Length:** short (about 1 second for answer reactions) so the game never feels slowed down.
- **Loops:** idle and sleeping animations must loop cleanly (last frame = first frame).
- **File size:** aim for under 50 KB per animation.
- **Sound:** triggered from the app code, not baked into the animation, so the mute toggle works.

### File names

| Moment | File |
|---|---|
| Correct answer | `oli_correct.lottie` |
| Streak | `oli_streak.lottie` |
| Wrong answer | `oli_wrong.lottie` |
| Level failed | `oli_failed.lottie` |
| 3 stars | `oli_three_stars.lottie` |
| Boss level | `oli_boss.lottie` |
| Out of lives | `oli_sleeping.lottie` |
| New levels | `oli_new_levels.lottie` |

---

## Oli's lines (EN / FR / ES)

| Moment | English | Français | Español |
|---|---|---|---|
| Wrong answer | You'll get it next time! | Tu y arriveras la prochaine fois ! | ¡La próxima vez lo lograrás! |
| Level failed | Let's try again together. | Réessayons ensemble. | Intentémoslo de nuevo juntos. |
| New levels | New levels are here! | De nouveaux niveaux sont là ! | ¡Llegaron niveles nuevos! |
