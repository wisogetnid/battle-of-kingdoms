# Spec: Battle of Kingdoms — Text Web UI (Stage 2 client)

Source: the "Stage 2 client: chat bot vs. minimal web page" decision in
[docs/ideas/battle-of-kingdoms-core-loop.md](docs/ideas/battle-of-kingdoms-core-loop.md) —
this spec picks the **minimal server-rendered page**.

**Status:** new. Built **in parallel with Iteration 3** ([SPEC.md](SPEC.md)) and kept
separate from it: this spec changes no game rule, no JSON route and no DTO. Its plan and
tasks live in `tasks/web-ui/`, not in `tasks/plan.md` / `tasks/todo.md`.

## Objective
A browser UI that lets 3–5 friends play a full game with zero setup: open a link, type a
name, play. **Text and hyperlinks only** — plain HTML with no CSS, no JavaScript and no
images. Actions are unstyled HTML forms (a button, or a few number fields for the army).

**Who:** the developer playing all seats in several browser tabs, then friends on the same
network.

**Why:** the core-loop idea doc's last open assumption is "friends will actually play",
tested with a zero-setup Stage 2 client. `.http` files are fine for the developer, not for
friends.

**Done means:** a complete game can be created, joined and played to `GAME_OVER` in a
browser, and the API tests prove the pages show exactly what the JSON player view shows.

### User stories
- As a host, I open the home page, enter the player count and my name, and land on my game
  page, which shows an invite link to share.
- As a player, I open the invite link, enter my name, and land on my game page.
- As a player, my game page shows the game status, my hand, the other players (VP, hand
  size, whether they have acted), the battle log, and the actions I can take right now.
- As a player, I trade, upgrade, mark ready and commit my army with form buttons; after
  each one I'm back on my game page showing the new state.
- As a player, I click **Refresh** to see what the others have done.
- As a player, when an action is rejected I see the reason on my game page.

## Pages and routes
All UI routes are new, under `/` and `/ui`; the JSON API under `/games` is untouched.
Forms post `application/x-www-form-urlencoded` (read with `call.receiveParameters()`).

| Method & path | Form fields | Success | Page / purpose |
|---|---|---|---|
| `GET /` | query `game` (optional) | `200` | Home: "Create game" form; with `?game=` a "Join game" form for that game, otherwise an "Open game" GET form asking for the id (a form can't put a typed id into the join path) |
| `POST /ui/games` | `players`, `name`, `seed` (optional) | `303` → my game page | Create a game; the host joins it |
| `POST /ui/games/{id}/players` | `name` | `303` → my game page | Join |
| `GET /ui/games/{id}/players/{name}` | — | `200` | My game page |
| `POST /ui/games/{id}/players/{name}/trades` | `resource` | `303` → my game page | Trade one resource for one Horde |
| `POST /ui/games/{id}/players/{name}/upgrades` | `into` | `303` → my game page | Upgrade one unit |
| `POST /ui/games/{id}/players/{name}/ready` | — | `303` → my game page | Finish build-up |
| `POST /ui/games/{id}/players/{name}/army` | one count per creature, e.g. `HORDE=2&KNIGHT=1` (blank or 0 = none) | `303` → my game page | Commit to the battle |

- **Post/Redirect/Get:** every successful POST answers `303 See Other` to the game page, so
  reloading never repeats a move. State only changes on POST; GET pages are read-only.
- **Errors** use the same status mapping as the JSON API (400 / 404 / 409). A rejected
  action re-renders the game page with that status and the error message at the top. A
  rejected create or join re-renders the home page with the message. An unknown game or
  player shows a short "not found" page with a link home.
- Player names are trusted, as in the JSON API: anyone with a game-page URL can act as that
  player. No auth.

### Game page content (top to bottom)
1. Title line: game id, "You are {name}", links **Refresh** and **Home**.
2. Error message, if the last action was rejected.
3. Status: phase, turn, pot, VP to win; the winner once the game is over.
4. While `WAITING_FOR_PLAYERS`: the invite link (`/?game={id}`) and who has joined.
5. Me: hand (count per creature and resource), units upgraded this turn, VP, whether I've
   acted.
6. Others: one line each — name, VP, hand size, ready/committed or not.
7. Actions, driven only by `availableActions`:
   - `TRADE`: one button per resource in my hand ("Trade 1 Wood for 1 Horde").
   - `UPGRADE`: one button per non-Horde creature type, labelled with its cost
     ("Upgrade Bow into Warbow (1 Wood)").
   - `READY`: one button.
   - `COMMIT_ARMY`: one form with a number field per creature in my hand (0 to the count I
     hold) and a "Commit army" button; all zero commits nothing.
   - None: a line saying what I'm waiting for (other players, or the game is over).
8. Battle log, newest first: turn, pot, winner (or "no winner, pot rolls over"); per player
   army, score, casualties, VP gained; every duel as "anna 6 – 7 ben".

Creature and resource names are shown in words ("High Mage", "Wood"), never as enum
constants.

## Architecture
The UI is the **Presentation** layer from SPEC.md's architecture, rendered on the server.
```
web/       WebRoutes (form parsing, PRG redirects, error pages), Pages (HTML rendering)
  │          calls GameServer, renders PlayerView
server/    unchanged: JSON routes, DTOs, GameServer
  │
domain     unchanged
```
- `web/` depends on `server/` (`GameServer`, `PlayerView`, `armyOf`, the exception →
  status mapping) and on `cards/` for creature and resource names and upgrade costs only.
- **Everything a page shows comes from `PlayerView.of(...)`** — the same DTO the JSON API
  returns — so hidden information stays hidden by construction. Pages never read `Game`
  or `Player` directly.
- **No game logic in `web/`.** The UI never decides whether an action is allowed; it shows
  what `availableActions` lists and reports what the engine rejects.
- **Following Iteration 3 without blocking it:** buttons are generated from
  `availableActions`, `CreatureType.entries` and the hand, so trading and tier-2 units
  appear in the UI as soon as Iteration 3 lands them, with no UI change. If the UI needs
  something that isn't in the player view, ask first instead of changing the DTO.
- Touches to shared files are limited to: one dependency line in `build.gradle.kts`, one
  `webRoutes(server)` call in `Application.kt` (sharing the same `GameServer` instance as
  the JSON routes), and the exception → status mapping in `GameRoutes.kt` made `internal`
  so both route sets use it.

## Tech Stack
As in SPEC.md (Kotlin 2.1.0, Java 21 via mise, Ktor 3.0.3), plus **one new dependency**:
`io.ktor:ktor-server-html-builder:$ktor_version` (kotlinx.html DSL; escapes all text,
including player names). No CSS, no JavaScript, no templates, no static files.

## Commands
```
Test:    mise run test                # ./gradlew test
Build:   mise run build               # ./gradlew build
Run:     mise run server              # then open http://localhost:8080/
```

## Project Structure
```
SPEC-web-ui.md                                  → this spec
tasks/web-ui/plan.md, tasks/web-ui/todo.md      → plan and tasks for this spec
src/main/kotlin/org/battleofkingdoms/web/
  WebRoutes.kt   → Route.webRoutes(server): home, create, join, game page, action POSTs
  Pages.kt       → homePage(...), gamePage(view, error), notFoundPage(...) as kotlinx.html
src/test/kotlin/org/battleofkingdoms/web/
  WebRoutesTests.kt  → testApplication tests per route
  WebGameTest.kt     → one full game played through the forms
```

## Code Style
Same as SPEC.md. Routes stay thin — parse the form, call one `GameServer` method,
redirect — and rendering is one small function per page section:
```kotlin
post("/trades") {
    webAction { server.trade(gameId(), playerName(), ResourceType.valueOf(form()["resource"])) }
}

private fun FlowContent.others(view: PlayerView) = ul {
    view.others.forEach { li { +"${it.name}: ${it.vp} VP, ${it.handSize} cards, ${if (it.acted) "done" else "thinking"}" } }
}
```
Unknown or malformed form values (an unknown resource, a non-number count) are a `400`
with a message, never a `500`.

## Testing Strategy
- **Route tests** (`testApplication`, JUnit 5): every route's success path (status, `303`
  `Location` header) and each error status; the error message appears on the re-rendered
  page; reloading the game page after a POST changes nothing.
