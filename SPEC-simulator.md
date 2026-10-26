# Spec: Battle of Kingdoms — Bot Simulator

**Status:** Spec approved; plan and tasks in `tasks/simulator/` ([plan](tasks/simulator/plan.md) · [todo](tasks/simulator/todo.md)).

## Objective
A headless simulator, run on demand, that plays many seeded games between simple
rule-based bots and prints a balance report. It gives a *feeling* for overbearing
strategies or artifacts between rule changes, without needing 3–5 people at a table.

**Who:** the developer, after tweaking a rule or a `GameConfig` default.

**Why:** human playtests are slow and small, and the rules change every few commits.

**Done means:** `mise run simulate` plays 2,000 games in seconds and prints a report that
makes a dominant strategy, commit tendency or artifact obvious. A new build plan is one
small class plus one registry line, and it comes in all three commit tendencies.

### User stories
- As the developer, I run `mise run simulate` and read win rates per strategy, per build
  plan and per commit tendency against a fair baseline, how often winners held each artifact, and the average game length.
- As the developer, I change a rule, run again with the same seed, and compare the two
  reports. Each header tells me which commit (and whether the tree was dirty) produced it.
- As the developer, I add a strategy by writing one class and registering it, then rerun.

## Game setup
- **Tables:** every game has **4 players**. Each seat gets a strategy drawn uniformly from
  the registry, and a table is redrawn when
  - every seat has the same strategy, or
  - **two seats share a build plan**, whatever their tendencies. Two copies of a plan build
    alike, can tie each other in every battle and stall the game, which wouldn't happen
    between real players. `random` isn't a plan and may sit more than once.

  The simulator refuses to run with fewer than two strategies, or when the registry can't
  fill a table under these rules.
- **Config:** the default `GameConfig()`, artifacts on, exactly as in normal play.
- **Seats** are named after their strategy and seat number (`horde-rush-aggressive-1`, `hoarder-cautious-2`) and
  join in seat order, so join order, and with it the engine's resolution order, is random per table.
- **Seeding:** every random choice comes from one run seed (`--seed`, default `1`), split
  into independent streams. Two streams never share a seed:
  ```
  run seed ──Random(runSeed)──▶ game seed 0, 1, 2, …                (nextLong() per game)
  game seed ──Random(gameSeed)──▶ table seed · engine seed · bot seed per seat
  ```
  - The **table** stream draws the strategies, re-drawing tables the rules above forbid.
  - The **engine** seed goes to `Game.create`. It decides income and resource draws,
    casualties, artifact draws and transmutes.
  - Each **bot** gets its own `Random`, so one bot's choices never shift another bot's dice.
