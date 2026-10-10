# Mancala and per-game tutorials: design spec

Date: 2026-10-10

## Intent

Add Tabletop's second game, Mancala (Kalah rules), and give every game an illustrated "How to play" guide. Mancala adds what Klondike can't: an opponent. It keeps the app's calm, hand-made feel: a wooden board drawn in Compose, stones that drop pit by pit with soft clicks, and a computer opponent that's strong on Hard.

Success:
- Kalah is fully playable against the computer (Easy, Medium, Hard) or against a friend on the same phone.
- The rules are learnable from the tutorial alone; the tutorial opens on a player's first visit to a game and is always reachable from the game's top bar.
- In-progress games survive restarts; stats track games against the computer.
- Klondike gets its own tutorial.

Out of scope: other Mancala rule sets (Oware), per-difficulty stats, hints, online play, interactive tutorials.

## Modules

```
games/mancala/engine   pure Kotlin/JVM: rules, computer opponent, session/undo
games/mancala/ui       Compose board, sowing animation, ViewModel, palette, tutorial pages
```

`engine` has no dependencies besides kotlinx.serialization. `ui` depends on `core/design`, `core/game-api`. `app` lists `MancalaGame.entry` after Klondike.

## Engine

- Board: 14 ints in sowing order. 0–5 are South's pits (the player), 6 South's store, 7–12 North's pits (computer or second player), 13 North's store. Pit `i` faces pit `12 - i`.
- `GameState(board, toMove, opponent, moves)`. `Opponent` is `EASY`, `MEDIUM`, `HARD` or `FRIEND`. South always moves first.
- Kalah(6, 4) rules: sow counter-clockwise skipping the opponent's store; the last stone in your own store gives another turn; the last stone in an empty pit on your side, with stones in the opposite pit, captures both into your store (no capture if the opposite pit is empty). When either side's pits are all empty, each player moves their remaining stones into their own store and the game ends. Bigger store wins; equal is a draw.
- `GameState.sow(pit): Turn?` is null when illegal (wrong side, empty pit, game over). `Turn` carries the dropped-stone path, any capture, whether the board was swept, and the resulting state, so the UI can replay it.
- `Session` keeps undo history (capped at 500). Against the computer, Undo goes back to the last position where it was the player's move; with a friend, one move.
- Computer: negamax with alpha-beta pruning on an `IntArray` board, evaluating store difference. A move that earns an extra turn is searched without switching sides. Moves that earn an extra turn are searched first. Easy: depth 2, and 30% of the time a random legal move. Medium: depth 6. Hard: iterative deepening to depth 14 within a 400 ms budget.

## UI

- Palette: walnut table, warm wood board, amber accent, sea-glass stones.
- Vertical board centered on screen: North's store at the top, South's at the bottom, South's pits in the left column (stones flow down), North's in the right column (stones flow up). Each pit shows its stones (deterministic scatter, with a count label) and is a tappable, labelled node.
- Top bar: back, whose turn it is, How to play (`?`), settings. Bottom bar: New, Undo.
- Playing a move: the session commits immediately (so saves are correct), then the board replays the `Turn`: the pit empties, one stone drops per step (about 140 ms, soft click and light tick), then the capture and end sweep. The system "remove animations" setting skips the replay.
- Computer turn: thinks on a background dispatcher, waits at least 600 ms, then replays like a player move. Input and Undo are disabled while replaying or thinking.
- Settings sheet: opponent (Easy, Medium, Hard, Friend). It applies straight away if no move has been made, otherwise to the next game.
- End panel: "You win 28–20", "The computer wins", "Draw", or "Player 1/2 wins", with Home and New game.

## Sound and haptics

New `FeedbackEvent.Stone`: a short, soft pebble click from `tools/gen_sounds.py`, with a tick haptic. Captures and extra turns use the existing `Foundation` chime; invalid taps use `Invalid`; a win against the computer uses `Win`.

## Stats and persistence

- `save_mancala`: the session plus whether the game counted. `mancala_settings`: the opponent.
- Only computer games count. A game counts as played at its first move. A win records the margin as best score; a loss or abandoning a game resets the streak; a draw changes nothing beyond played.
- `GameStats.recordWin` takes a nullable time. `GameEntry` gains `scoreLabel` so the stats card reads "Best margin" for Mancala and hides "Best time" when a game has none.

## Tutorials

- `GameEntry.tutorial: List<TutorialPage>`, each page a title, a short body and a Compose illustration.
- The app shell owns the tutorial: a bottom sheet in the game's palette with a pager, page dots and Next / Got it. It opens automatically on entering a game whose tutorial hasn't been seen (`tutorial_seen_<id>`) and that has never been played, so existing Klondike players aren't interrupted. The game's `?` button opens it at any time.
- `GameEntry.content` gains an `onHowToPlay` callback for that button.
- Klondike: four pages (goal, building columns, the stock, empty columns and helpers) illustrated with `PlayingCard`.
- Mancala: five pages (your side, sowing, extra turn, capture, game end) illustrated with a miniature of the board.

## Testing

- Engine: initial position, sowing, skipping the opponent's store, extra turns, capture and no-capture cases, end sweep, illegal moves, stone conservation across random games, undo across computer turns, and the computer taking an obvious capture or extra turn and Hard beating Easy.
- UI: ViewModel tests for counting, saving and resuming, the computer replying, undo, abandon and win stats; a Compose test that tapping a pit plays a move. Klondike tests updated for the new signature.
- Manual: emulator screenshots of home, the Mancala board, the tutorial sheet and the end panel.

## Release

Bump to 0.2.0 (versionCode 2), update the README, tag `v0.2.0` and push so the release workflow publishes the signed APK.