- **Content tests:** the game page lists exactly the action forms in `availableActions`;
  it never contains another player's hand or `upgradedThisTurn`, or any uncommitted army;
  a player named `<b>x</b>` appears escaped.
- **Text-only check:** no page contains `<script`, `<style`, `<img`, `<link` or a `style=`
  attribute.
- **Full game through the forms:** 3 players, fixed seed, played via the UI POSTs to
  `GAME_OVER`. Asserts structure (phases advance, a winner is shown, the battle log grows),
  **not** seeded numbers, so Iteration 3's rule changes don't break it.
- Existing JSON API, unit and end-to-end tests stay green and unchanged.

## Boundaries
- **Always:** render from `PlayerView` only; POST for every state change with a `303`
  redirect; run `mise run test` before handing work over.
- **Ask first:** changing `PlayerView`, other DTOs, JSON routes or the engine; adding any
  dependency beyond `ktor-server-html-builder`; adding CSS, JavaScript, images or
  auto-refresh.
- **Never:** put game rules in `web/`; change anything in SPEC.md or Iteration 3's
  `tasks/plan.md` / `tasks/todo.md`; add auth, sessions, cookies, WebSockets or
  persistence; skip or weaken tests to get to green; commit (the user reviews and commits).

## Success Criteria
1. `mise run build` passes with every test green, including all pre-existing tests unchanged.
2. A 3-player game can be created, joined and played to `GAME_OVER` in three browser tabs
   using only links and form buttons (manual check), and the seeded UI game test does the
   same.
3. Every state change is a POST answered with `303`; every GET is side-effect free.
4. No page contains `<script`, `<style`, `<img`, `<link` or `style=` (asserted).
5. The game page shows exactly the actions in `availableActions` and no information missing
   from that player's JSON view (asserted).
6. Rejected actions show the engine's message with the matching 400 / 404 / 409 status.
7. Once Iteration 3's trading and tier-2 units are merged, they work in the UI without any
   change to `web/`.

## Open Questions
None blocking. To decide after the first session with friends: whether the manual Refresh
link is enough or waiting players want auto-refresh; whether names in URLs (no auth) are
acceptable beyond a trusted group.