- **What that buys:**
  - Same code + same seed + same game count ⇒ byte-identical report.
  - `--games 4000` replays the 2,000 games of the default run and adds 2,000 more.
  - Different run seeds give unrelated games (seeds 1 and 2 don't share games).
  - Tables depend only on the seed and the registry (ids and plans), not on the rules. After a rule
    change, the same seed seats the **same line-ups**, so the comparison isn't skewed by
    who happened to sit together. The dice inside a game still diverge once a decision
    changes, because the engine draws randomness as actions happen. That can't be fixed
    without touching the engine, and it isn't needed for "a feeling".
- **Reproducing one game:** a bot error prints the game seed. `Simulation.play(gameSeed)`
  replays exactly that game, from a test or a scratch `main`.
- **Turn cap:** a game still running after 100 turns stops and counts as *unfinished*. It
  counts toward neither wins nor baselines and is reported separately.

## Bots
### Strategy interface
A strategy is a stateless decision function. The simulator calls it whenever its seat has
something to do. It never mutates the game itself.

```kotlin
package org.battleofkingdoms.simulator

/** One build-up step: a [Game] action, or [Ready] to end the build-up. */
sealed interface BuildUpMove {
    data class Trade(val resource: ResourceType) : BuildUpMove
    data class Upgrade(val into: CreatureType) : BuildUpMove
    data class Transmute(val resource: ResourceType) : BuildUpMove
    data object Ready : BuildUpMove
}

/** A bot. [view] is exactly what its seat would see over the API; [random] is its own. */
interface Strategy {
    /** Called repeatedly during build-up until it answers [BuildUpMove.Ready]. */
    fun buildUp(view: PlayerView, random: Random): BuildUpMove

    fun commit(view: PlayerView, random: Random): Army

    /** Only called on the battle's winner, while it chooses its prize (`view.me.drawnArtifact`). */
    fun claim(view: PlayerView, random: Random): Take
}
```

### What a bot sees
Bots see the **player view and nothing else**: the existing `PlayerView.of(gameId, game, seat)`
from `server/Dtos.kt`, the same object a human gets from `GET /games/{id}?player=…`, built
fresh before every call. So bots get the information humans get, by construction:
- other seats' hands only as `handSize`, the current turn's commits not at all, and the drawn
  artifact only for the winner while it chooses;
- no access to the engine's `Random`, so a bot can't disturb the game's dice. Bots draw
  from their own `random` parameter.
- `availableActions` and `affordableUpgrades` come with the view, so strategies don't
  re-implement the rules.

The simulator passes a fixed `gameId` derived from the game seed. The view's `hand` uses
string keys (`"HORDE"`, `"WOOD"`). A small helper in the simulator package turns it into
creature and resource counts for strategies.

### Game loop
The phases run in the engine's join order. Build-up: each seat calls `buildUp` until it
returns `Ready`, applying each move to the real `Game`. Battle: each seat commits. Claim: the
winner's `claim`. A `GameException` from a bot's move is a bug in that bot: the run aborts
with the strategy, game seed, turn and move in the message. There is no silent retry.
A build-up that hasn't answered `Ready` after 50 moves aborts the same way.

### Strategies = build plan × commit tendency
Deliberately crude: they're meant to probe the rules, not to play well. A strategy has two
parts: a **build plan** (what it does in build-up and how it claims) and a **commit
tendency** (how much of its army it sends to battle). Every plan is registered once per
tendency, so the id is `<plan>-<tendency>`, e.g. `tier-2-focus-cautious`.

#### Build plans
| plan | Build-up | Commit pool | Claim |
|---|---|---|---|
| `horde-rush` | trade every resource for Horde | whole army | one artifact |
| `mono-line` | Bow line only: upgrade Horde → Bow → Warbow whenever the wood allows, trade every other resource for Horde | whole army | one artifact |
| `tier-1-focus` | upgrade every Horde it can into a tier-1 unit of any line, never into tier 2; keep leftover resources for later turns | whole army | one artifact |
| `tier-2-focus` | tier-2 upgrades first, then tier-1 upgrades to feed next turn's tier 2; never trades | its tier-2 units only | one artifact |
| `hoarder` | upgrade anything affordable, tier 2 first; uses the Transmuter Stone on a resource it can't spend; never trades | whole army | **always artifact** |

- **one artifact:** take the first artifact offered, then always the VP. That keeps
  artifacts in circulation, so the artifact table isn't only about `hoarder`.
- **hoarder** is the artifact-focused plan. It takes every artifact it's offered and only
  scores VP once the stack is empty.
- There's only one mono-line plan: in a rock-paper-scissors triangle the three lines are
  symmetric, so they would measure the same thing.

#### Commit tendencies
The tendency takes a share of the plan's **commit pool**, rounded up, strongest units
first (tier 2, then tier 1, then Horde; ties in `CreatureType` order). The rest stays home.

| tendency | share of the pool |
|---|---|
| `cautious` | ⅓ |
| `balanced` | ½ |
| `aggressive` | all |

An empty pool commits nothing, and a pool of one unit always commits that unit.

#### The random baseline
`random` stands outside the grid and is registered once. Build-up: a random affordable
action (trade / upgrade / transmute), and each step it gets ready with a 1-in-3 chance.
Commit: a random subset of the hand. Claim: a coin flip.

#### How the parts fit
```kotlin
/** What a strategy does apart from how much it commits. */
interface BuildPlan {
    val id: String
    fun buildUp(view: PlayerView, random: Random): BuildUpMove
    /** The units this plan is willing to send; the [Tendency] picks how many of them go. */
    fun commitPool(view: PlayerView): Army
    fun claim(view: PlayerView, random: Random): Take
}

enum class Tendency(val id: String, val share: Double) {
    CAUTIOUS("cautious", 1.0 / 3), BALANCED("balanced", 0.5), AGGRESSIVE("aggressive", 1.0);
    fun pick(pool: Army): Army = TODO("ceil(share × pool.size), strongest first")
}

/** A [BuildPlan] with a [Tendency] is a [Strategy]. */
class PlannedStrategy(val plan: BuildPlan, val tendency: Tendency) : Strategy {
    override fun buildUp(view: PlayerView, random: Random) = plan.buildUp(view, random)
    override fun commit(view: PlayerView, random: Random) = tendency.pick(plan.commitPool(view))
    override fun claim(view: PlayerView, random: Random) = plan.claim(view, random)
}
```

### Registry
```kotlin
val strategies: Map<String, Strategy> =
    mapOf("random" to RandomStrategy) +
        listOf(
            HordeRush, MonoLine(Line.BOW), TierOneFocus, TierTwoFocus, Hoarder,
            // adding a build plan = one class + one entry here
        ).flatMap { plan ->
            Tendency.entries.map { "${plan.id}-${it.id}" to PlannedStrategy(plan, it) }
        }
```
That's 16 strategies. To keep each one's sample size reasonable, the default run is
2,000 games (≈ 500 seats each).

## Report
Printed to stdout as plain text, for example:

```
Battle of Kingdoms simulation
commit 32660fd (dirty) · seed 1 · 2000 games · 4 players · artifacts on
finished 1994 · unfinished 6 · avg 9.4 turns

strategy                 seats   wins   win %   fair %   ratio
horde-rush-aggressive     512     190   37.1     24.9    1.49  ◀
tier-2-focus-cautious     498     118   23.7     25.1    0.94
...

plan            seats   wins   win %   fair %   ratio
horde-rush       1530    ...
...

tendency        seats   wins   win %   fair %   ratio
aggressive       2541    ...
...

artifact          taken in   winner held it
HORN_OF_FRENZY      61 %          48 %
...
```

- **win %** = wins ÷ seats. **fair %** = average over the strategy's seats of
  1 ÷ that table's player count (what it would win if all strategies were equal; 25 % at
  4 players).
  **ratio** = win % ÷ fair %. The plan and tendency tables add up the seats of their
  strategies (`random` appears only in the strategy table). Rows are sorted by ratio. A `◀` marks ratio ≥ 1.3 or ≤ 0.7.
  This is a reading aid, not a pass/fail.
