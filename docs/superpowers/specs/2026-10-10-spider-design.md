# Spider Solitaire: design spec

Date: 2026-10-10

## Intent

Add Spider Solitaire as Tabletop's third game. It's a two-deck solitaire about planning rather than luck of the draw, with 1, 2 and 4-suit difficulty. It should feel like Klondike's sibling: the same drag and tap controls, motion, sounds and win cascade. To get there without a second copy of Klondike's table code, the general parts of that code move into a shared module first.

Success:
- Spider is fully playable in all three suit modes with drag, tap-to-move, unlimited undo and hints, and games survive restarts.
- Ten columns of two decks stay readable on a portrait phone, and tall columns compress so nothing runs off screen.
- Klondike behaves exactly as before, and its existing tests pass unchanged apart from imports.

Out of scope: auto-finish, per-mode stats, left-handed layout for Spider.

## Shared table module

New `core/solitaire-ui` (depends on `core/cards-ui`), holding what both solitaire tables need:

- `CardPlacement<P>`: one card's key, card, face-up flag, pile, index in pile, top-left and z order. `key` identifies a physical card: Klondike uses the `Card`; Spider uses a per-card id, because two decks contain duplicates.
- `CardTableLayout<P>`: card size, pile drop areas and placements, with `placementOf(key)`, `cardAt(point)` and `dropCandidates(rect)`.
- `fanOffsets(...)`: vertical offsets for a column, compressed to fit the available height (moved from Klondike's layout).
- `CardTable`: renders the cards with spring movement, opening-deal stagger and drag lift, and owns the single gesture layer. Games supply callbacks: tap (with the card under the finger, if any), which cards a drag picks up, pickup feedback and drop.
- `HintOutline`, `hintGlow`, `WinCascade` and the `TutorialCards` illustration helper, all taking colors as parameters.

Klondike keeps its own layout function, slots, stock and hint target logic, and builds them on these pieces.

## Engine (`games/spider/engine`)

- `SpiderCard(id, card, faceUp)`; ids 0–103 are unique.
- `Suits` is `ONE`, `TWO` or `FOUR`: two decks built from spades only, spades and hearts, or all four suits.
- `GameState(seed, suits, stock, tableau[10], completed, score = 500, moves, elapsedMs)`. Deal from a seed: columns 0–3 get 6 cards and 4–9 get 5, with the top card face up; the 50 remaining form the stock (5 deals).
- `Move.Deal` puts one face-up card on every column. It is only allowed while the stock has cards and no column is empty.
- `Move.Transfer(from, count, to)`: the top `count` cards of a column move if they are face up and form a descending run in one suit. They can go onto a card one rank higher of any suit, or into an empty column.
- After every move, a column ending in a face-up K-to-A run of one suit sends that run to `completed`. Exposed face-down cards flip up automatically. Eight completed runs win.
- Scoring (Windows): start at 500, −1 per move or undo, +100 per completed run, never below 0.
- `Session`: undo stack capped at 500; undo counts as a move.
- Helpers: `bestMoveFor(column, count)` for tap-to-move (prefers a same-suit target, then any legal card, then an empty column), and `hint()`, which ranks moves that complete or extend same-suit runs, reveal a card or empty a column, skips pointless shuffles, and suggests dealing when nothing else helps.

## UI (`games/spider/ui`)

- Palette: midnight blue table, periwinkle accent, blue card backs.
- Top row: completed runs fanned on the left (kings showing), the stock on the right as one fanned back per remaining deal. Ten columns below, with card width from the screen width.
- Controls and chrome match Klondike: time, moves and score at the top; How to play and settings buttons; New, Undo and Hint at the bottom. Tap the stock to deal. If a column is empty, the deal is refused with the invalid sound and a short "Fill every column first" note.
- Settings: suits (1, 2, 4; applies to the next game), tap to move, four-color suits.
- Win: completed cards cascade off, then a panel with time, moves, score and bests.
- Tutorial: four pages (goal, moving cards, dealing, empty columns and helpers).

## Stats and persistence

Same as Klondike: `save_spider` holds the session; a game counts as played after the first move; winning records time and score; starting over an unfinished game breaks the streak.

## Testing

- Engine: deal shape and unique ids per mode, legal and illegal transfers, dealing rules, run completion (including from a deal), auto flip, scoring, undo, tap targets, and a seeded random-play test driven by hints that checks invariants (104 unique ids, face-down cards never above face-up ones).
- Shared module: layout helpers (fan compression, hit testing, drop candidates).
- UI: ViewModel tests (save and resume, counting, deal refusal, win), a Compose test that tapping the stock deals.
- Klondike: the existing suites keep passing.
- Manual: emulator screenshots in 1- and 4-suit modes, and of Klondike after the refactor.

## Release

Version 0.3.0 (versionCode 3), README updated, tag `v0.3.0`, signed APK published to the GitHub release.
