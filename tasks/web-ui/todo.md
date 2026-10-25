# Todo: Text Web UI (Stage 2 client)

Plan: [plan.md](plan.md) · Spec: [SPEC-web-ui.md](../../SPEC-web-ui.md)

## Phase 1 — Skeleton
- [x] **W1. Share the status mapping**
  - Acceptance: `status(e: GameException)` in `GameRoutes.kt` is `internal`; no other change to that file; JSON API behaviour unchanged
  - Verify: `mise run test` (all existing tests green, none edited)
  - Files: `server/GameRoutes.kt`

- [x] **W2. Skeleton and home page**
  - Acceptance: `io.ktor:ktor-server-html-builder:$ktor_version` added; `Application.module()` creates one `GameServer` and passes it to `gameRoutes` and `webRoutes`; `GET /` returns `200` HTML with a "Create game" form (players, name, optional seed → `POST /ui/games`) and a "Join game" form (game id, name), the id prefilled from `?game=`; `displayName` turns enum names into words; a test helper `assertTextOnly(html)` fails on `<script`, `<style`, `<img`, `<link` or `style=`
  - Verify: `mise run test`; `WebRoutesTests` covers `GET /` with and without `?game=` and asserts text-only
  - Files: `build.gradle.kts`, `server/Application.kt`, `web/WebRoutes.kt`, `web/Pages.kt`, `web/WebRoutesTests.kt`

- [x] **W3. Create, join and the game page**
  - Acceptance: `POST /ui/games` creates the game, joins the host and answers `303` to `/ui/games/{id}/players/{name}`; `POST /ui/games/{id}/players` joins and redirects the same way; invalid player count, blank or duplicate name, full game re-render the home page with the message and 400/409; `GET /ui/games/{id}/players/{name}` shows the title line with Refresh and Home links, phase, turn, pot, VP to win, winner when over, the invite link `/?game={id}` and joined players while waiting, my hand (in words), VP and acted flag, and one line per other player (VP, hand size, acted); unknown game or player returns `404` with a link home; names are HTML-escaped and URL-encoded in links
  - Verify: `mise run test`; tests for each redirect `Location`, each error status, a name `<b>x</b>` shown escaped, a name with a space linking correctly, no other player's hand on the page, every page text-only
  - Files: `web/WebRoutes.kt`, `web/Pages.kt`, `web/WebRoutesTests.kt`

**Checkpoint A (passed):** build green; a 3-player game created and joined in three browser tabs. Review follow-up done: the hand lists Horde first, then the other creatures, then resources alphabetically.

## Phase 2 — Playing
- [x] **W4. Build-up actions**
  - Acceptance: `POST …/upgrades` (`into`), `POST …/trades` (`resource`) and `POST …/ready` call `GameServer` and answer `303` to the game page; the page shows, per `availableActions`: one "Upgrade X into Y (1 Z)" button per non-Horde creature type, one "Trade 1 Z for 1 Horde" button per resource in hand, one "Ready" button; units upgraded this turn are listed under my hand; a rejected action (wrong phase, already ready, can't afford) re-renders the game page with the engine's message and 400/409; an unknown `into` or `resource` is a 400, not a 500; with no available actions the page says what I'm waiting for
  - Verify: `mise run test`; tests for each route's success and error statuses, forms present exactly when their action is available, reloading the game page after a POST changes nothing
  - Files: `web/WebRoutes.kt`, `web/Pages.kt`, `web/WebRoutesTests.kt`
  - Note: trading shipped in Iteration 3, so the trade buttons are tested end to end like the others. Also set the hand-built view in `WebRoutesTests` to `victoryPointsToWin = 3` (left to this task by Iteration 4's plan)

- [x] **W5. Battle**
  - Acceptance: in the battle phase the page shows one form with a number field (0 to count held) per creature in my hand and a "Commit army" button; `POST …/army` builds the army with `armyOf` from the non-blank fields and answers `303`; all zero commits nothing; a negative or non-number count is a 400 with a message; the battle log is shown newest first, numbered by position (the view has no turn field): pot, winner or "no winner, pot rolls over", per player army, score, casualties and VP gained, every duel as "a 6 – 7 b"
  - Verify: `mise run test`; tests for commit success, empty commit, malformed counts, committing more than owned (400), and a resolved battle appearing in every player's log without anyone's uncommitted army
  - Files: `web/WebRoutes.kt`, `web/Pages.kt`, `web/WebRoutesTests.kt`

- [ ] **W6. Full game through the forms and docs**
  - Acceptance: `WebGameTest` plays a seeded 3-player game only through the UI POSTs and game pages: everyone marks ready each build-up; one player commits their whole hand (counts read from the JSON player view), the others commit nothing; it loops until the page shows `GAME_OVER` (capped at 20 turns) and asserts the winner is shown and the battle log grew every turn, with no seeded numbers or VP target asserted (works with Iteration 4's 3 VP: the lone committer wins every pot); README gains a "Play in the browser" section (`mise run server`, open `http://localhost:8080/`, share the invite link)
  - Verify: `mise run build`; manual full game in three browser tabs
  - Files: `web/WebGameTest.kt`, `README.md` (new section only; Iteration 4's T3 rewrites the rules text)

**Checkpoint B:** all 7 success criteria in SPEC-web-ui.md met.