- **taken in** = share of finished games where someone took the artifact.
  **winner held it** = among those games, how often the game's winner owned it at the end.
  With one owner per game, a neutral artifact sits near 1 ÷ 4.
- **Commit:** `git rev-parse --short HEAD`, plus `(dirty)` when `git status --porcelain`
  is non-empty. It prints `unknown` when git isn't available, and the run still goes on.

## Tech Stack
Kotlin 2.1 on Java 21 (temurin via mise), Gradle wrapper, JUnit 5. No new dependencies.
Uses `kotlin.random.Random`, as the engine does.

## Commands
```
Simulate:  mise run simulate                       # 2000 games, seed 1
           mise run simulate -- --games 5000 --seed 42
Build:     mise run build
Test:      mise run test
```
`simulate` is a Gradle `JavaExec` task (`./gradlew -q simulate --args="…"`) with main class
`org.battleofkingdoms.simulator.MainKt`. The mise task wraps it.

## Project Structure
```
src/main/kotlin/org/battleofkingdoms/simulator/
  Main.kt          → argument parsing, git commit lookup, prints the report
  Strategy.kt      → Strategy, BuildUpMove
  Views.kt         → PlayerView helpers (hand as creature / resource counts)
  Seeds.kt         → the seed streams
  BuildPlan.kt     → BuildPlan, Tendency, PlannedStrategy (plan + tendency → Strategy)
  Strategies.kt    → the registry
  strategies/      → one file per build plan, plus RandomStrategy
  Simulation.kt    → table draw, game loop, turn cap; returns raw results per game
  Report.kt        → aggregates results into the printed report
src/test/kotlin/org/battleofkingdoms/simulator/
  SimulationTests.kt, ReportTests.kt, StrategyTests.kt
```
The simulator lives in `main`, not `test`: it's a tool, and it must not be run as part of
`mise run test`.

