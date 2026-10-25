# Plan: Text Web UI (Stage 2 client)

Spec: [SPEC-web-ui.md](../../SPEC-web-ui.md) · Tasks: [todo.md](todo.md)

Runs in parallel with Iteration 3 ([tasks/plan.md](../plan.md)); neither plan edits the
other's tasks.

## Architecture decisions
1. **Share the error mapping first, in its own slice.** `status(e: GameException)` in
   `GameRoutes.kt` becomes `internal` and nothing else in that file changes. This is the only
   edit to a file Iteration 3 also touches, so it stays a one-line diff that merges easily.
2. **One `GameServer`, two route sets.** `Application.module()` creates the `GameServer`
   once and passes it to `gameRoutes` and `webRoutes`, so a game created in the browser is
   also visible through the JSON API (handy for tests and debugging).
3. **Routes in `WebRoutes.kt`, HTML in `Pages.kt`.** Routes parse the form, call one
   `GameServer` method and redirect with `303`; pages are pure functions from `PlayerView`
   (+ optional error message) to kotlinx.html. Page functions can be tested without HTTP.
4. **One error wrapper.** `webAction { … }` mirrors `respondOrError`: on success it
   redirects to the game page; on `GameException` it re-renders the game page with
   `status(e)` and the message; malformed form values (unknown enum name, non-number count)
   become a `RuleViolation`-style 400, never a 500.
5. **Buttons come from data, not from rules.** Trade buttons from the hand's resources,
   upgrade buttons from `CreatureType.entries` minus Horde (cost from `upgradesFrom` and
   `upgradeResource`), army fields from the creatures in the hand, each section shown only
   when its `Action` is in `availableActions`. So Iteration 3 features appear without UI
   changes.
6. **Display names in one place.** A small `displayName` helper turns `HIGH_MAGE` into
   "High Mage" and `WOOD` into "Wood"; pages never print enum constants.
7. **Full-game test is rule-agnostic.** One player commits their whole hand, the others
   commit nothing, so the same player wins every battle (spec: a single committer wins)
   regardless of bonuses or VP target. The test loops until the page shows `GAME_OVER`,
   with a turn cap, and asserts no seeded numbers.

## Dependency graph
```
W1 share status mapping
 └─ W2 dependency, wiring, home page, text-only check
     └─ W3 create, join, read-only game page, not-found page
         ├─ W4 build-up actions: upgrade, trade, ready
         └─ W5 battle: commit army, battle log
             └─ W6 full game through the forms + README  (needs W4 and W5)
```
W4 and W5 both add to `WebRoutes.kt` and `Pages.kt`, so they run in sequence even though
they don't depend on each other.

## Phases and checkpoints

### Phase 1 — Skeleton
**W1. Share the status mapping.** `status(e)` made `internal`; no behaviour change.

**W2. Skeleton and home page.** `ktor-server-html-builder` added; `Application.module()`
shares one `GameServer`; `GET /` renders the create and join forms (`?game=` prefills the id);
a test helper asserts a page is text-only.

**W3. Create, join and the game page.** `POST /ui/games` and `POST /ui/games/{id}/players`
with `303` redirects and error re-rendering on the home page; `GET` game page with title
line, Refresh/Home links, status, invite link while waiting, my hand and VP, the other
players; a not-found page for unknown games and players.

**Checkpoint A:** `mise run build` green; manually create and join a 3-player game in
three browser tabs; review page structure and escaping before adding actions.

### Phase 2 — Playing
**W4. Build-up actions.** Upgrade, trade and ready forms and routes, shown per
`availableActions`; units upgraded this turn on the page; rejected actions re-render with
the message and status.

**W5. Battle.** Army form (a number field per creature in hand) and route; all-zero
commits nothing; malformed counts are a 400; battle log newest first with armies, scores,
casualties, VP gained and duels.

**W6. Full game and docs.** Seeded 3-player game played through the UI POSTs to
`GAME_OVER`; README "Play in the browser" section.

**Checkpoint B:** all 7 success criteria in SPEC-web-ui.md met, including a manual full
game in three tabs.

## Risks and mitigations
| Risk | Impact | Mitigation |
|---|---|---|
| Iteration 3 edits `Application.kt`, `build.gradle.kts` or `GameRoutes.kt` at the same time | Merge conflicts | Keep each shared-file edit to one or two lines (W1, W2); rebase onto Iteration 3 as tasks land |
| Iteration 3 changes `PlayerView` (e.g. adds fields) | UI misses new info | Pages render from named fields; new fields are added to the UI in a follow-up, not by changing the DTO from here |
| Seeded numbers shift with Iteration 3 rules | UI tests break for unrelated reasons | UI tests assert structure, links, statuses and escaping, never seeded strengths or hands |
| Form parsing throws on bad input (`valueOf`, `toInt`) | 500 instead of 400 | Parse through one helper that maps failures to a 400 with a message; a test per malformed field |
| Player names with spaces or `/` break URLs | Broken links | Encode path segments when building links; test a name with a space |
| Gradle needs `~/.gradle`, which the sandbox can't write | Agent-run builds fail | Agent runs Gradle outside the sandbox (prompted), or adjust via `/sandbox` |

## Open questions
None blocking. Auto-refresh and auth are deferred until after the first session with
friends (spec Open Questions).
