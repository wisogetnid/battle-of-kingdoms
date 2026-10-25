# Spec: Battle of Kingdoms — Two Tiers and Trading (Iteration 3)

Source: [docs/ideas/battle-of-kingdoms-core-loop.md](docs/ideas/battle-of-kingdoms-core-loop.md) ·
Beyond this iteration: [docs/ideas/full-game-vision.md](docs/ideas/full-game-vision.md)

**Status:** Stage 1 (the 3–5 player core loop over HTTP) and Iteration 2 (the counter
triangle, duels and scores) are delivered. Iteration 3 rebalances the build-up: resources can
be traded for Horde, every upgraded unit gets a second tier, bonuses depend on tiers, renown
is removed, and a game runs to 5 VP. Everything not mentioned under
[Changes from Iteration 2](#changes-from-iteration-2) stays as it is.

## Objective
Give the build-up phase three real strategies that trade off against each other:
- **Go wide:** trade resources for Horde. Cheap strength and cheap casualties, but no
  resources left for upgrades.
- **Upgrade:** fewer, stronger units with big bonuses, but every random casualty costs more.
- **Plan for tier 2:** keep resources and a tier-1 unit across a turn to build a stronger
  unit next turn, at the cost of strength now.

**Who:** the developer playing all seats over HTTP, then 3–5 friends.

**Why:** in Iteration 2 renown compounds Horde income, so whoever gets ahead on army size
stays ahead, and upgrades are a small edge. Iteration 3 removes the snowball and makes the
choice of what to do with each resource matter. 5 VP gives the game enough turns for tier 2
and multi-turn plans to show.

**Done means:** a complete game under the new rules can be played over HTTP from start to
finish, and seeded tests play full 3- and 5-player games with trades and tier-2 units.

### User stories
- As a host, I create a game for N (3–5) players and get a game id to share.
- As a player, I join a game by id with a unique name; the game starts when it is full.
- As a player, I see my own hand (including which units I upgraded this turn), everyone's
  VP and readiness, the current pot, and the log of past battles — but never other
  players' hands or uncommitted armies.
- As a player, I see which actions I can take right now.
- As a player, I trade resources for Horde, upgrade Horde into tier-1 units and tier-1 units
  I've held since an earlier turn into tier-2 units, then mark myself ready.
- As a player, I secretly commit any part of my creatures (including nothing) to the battle.
- As a player, I see the battle result once everyone has committed: all armies, every duel's
  strengths, everyone's score, the winner (or tie), casualties, and VP changes.
- As a player, I see when the game is over and who won.

## Game Rules

### Turn structure
```
WAITING_FOR_PLAYERS ──(game full)──▶ BUILD_UP ──(all ready)──▶ BATTLE ──(all committed)──▶ resolve
                                       ▲                                                    │
                                       └──────────────── next turn ◀────── no winner yet ───┤
                                                                                            ▼
                                                                                        GAME_OVER
```

1. **Income** (start of every turn): each player receives `baseCreaturesPerTurn` Horde and
   `resourcesPerTurn` resources. On turn 1, each player additionally receives
   `startingHorde` Horde (0 by default, so turn 1 deals 1 Horde and 2 resources). Income is
   the same for every player.
2. **Build-up:** a player may trade and upgrade any number of times, in any order, then marks
   ready. Once ready, a player can't trade or upgrade again this turn.
3. **Victory point:** one victory point (VP) is added to the pot each turn (pot starts at 0,
   so a fresh pot is 1; after a tie it carries over and grows by 1 per turn).
4. **Battle:** every player secretly commits a subset of their creatures (may be empty).
   Nothing is revealed until all players have committed.
5. **Resolve:** see below.
6. **Game over** after the resolution in which any player reaches `victoryPointsToWin` VP.
   Only one player can win a battle, so the winner is unambiguous.

### Trading
A **trade** removes one resource of any kind from the hand and adds one Horde. A player may
trade any number of times during build-up, before marking ready.

### Creatures and upgrades
Every creature has attack 1. Upgraded creatures come in three **lines**, each with two
tiers. The lines form the counter triangle from Iteration 2: Bow counters Mage, Sword
counters Bow, Mage counters Sword.

| Line | Tier 1 (cost) | Tier 2 (cost) | Counters line |
|---|---|---|---|
| Bow | **Bow** (1 Horde + 1 Wood) | **Warbow** (1 Bow + 1 Wood) | Mage |
| Sword | **Sword** (1 Horde + 1 Iron) | **Knight** (1 Sword + 1 Iron) | Bow |
| Mage | **Mage** (1 Horde + 1 Essence) | **High Mage** (1 Mage + 1 Essence) | Sword |

An upgrade removes the unit it upgrades from and one matching resource from the hand and
adds the new creature. **A unit climbs at most one tier per turn:** a tier-2 upgrade needs a
tier-1 unit of that line that the player held at the start of this turn, not one they
upgraded into this turn. The player view shows which units were upgraded this turn.

Resources are drawn from an **infinite supply**: each draw is Wood, Iron, or Essence with
equal probability (1/3 each) from the game's seeded `Random`. Resources and creatures carry
over between turns; there is no hand limit.

### Bonuses
In a duel, each creature's strength is its attack (1) plus the **single best** bonus it has
against any creature in that opponent's army. Bonuses never add up.

| Attacker | vs Horde | vs its countered line, tier 1 | vs its countered line, tier 2 |
|---|---|---|---|
| Horde | — | — | — |
| Tier 1 (Bow, Sword, Mage) | **+`tier1VsHorde`** (2) | **+`sameTierCounter`** (1) | — (lower tier gets nothing against higher) |
| Tier 2 (Warbow, Knight, High Mage) | **+`tier2VsHorde`** (3) | **+`higherTierCounter`** (2) | **+`sameTierCounter`** (1) |

Any matchup not in the table (e.g. a Bow against a Sword, or a Warbow against a Bow) gives
no bonus. For example, a Warbow in a duel against an army with a Mage and a Horde gets
max(+2, +3) = +3, so its strength is 4.

### Battle resolution
1. **Duels:** every pair of players fights one duel — 3 duels with 3 players, 10 with 5.
   Every player takes part, including those who committed nothing: an empty army has
   strength 0 and gives its opponent no bonus.
2. **Duel strength** of my army against one opponent = the sum of my creatures' strengths
   (attack + best bonus) against that opponent's army. A bonus against one opponent doesn't
   apply in my other duels.
3. **Score:** in each duel, my margin = my duel strength − my opponent's duel strength.
   My score = the sum of my margins over all my duels. Scores always add up to 0.
4. **Winner:** the single player with the highest score. If two or more share the highest
   score — including everyone at 0, e.g. when nobody commits anything — there is **no winner**.
   Because scores add up to 0, a single highest score is always positive. If exactly one
   player commits anything, they win.
5. **Pot:** the winner gains VP equal to the pot and the pot resets to 0. With no winner, the
   pot rolls over.
6. **Casualties** are worked out once, after all duels: each player loses half of their
   committed creatures — the winner `floor(n / 2)`, every other player (all players if
   there's no winner) `ceil(n / 2)`. Casualties are picked uniformly at random from the
   committed creatures using the seeded `Random`. Survivors return to the hand.
7. A **battle log entry** records every player's committed army, score, casualties and VP
   gained; both strengths of every duel; the winner (or none); and the pot size.

**Worked example** (3 players, default config):

| | anna: 1 Warbow, 2 Horde | ben: 2 Mage, 1 Horde | cleo: 1 Knight, 1 Bow |
|---|---|---|---|
| anna vs ben | 6 = Warbow 4 (+3 vs Horde beats +2 vs Mage) + 2 Horde | **7** = 2 Mage × 3 (+2 vs Horde) + 1 Horde | |
| anna vs cleo | 3 = Warbow 1 (no Horde, no Mage) + 2 Horde | | **7** = Knight 4 (+3 vs Horde beats +1 vs Warbow) + Bow 3 (+2 vs Horde) |
| ben vs cleo | | 3 = 2 Mage × 1 (nothing vs a higher-tier Knight) + 1 Horde | **7** = Knight 4 (+3 vs Horde) + Bow 3 (+2 vs Horde beats +1 vs Mage) |
| **Score** | −1 − 4 = **−5** | +1 − 4 = **−3** | +4 + 4 = **+8** |

cleo wins: her army has no Horde, so neither opponent gets a Horde bonus against her, while
both of theirs contain Horde.

### Configuration
All numbers live in one object; nothing in the rules above is hard-coded elsewhere.
```kotlin
data class GameConfig(
    val minPlayers: Int = 3,
    val maxPlayers: Int = 5,
    val startingHorde: Int = 0,
    val baseCreaturesPerTurn: Int = 1,
    val resourcesPerTurn: Int = 2,
    val tier1VsHorde: Int = 2,
    val tier2VsHorde: Int = 3,
    val sameTierCounter: Int = 1,
    val higherTierCounter: Int = 2,
    val victoryPointsToWin: Int = 5,
)
```
A game is created with a `GameConfig` and a seed. The same config, seed, and sequence of
player actions always produce the same game.

## HTTP API
JSON in and out (Ktor + Jackson, already in the build). Player names are trusted — no auth.
Creature and resource names in JSON: `HORDE`, `BOW`, `SWORD`, `MAGE`, `WARBOW`, `KNIGHT`,
`HIGH_MAGE`, `WOOD`, `IRON`, `ESSENCE`.

| Method & path | Body | Success | Purpose |
|---|---|---|---|
| `POST /games` | `{ "players": 4, "host": "anna", "seed": 42 }` (`seed` optional) | `201 { "gameId": "…" }` | Create a game; the host joins it |
| `POST /games/{id}/players` | `{ "name": "ben" }` | `201` | Join; the game starts when full |
| `GET /games/{id}?player={name}` | — | `200` player view (below) | Read the game as that player |
| `POST /games/{id}/players/{name}/trades` | `{ "resource": "IRON" }` | `200` player view | Trade one resource for one Horde |
| `POST /games/{id}/players/{name}/upgrades` | `{ "into": "WARBOW" }` | `200` player view | Upgrade one unit by one tier |
| `POST /games/{id}/players/{name}/ready` | — | `200` player view | Finish build-up |
| `POST /games/{id}/players/{name}/army` | `{ "HORDE": 2, "KNIGHT": 1 }` (`{}` = commit nothing) | `200` player view | Commit to the battle |

The upgrade's `into` names the target; the unit it upgrades from follows from the table
(Horde for tier 1, the tier-1 unit of the same line for tier 2).

**Player view** contains: game id, phase, turn number, pot, `victoryPointsToWin`; *my* hand
(counts per creature and resource), `upgradedThisTurn` (counts per creature I upgraded into
this turn), VP, and whether I've acted this phase; for every other player: name, VP, hand
size, and whether they've acted; the battle log; the winner once the game is over; and
`availableActions`, the actions I can take right now: `TRADE`, `UPGRADE` and `READY` during
build-up until I'm ready, `COMMIT_ARMY` during the battle until I've committed, otherwise
none. `TRADE` and `UPGRADE` are listed by phase only, even if I can't currently afford one.
Other players' hands, their `upgradedThisTurn`, and the current turn's commits are never
included.

**Battle log entry** in the player view:
```json
{
  "pot": 1,
  "winner": "cleo",
  "armies": {
    "cleo": { "army": { "BOW": 1, "KNIGHT": 1 }, "score": 8, "casualties": { "BOW": 1 },
              "survivors": { "KNIGHT": 1 }, "vpGained": 1 }
  },
  "duels": [
    { "players": ["anna", "ben"], "strengths": { "anna": 6, "ben": 7 } }
  ]
}
```
(`armies` lists every player; `duels` lists every pair, in join order.)

**Errors** (JSON `{ "error": "…" }`):
- `404` — unknown game, or a player name that isn't in the game
- `409` — action not allowed now: wrong phase, already ready/committed, game full,
  duplicate name, game over
- `400` — invalid request: player count outside 3–5, blank name, trading a resource I don't
  have or something that isn't a resource, upgrading without the unit or resource it needs,
  upgrading a tier-1 unit I upgraded into this turn, upgrading into a Horde, committing more
  creatures than owned, unknown creature or resource

Games live in memory; restarting the server loses them.

## Changes from Iteration 2
| Area | Iteration 2 | Iteration 3 |
|---|---|---|
| Income | 1 Horde + 1 per renown, 2 resources; 3 extra Horde on turn 1 | 1 Horde, 2 resources, the same for everyone; no extra Horde on turn 1 (`startingHorde = 0`) |
| Renown | +1 per battle fought, permanent extra income | **Removed** (`Player.renown`, `renownPerBattle`, `gainedRenown`, view fields) |
| Trading | — | 1 resource → 1 Horde during build-up; new route and `TRADE` action |
| Creatures | Horde, Bow, Sword, Mage | + Warbow, Knight, High Mage (tier 2, upgraded from tier 1) |
| Upgrade limit | — | one tier per unit per turn; `upgradedThisTurn` in the view |
| Bonuses | +`counterBonus` if the opponent has the countered unit | single best bonus from the tier table; bonuses against Horde |
| Config | `counterBonus`, `renownPerBattle`, `victoryPointsToWin = 3` | `tier1VsHorde`, `tier2VsHorde`, `sameTierCounter`, `higherTierCounter`, `victoryPointsToWin = 5` |

Seeded expectations change with the new rules, so every seeded test, the end-to-end games,
and `http/playtest.http` are re-derived, not patched.

## Architecture
Three layers; dependencies point downwards only.
```
Presentation  .http files, curl, e2e tests, Stage 2 client — stateless, render the player view
     │
Application   server/  Ktor routes, request/response DTOs, GameServer registry (Map<UUID, Game>)
     │
Domain        game/ battle/ cards/ player/  pure engine: rules, state, seeded randomness
```
- **Domain** knows nothing about HTTP, JSON, or Ktor: no imports from `server/`, Ktor, or
  Jackson. Every rule lives here, including trading, tiers, bonuses and duel scoring.
- **Application** looks up the game, calls one engine function per request, stores the
  result, and maps it to a DTO. It maps `GameException`s to status codes and holds no rules.
- **Responses are DTOs only.** Domain objects are never serialized directly. The player-view
  DTO is built per player, which is how hidden information stays hidden.
- **Presentation** holds no state; everything it shows comes from the player view.

## Tech Stack
- Kotlin 2.1.0 (JVM), Gradle 8.14.5 wrapper
- **Java 21 (Temurin), pinned with mise** in `.mise.toml`; mise also provides `JAVA_HOME`
- Ktor 3.0.3 (Netty, content negotiation, Jackson), Logback 1.5.15
- Tests: JUnit 5 (+ params) 5.11.3, kotlin-test, ktor-server-test-host
- No new dependencies

## Commands
```
Setup:   mise trust && mise install   # installs Java 21 from .mise.toml
Test:    mise run test                # ./gradlew test
Build:   mise run build               # ./gradlew build
Run:     mise run server              # ./gradlew run, serves on http://localhost:8080
Gradle:  mise run gradle -- <args>    # any other Gradle invocation
Play:    open http/playtest.http in IntelliJ and run the requests top to bottom
```

## Project Structure
```
.mise.toml                          → Java 21 pin
SPEC.md                             → this spec
docs/ideas/                         → one-pagers and the long-term vision
http/playtest.http                  → scripted local game for manual play
src/main/kotlin/org/battleofkingdoms/
  game/      → Game state, GameConfig, phases, turn, income, trading and upgrade rules
  battle/    → Army, duel strength and bonuses, round-robin Battle resolution, BattleResult / log entry
  cards/     → Card, creatures (lines, tiers, counters), resources (Wood, Iron, Essence)
  player/    → Player state (hand, units upgraded this turn, VP, acted flag)
  server/    → Ktor module, routes, request/response DTOs, GameServer registry
src/main/resources/                 → application.conf, logback.xml
src/test/kotlin/org/battleofkingdoms/
  battle/ cards/ game/              → unit tests per rule
  server/                           → API tests (testApplication)
  e2e/                              → full seeded multiplayer games over HTTP
```

## Code Style
Kotlin official style (`kotlin.code.style=official`). Immutable data classes; state
changes return a new state via `copy(...)` instead of mutating `var`s; `when` expressions
over `if` chains; small private helpers named after the rule they implement.

The engine is **pure**: every action is a function from (state, action) to a new state
or a rule violation. Randomness only comes from the `Random` the game was created with.
```kotlin
fun trade(playerName: String, resource: ResourceType): Game {
    val player = player(playerName)
    return when {
        phase != BUILD_UP -> throw WrongPhase(phase)
        player.acted -> throw AlreadyActed(playerName)
        !player.hand.has(resource) -> throw RuleViolation("$playerName has no $resource to trade")
        else -> updatePlayer(player.copy(hand = player.hand - resource + HORDE))
    }
}
```
The routing layer maps `RuleViolation` → 400, `WrongPhase` / `AlreadyActed` /
`DuplicatePlayer` → 409, unknown game/player → 404. No game logic lives in routes.

## Testing Strategy
- **Unit tests** (JUnit 5, parameterized where a rule has cases): every row of the rules
  tables and every resolution step has at least one test — each trade, each tier-1 and
  tier-2 upgrade and its cost, the one-tier-per-turn limit (and that it resets next turn),
  every cell of the bonus table for every line, no bonus for lower against higher tier or
  outside the table, only the best bonus counting, bonuses applying only in the duel against
  that opponent, duel margins summed into scores that add up to 0, winner / tie / all-zero /
  single committer, pot rollover and payout, floor/ceil losses, random casualties with a
  fixed seed, equal income for everyone, the 1/3 resource draw, game-over at 5 VP.
- **The worked example** above is a unit test with exactly those duel strengths and scores.
- **API tests** (`testApplication`): each endpoint's success path and each error status,
  including the trade route; `upgradedThisTurn` appears only in my own view; no renown in any
  view; hidden information never leaks into another player's view; `availableActions`
  matches the phase and whether the player has acted.
- **End-to-end** (seeded, over HTTP): one full 3-player game and one full 5-player game, each
  including at least one rollover, at least one trade, and at least one tier-2 unit in a
  committed army, played to a winner at 5 VP.
- Tests use a fixed seed; no test depends on unseeded randomness.
- Iteration 2 tests that encode retired rules (renown, `counterBonus`, the flat counter
  bonus, 3 VP) are **rewritten** to the new rules, not deleted.

## Boundaries
- **Always:** run `./gradlew test` before every commit; route every number through
  `GameConfig`; take randomness only from the game's seeded `Random`; update this spec
  first when a rule changes.
- **Ask first:** adding dependencies; changing the rules or default numbers; bumping
  Gradle / Kotlin / Ktor / Java versions; deleting tests rather than rewriting them.
- **Never:** build anything from the one-pager's "Not Doing" list (marketplace, supporters,
  diplomacy, artifact powers, auth, persistence, deployment) or the out-of-scope items of
  this iteration: a third tier, trading resources for other resources or between players,
  changes to casualties, simulation tooling; let more than one player score in a battle;
  raise base attacks above 1; add bonuses together; disable or skip tests to get to green;
  commit secrets.

## Success Criteria
1. `mise run build` passes with every test green.
2. Every rule in the "Game Rules" section has at least one passing unit test, including every
   cell of the bonus table and the worked example.
3. The seeded 3-player and 5-player end-to-end tests play complete games over HTTP, each with
   at least one rollover, one trade and one tier-2 unit in a committed army, and end in
   `GAME_OVER` at 5 VP with the correct winner.
4. Running the same seed and actions twice produces identical battle logs.
5. No player view ever contains another player's hand, `upgradedThisTurn`, or uncommitted
   army (asserted in API tests).
6. Changing any `GameConfig` value, including each bonus value, changes behavior without
   touching any other code.
7. `http/playtest.http` plays a 3-player game through at least two battles, including a
   trade and a tier-2 unit in a committed army.
8. Every player view lists exactly the actions that player can take right now.
9. No `renown`, `counterBonus`, or `renownPerBattle` remains in code, API, or README.

## Open Questions
None that block this iteration. To watch in play: whether fielding Horde becomes too costly
now that every upgraded unit gets a big bonus against them (Horde as casualty fodder kept
at home vs. committed); whether going wide through trading is ever the best choice; whether
5 VP keeps a game under ~30 minutes. Deferred to later specs: themed artifacts without
leader snowball; Stage 2 client (chat bot vs. web page).
