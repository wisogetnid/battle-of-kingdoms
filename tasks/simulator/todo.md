# Todo: Bot Simulator

Plan: [plan.md](plan.md) · Spec: [SPEC-simulator.md](../../SPEC-simulator.md)

Paths below are relative to `src/main/kotlin/org/battleofkingdoms/simulator/` and
`src/test/kotlin/org/battleofkingdoms/simulator/`.

## Phase 1 — Engine to report
- [ ] **S1. Contract and one-game loop**
  - Acceptance: `Strategy` and `BuildUpMove` as in the spec; `Views.kt` extension functions on `PlayerView` (`resources(type)`, `count(creature)`, `army()` = whole hand as an `Army`); `playGame(lineup: List<Pair<String, Strategy>>, gameSeed, engineSeed, botSeeds)` creates the game with `GameConfig()`, joins seats in lineup order (`<id>-<seat>` names), runs build-up / battle / claim until `GAME_OVER` or 100 turns, and returns a `GameRecord`; each strategy call gets a fresh `PlayerView.of(UUID(gameSeed, 0), game, seat)` and its own `Random(botSeed)`; a `GameException` from a move, or 50 build-up moves without `Ready`, throws `BotError` naming strategy id, game seed, turn and move
  - Verify: `mise run test`; `SimulationTests` with test-only strategies: a never-commit strategy makes the game unfinished at turn 100; an illegal-move strategy throws `BotError` with id, seed and turn in the message; a recording strategy's views equal `PlayerView.of` for its seat, and a seat committing after another sees no trace of that commit; same seeds ⇒ equal records
  - Files: `Strategy.kt`, `Views.kt`, `Simulation.kt`, `SimulationTests.kt`

- [ ] **S2. Build plans, tendencies, random and horde-rush**
  - Acceptance: `BuildPlan`, `Tendency` (cautious ⅓, balanced ½, aggressive all, rounded up, strongest first: tier 2, tier 1, Horde, ties in `CreatureType` order) and `PlannedStrategy` as in the spec; `oneArtifact(view)` = artifact while `me.artifacts` is empty, else VP; `RandomStrategy` (random affordable action, ready with 1-in-3 per step, random subset of the hand, coin-flip claim); `HordeRush` plan
  - Verify: `mise run test`; `TendencyTests`: shares on pools of 0, 1, 3, 7 units and the strongest-first order; `StrategyTests` parameterized over a list of strategies (here: `random`, `horde-rush` × 3): each plays 5 seeded games against `random` via `playGame` without throwing
  - Files: `BuildPlan.kt`, `strategies/RandomStrategy.kt`, `strategies/HordeRush.kt`, `TendencyTests.kt`, `StrategyTests.kt`

- [ ] **S3. Seeding, tables, registry and runs**
  - Acceptance: `Seeds.kt` derives game seeds from `Random(runSeed)` and table / engine / bot seeds (always 5 bot seeds) from `Random(gameSeed)`; the table draw takes the player count from `GameConfig().minPlayers..maxPlayers` and seats from the registry in declared order, redrawing all-same tables; fewer than two strategies is an error; `Strategies.kt` registers `random` plus `horde-rush` × 3 tendencies as in the spec's registry snippet; `Simulation.run(runSeed, games): List<GameRecord>` and `Simulation.play(gameSeed): GameRecord`; `StrategyTests` switches to the registry
  - Verify: `mise run test`; tests: a 20-game run equals the first 20 records of a 40-game run; `play(record.gameSeed)` equals each record of a run; 1,000 drawn tables contain no all-same table; the drawn tables of a run don't change when a strategy's behaviour does (registry with the same ids, different strategies)
  - Files: `Seeds.kt`, `Simulation.kt`, `Strategies.kt`, `SimulationTests.kt`, `StrategyTests.kt`

- [ ] **S4. Report and the simulate command**
  - Acceptance: `Report.render(records, header)` prints the header (commit, seed, games, player range, artifacts on), the finished / unfinished counts, average turns overall and per player count, the strategy, plan and tendency tables (seats, wins, win %, fair %, ratio, sorted by ratio, `◀` at ≥ 1.3 or ≤ 0.7, `random` only in the strategy table), and the artifact table (taken in, winner held it); unfinished games count toward no table but the counts; `Main.kt` parses `--games` (default 2000) and `--seed` (default 1) and rejects anything else with a usage line; the commit comes from `git rev-parse --short HEAD` plus `(dirty)` from `git status --porcelain`, `unknown` on failure; Gradle `simulate` (`JavaExec`, main class `org.battleofkingdoms.simulator.MainKt`) and a mise `simulate` task passing arguments through
  - Verify: `mise run test`; `ReportTests` from hand-built records: win %, fair % on mixed player counts, ratio, marker thresholds at exactly 1.3 and 0.7, plan / tendency sums, artifact percentages, unfinished games excluded; then manually `mise run simulate` twice (identical output) and `mise run simulate -- --games 200 --seed 7`
  - Files: `Report.kt`, `Main.kt`, `ReportTests.kt`, `build.gradle.kts`, `.mise.toml`

**Checkpoint A:** build green; a full report for 7 strategies, reproducible, arguments passed through; the developer reads one report. Unfinished games under 5 %, or raised with the developer.

## Phase 2 — The full strategy set
- [ ] **S5. `mono-line` and `tier-1-focus`**
  - Acceptance: `MonoLine(Line.BOW)`: upgrade Horde → Bow → Warbow whenever affordable (tier 2 first), trade every non-wood resource for Horde, whole army as pool, `oneArtifact`; `TierOneFocus`: upgrade every Horde it can into any tier-1 unit (from `affordableUpgrades`), never tier 2, never trades, whole army as pool, `oneArtifact`; both registered
  - Verify: `mise run test` (registry-wide `StrategyTests` cover them); a unit test per plan on a hand-built `PlayerView`: `MonoLine` never upgrades outside the Bow line, `TierOneFocus` never picks a tier-2 upgrade
  - Files: `strategies/MonoLine.kt`, `strategies/TierOneFocus.kt`, `Strategies.kt`, `StrategyTests.kt`

- [ ] **S6. `tier-2-focus` and `hoarder`**
  - Acceptance: `TierTwoFocus`: tier-2 upgrades first, then tier-1, never trades, pool = its tier-2 units only, `oneArtifact`; `Hoarder`: upgrades anything affordable, tier 2 first, transmutes a resource no affordable upgrade uses when it owns the Stone (once per turn, as the view allows), never trades, whole army as pool, always takes the artifact; both registered (16 strategies)
  - Verify: `mise run test`; unit tests on hand-built views: `TierTwoFocus` pool excludes tier-1 and Horde; `Hoarder` claims the artifact even when it holds others, transmutes only with the Stone and only an unusable resource
  - Files: `strategies/TierTwoFocus.kt`, `strategies/Hoarder.kt`, `Strategies.kt`, `StrategyTests.kt`

- [ ] **S7. Docs and wrap-up**
  - Acceptance: README gains a short "Simulating games" section (`mise run simulate`, the flags, how to read ratio and the marker, ±2 points of noise at 2,000 games); the default run takes under 30 s (time noted in the commit message); spec status set to delivered
  - Verify: `mise run build`; `time mise run simulate`
  - Files: `README.md`, `SPEC-simulator.md`

**Checkpoint B:** all 9 success criteria in SPEC-simulator.md met; 16 strategies in the report.