## Code Style
Same as the engine: immutable data, `when` expressions, small functions with KDoc only where
the *why* isn't obvious, kebab-case ids for strategies. Strategies decide from the view's
own lists (`availableActions`, `affordableUpgrades`, `me.hand`) and don't re-implement rules.

```kotlin
/** Trades every resource for Horde; its whole army is up for battle. */
object HordeRush : BuildPlan {
    override val id = "horde-rush"

    override fun buildUp(view: PlayerView, random: Random): BuildUpMove =
        ResourceType.entries.firstOrNull { view.resources(it) > 0 }
            ?.let { BuildUpMove.Trade(it) }
            ?: BuildUpMove.Ready

    override fun commitPool(view: PlayerView): Army = view.army()

    override fun claim(view: PlayerView, random: Random): Take = oneArtifact(view)
}
```

## Testing Strategy
JUnit 5 in `src/test/kotlin/org/battleofkingdoms/simulator/`, run by `mise run test`.
- **Seeding:** a 20-game run is the prefix of a 40-game run with the same seed;
  `Simulation.play(gameSeed)` reproduces a game from the run; changing a strategy's
  behaviour (same id and plan) leaves the drawn tables unchanged.
- **Tables:** always 4 seats, never all the same strategy, never one build plan twice.
- **Simulation:** a small run (e.g. 50 games) finishes, is deterministic (same seed ⇒
  identical results), never seats one strategy at every seat, and stops at the turn cap
  (tested with a strategy that never commits).
- **Player view only:** a strategy that records what it's given receives `PlayerView`s
  whose contents match `PlayerView.of` for its seat at that moment. In particular, a seat
  that commits after others sees no trace of their commits.
- **Bot errors:** a strategy that makes an illegal move aborts the run with its id, seed and turn.
- **Report:** win %, fair % and ratio computed from hand-built results; formatting of the
  header and the marker threshold.
- **Tendencies:** pool shares for cautious / balanced / aggressive, with rounding up,
  strongest-first order, an empty pool and a one-unit pool.
- **Strategies:** every registered strategy plays a full seeded game against `random`
  without throwing. No assertions on how well it plays.

## Boundaries
- **Always:** go through the public `Game` API; keep runs deterministic for a given seed;
  keep strategies stateless and build plans in their own files.
- **Ask first:** any change in `game/`, `battle/`, `cards/`, `player/`, `server/` or `web/`
  (using `PlayerView` from `server/` is fine; changing it is not); adding dependencies;
  changing report columns beyond this spec.
- **Never:** hand a strategy anything but its `PlayerView` and its own `Random`; put game
  rules in a strategy that the view could answer; run the simulator from the regular test
  suite or CI.

## Success Criteria
1. `mise run simulate` prints a report in the format above; with defaults it finishes in
   under 30 s on the developer's machine.
2. Running it twice with the same seed on the same commit produces byte-identical output.
3. Every simulated table has 4 seats, never the same strategy at every seat and never one
   build plan twice.
4. The header shows the short SHA and `(dirty)` for uncommitted changes.
5. All 16 strategies (5 plans × 3 tendencies + `random`) are registered and each one plays
   full games without errors.
   Strategies only ever receive a `PlayerView` and their own `Random`.
6. Adding a build plan touches exactly one new file and one registry entry, and yields
   its three tendency variants.
7. A bot's illegal move aborts with strategy id, game seed and turn; games over 100 turns
   are counted as unfinished.
8. `git diff` of the feature touches nothing in the engine, server or web packages
   (`build.gradle.kts` and `.mise.toml` gain the `simulate` task).
9. The report has strategy, plan and tendency tables, plus the artifact table.

## Open Questions
1. **Strategy filter** (`--strategies …`): left out for now and can be added later.
