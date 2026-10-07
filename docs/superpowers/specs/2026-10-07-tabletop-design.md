# Tabletop: design spec

Date: 2026-10-07

## Intent

Tabletop is an open-source (GPL-3.0) Android app of curated mini games. It is ad-free, offline and tracker-free. Each game feels hand-made: its own palette, motion, sounds and haptics inside a shared, calm app shell. The first game is Klondike Solitaire, shipped polished rather than broad.

Success for v1:
- Klondike is fully playable, smooth (60fps+ drags and animations on a mid-range phone) and intuitive without a tutorial.
- In-progress games survive app restarts.
- A signed release APK can be built locally; later a GitHub Actions workflow attaches APKs to GitHub Releases.
- Nothing in the app blocks F-Droid inclusion (no Google Play Services, no proprietary deps, no network).

Out of scope for v1: other games, other Solitaire variants, online features, achievements, cloud sync, the GitHub Actions workflow (follow-up).

## Stack

- Kotlin, Jetpack Compose (Material 3), Navigation Compose.
- kotlinx.serialization + DataStore for persistence. No Room, no DI framework (manual wiring).
- JDK 21, Android SDK installed locally in `~/Android/Sdk`. minSdk 26, targetSdk/compileSdk 36.
- Gradle Kotlin DSL with a version catalog (`gradle/libs.versions.toml`).
- Package/applicationId: `dev.tabletop`. Release builds use R8 minify and resource shrinking.

## Module layout

```
app/                  shell: home, settings, stats screens, navigation, game registry
core/design/          theme tokens, typography, motion specs, shared components
core/feedback/        sound (SoundPool) and haptics, behind a small Feedback interface
core/cards-model/     pure Kotlin/JVM: Suit, Rank, Card
core/cards-ui/        Compose card rendering, reusable by any card game
games/klondike/engine pure Kotlin/JVM module: rules, deal, moves, undo, scoring, hints
games/klondike/ui     Compose table, gestures, animations, Klondike palette, ViewModel
```

Dependency rules: `engine` depends only on `core/cards-model` (no Android deps). `ui` modules depend on `core/*`. `app` depends on everything and nothing depends on `app`.

### Game registry

Each game exposes one `GameEntry`:

```kotlin
data class GameEntry(
    val id: String,
    val title: String,
    val tagline: String,
    val palette: GamePalette,        // accent colors used for its home tile
    val content: @Composable (onExit: () -> Unit) -> Unit,
)
```

`app` holds `val games = listOf(KlondikeGame.entry)`. Adding a game means adding a module and one list entry.

## Klondike engine

- Immutable `GameState`: `stock`, `waste`, `foundations[4]`, `tableau[7]` (each pile a list of cards with face-up flag), `drawMode` (ONE or THREE), `score`, `moves`, `elapsedMs`, `seed`, `redeals`.
- `Move` sealed type: `Draw`, `Recycle`, `WasteTo(target)`, `TableauTo(fromPile, fromIndex, target)`, `FoundationTo(pile, toTableau)`. Turning the top tableau card face-up happens automatically after a move.
- `fun GameState.apply(move: Move): GameState?` returns null when illegal.
- Undo: `GameSession` keeps a stack of previous `GameState`s. Unlimited undo; undo costs points per standard scoring.
- Deal from a seed (`Random(seed)` Fisher-Yates) so deals are reproducible.
- Scoring (Windows standard): waste to tableau +5, waste to foundation +10, tableau to foundation +10, turn over tableau card +5, foundation to tableau -15, recycle waste in draw-1 -100 (floor at 0). Timer runs only while the game screen is visible.
- Helpers: `bestMoveFor(card location)` for tap-to-move (prefer foundation, then a tableau pile that reveals a card or accepts a king to an empty pile), `hint(): Move?`, `canAutoComplete()` (stock and waste empty and all tableau cards face up), `autoCompleteMoves()`, `isWon()`.
- Unlimited passes through the stock in both draw modes.

## UI and feel

Visual direction: calm modern minimal.

- **Home:** large, calm game tiles in each game's palette with a small drawn illustration, game title and tagline, plus a "Continue" chip when a saved game exists. A muted "More games soon" tile. Settings and stats from the top bar.
- **Shell theme:** Material 3 with dynamic color (Android 12+) and a fallback palette, following system light/dark.
- **Klondike table:** deep ink-green table with a soft radial vignette. Flat cards with rounded corners, large corner indices, a simple pip/face design drawn in Compose (no bitmap art), and an optional four-color-suits mode. Card width derives from the available width (7 columns plus gutters), and tableau fan spacing compresses when a pile gets tall so nothing goes off screen. Portrait first; landscape uses the same layout scaled to height.
- **Gestures:** drag a card and everything stacked on it; on drop, snap to the nearest legal target under the finger or spring back. Tap a card to auto-move it to its best target (setting, on by default). Tap the stock to draw. Double tap is not used.
- **Motion:** spring animations for moves, snap-back and deals; a 3D flip when a card turns. The opening deal animates from the stock. Respect the system "remove animations" setting.
- **Chrome:** slim top row with time, moves and score. Bottom bar: New game, Undo, Hint (pulses the suggested card and target), and an Auto-finish button that appears when `canAutoComplete()`.
- **Win:** foundation cards cascade gently across the table, then a bottom sheet with time, moves, score, personal bests and "New game".
- **Settings (Klondike):** draw 1/3 (applies to the next game), tap-to-move, four-color suits, left-handed layout (stock and foundations mirrored). **Global settings:** sound on/off, haptics on/off.

## Sound and haptics

- `core/feedback` exposes `Feedback { fun play(event: FeedbackEvent) }` with events like `CardPickup`, `CardPlace`, `CardFlip`, `Foundation`, `Invalid`, `Deal`, `Win`.
- Sounds: short, soft, calm WAV files (paper flicks, soft taps, a gentle chime for foundation and a soft arpeggio for win). They are generated by a script checked into the repo (`tools/gen_sounds.py`, synthesized noise bursts and sine tones with envelopes) so the audio license is unambiguous. Played via `SoundPool`, low volume, respecting the media volume.
- Haptics: `View.performHapticFeedback` with constants matched to events (light tick on pickup, confirm on place/foundation, reject on invalid drop). Falls back gracefully on older APIs.
- Both are toggled in global settings and default on.

## Persistence

- DataStore with JSON (kotlinx.serialization):
  - `klondike_save`: the current `GameState` plus undo stack (capped at the last 500 states), written after every move with a debounce, and on pause.
  - `settings`: global and per-game settings.
  - `stats`: per game: played, won, best time, best score, current streak, best streak.
- A game counts as played once the first move is made. Starting a new game over an unfinished one ends the streak.
- A corrupt or unreadable save is discarded and the user simply starts a new game.

## Testing

- Engine: JUnit tests covering legal and illegal moves for each move type, auto flip, scoring, undo, recycle, draw-3 behavior, deal reproducibility from a seed, win detection, auto-complete and hint never suggesting illegal moves. Also a randomized test that plays many seeded games using hints and checks invariants (52 unique cards always, piles valid).
- Persistence: round-trip serialization tests of `GameState`.
- UI: a few Compose UI tests (tap stock draws, tap-to-move, undo).
- Manual: run on a headless emulator and capture screenshots to check visuals.

## Release

- Local signed release build once a keystore exists (keystore kept out of git; signing config read from `keystore.properties` or env vars).
- Follow-up task (not v1): GitHub Actions workflow that builds and signs on tag push and attaches the APK to the GitHub Release.
- Repo files: `README.md` (short), `LICENSE` (GPL-3.0), `.gitignore`.
