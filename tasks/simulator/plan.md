# Plan: Bot Simulator

Spec: [SPEC-simulator.md](../../SPEC-simulator.md) · Tasks: [todo.md](todo.md)

All work happens in a new `simulator` package plus the `simulate` task in
`build.gradle.kts` and `.mise.toml`. No engine, server or web file changes. `PlayerView`
from `server/Dtos.kt` is used as is.

## Architecture decisions
1. **Pure core, IO at the edge.** `Simulation` plays games and returns one `GameRecord` per
   game: game seed, player count, seats with strategy ids, winner, turns, artifacts held
   per seat at the end, finished or unfinished. `Report` turns records into text. `Main`
   only parses arguments, looks up the git commit and prints the report. Each layer can be
   tested without the next one.
2. **Strategies only ever see `PlayerView.of(gameId, game, seat)`.** The loop builds a
   fresh view before every call, with `gameId = UUID(gameSeed, 0)`. Hand parsing (`"HORDE"`
   → `CreatureType.HORDE`) lives in `Views.kt` as extension functions (`resources`,
   `count`, `army`), so strategies never touch string keys.
3. **The loop maps moves onto the public `Game` API.** `BuildUpMove.Trade` → `trade`,
   `Upgrade` → `upgrade`, `Transmute` → `transmute`, `Ready` → `ready`. Then `commitArmy`
   and `claim`. Seats act in `game.players` order, which is join order. Any `GameException`
   becomes a `BotError(strategyId, gameSeed, turn, move)` that aborts the run.
4. **Seeding as specified, in one place.** `Seeds.kt` holds the stream derivation:
   `Random(runSeed)` yields game seeds, and `Random(gameSeed)` yields the table seed, the
   engine seed and one bot seed per seat, in that order. Bot seeds are drawn for the
   maximum player count, so the number of seats doesn't shift the other seeds. The table
   draw walks the registry in its declared order, so tables are stable as long as the
   registry is.
5. **Plan × tendency is a composition, not inheritance.** `BuildPlan` +
   `Tendency` → `PlannedStrategy : Strategy`. `RandomStrategy` implements `Strategy`
   directly. The registry is a `Map<String, Strategy>` built from a plan list, so a new
   plan needs one file plus one list entry.
6. **Shared plan helpers live next to `BuildPlan`.** For now that's `oneArtifact(view)` and
   "the strongest units first" ordering. They're only extracted when a second plan needs
   them, not before.
7. **Registry tests cover new plans automatically.** `StrategyTests` is parameterized over
   the registry, so each later plan is tested (full games without errors) just by being
   registered.

## Dependency graph
```
S1 contract + one-game loop (test strategies only)
 └─ S2 plan × tendency, random, horde-rush
     └─ S3 seeding, table draw, registry, runs
         └─ S4 report, main, simulate task          ── Checkpoint A
             ├─ S5 mono-line, tier-1-focus
             └─ S6 tier-2-focus, hoarder
                 └─ S7 README, timing check, spec status   ── Checkpoint B
```
S5 and S6 both edit `Strategies.kt`, so they run one after the other.

## Phases and checkpoints

### Phase 1 — Engine to report
**S1. Contract and one-game loop.** `Strategy`, `BuildUpMove`, view helpers, and
`playGame(lineup, engineSeed, botSeeds)` with the turn cap and `BotError`. It's tested
with strategies defined in the test: one that never commits (hits the cap), one that
makes an illegal move, one that records its views.

**S2. Build plans and tendencies.** `BuildPlan`, `Tendency`, `PlannedStrategy`,
`RandomStrategy`, `HordeRush`. It includes the tendency share tests and the parameterized
"plays full games" test.

**S3. Runs.** `Seeds`, the table draw with no all-same tables, the registry
(`random` + `horde-rush` × 3), `run(runSeed, games)` and `play(gameSeed)`. It's tested for
the prefix, reproduction and table-stability properties.

**S4. Report and command.** `Report` (three win tables, the artifact table, the header
with turns per player count), `Main` (`--games`, `--seed`, git SHA with `(dirty)`,
`unknown` fallback), the Gradle `JavaExec` task and the mise task.

**Checkpoint A:** `mise run build` green; `mise run simulate` prints a full report for the
7 strategies registered so far, twice with identical output; `mise run simulate -- --games
200 --seed 7` passes its arguments through. The developer reads one report for sense.

### Phase 2 — The full strategy set
**S5. `mono-line` and `tier-1-focus`.**

**S6. `tier-2-focus` and `hoarder`** (the hoarder's Transmuter Stone use included).

**S7. Docs and wrap-up.** A README section "Simulating games", a timing check of the
default run (< 30 s), and the spec status set to delivered.

**Checkpoint B:** all 9 success criteria in SPEC-simulator.md met; 16 strategies in the
report.

## Risks
- **Argument passing through mise → Gradle.** `mise run simulate -- --games 5000` has to
  reach `--args`. Verify in S4. If mise's trailing-args handling fights `--args="…"`, use
  a mise `usage` spec or a tiny wrapper script.
- **`PlayerView` cost.** It's built before every decision and maps the whole battle log.
  That's fine at ~10 turns per game, but if the default run is slow, cache one view per
  seat between build-up steps only. Never cache across moves of other seats.
- **Strategy loops in build-up.** A plan that keeps trading back and forth never gets
  ready. The 50-move limit turns that into a `BotError` instead of a hang.
- **Too many unfinished games.** `tier-2-focus-cautious` vs `tier-2-focus-cautious`-like
  tables might stall. Tables are never all the same strategy, but a mix of
  near-passive strategies could still hit the cap often. If more than 5 % of games end
  unfinished at Checkpoint A or B, raise it with the developer rather than tweaking
  strategies silently.
- **Hidden coupling to `PlayerView`.** If the view changes for the UI, strategies change
  too. That's intended, since bots should see what players see, but the `StrategyTests`
  suite is what catches a break.
