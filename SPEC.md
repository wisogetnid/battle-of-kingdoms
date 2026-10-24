# Spec: Battle of Kingdoms — Core Loop MVP (Stage 1)

Source: [docs/ideas/battle-of-kingdoms-core-loop.md](docs/ideas/battle-of-kingdoms-core-loop.md)

## Objective
Turn the existing 2-player engine into a playable **3–5 player** game of the core loop,
exposed as a JSON HTTP API that can be played locally with `.http` files / curl.

**Who:** the developer first (playing all seats over HTTP), then 3–5 friends through a
Stage 2 client (bot or web page — separate spec, not in scope here).

**Why:** to learn whether the build-up → blind-bid-for-the-artifact loop is fun, and to
make its numbers easy to tune.

**Done means:** a complete game — join, several turns of upgrades and battles, rollovers,
a winner — can be played start to finish over HTTP, and an automated seeded test does
exactly that.

### User stories
- As a host, I create a game for N (3–5) players and get a game id to share.
- As a player, I join a game by id with a unique name; the game starts when it is full.
- As a player, I see my own hand, everyone's VP / renown / readiness, the current pot,
  and the log of past battles — but never other players' hands or uncommitted armies.
- As a player, I upgrade Horde into Shields or Bows with my resources, then mark myself ready.
- As a player, I secretly commit any part of my creatures (including nothing) to the battle.
- As a player, I see the battle result once everyone has committed: all armies, strengths,
  winner (or tie), casualties, and VP / renown changes.
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

1. **Income** (start of every turn): each player receives
   `baseCreaturesPerTurn + renown` Horde and `resourcesPerTurn` resources.
   On turn 1, each player additionally receives `startingHorde` Horde.
2. **Build-up:** a player may upgrade any number of times, then marks ready.
   Once ready, a player can't upgrade again this turn.
3. **Artifact:** one artifact is added to the pot each turn (pot starts at 0, so a fresh
   pot is 1; after a tie it carries over and grows by 1 per turn).
4. **Battle:** every player secretly commits a subset of their creatures (may be empty).
   Nothing is revealed until all players have committed.
5. **Resolve:** see below.
6. **Game over** after the resolution in which any player reaches `artifactsToWin` VP.
   Only one player can win a battle, so the winner is unambiguous.

### Creatures and upgrades
| Creature | Cost (upgrade) | Attack | Traits |
|---|---|---|---|
| Horde | — (income only) | 1 | Infantry |
| Shield | 1 Horde + 1 Wood | 1, **2** if any *other* committed army in this battle contains a Ranged creature | Infantry |
| Bow | 1 Horde + 1 Iron | 2 | Infantry, Ranged |

An upgrade removes one Horde and one matching resource from the hand and adds the new
creature. Resources are drawn from an **infinite supply**: each draw is Wood or Iron with
50/50 probability from the game's seeded `Random`. Resources and creatures carry over
between turns; there is no hand limit. `Food` is not part of the MVP.

### Battle resolution
1. **Strength** of an army = sum of its creatures' attack (with Shield's conditional bonus).
2. **Winner:** the single player with the highest strength. If two or more share the
   highest strength — including everyone committing nothing — there is **no winner**.
   If exactly one player commits anything, they win.
3. **Pot:** the winner gains VP equal to the pot (all accumulated artifacts) and the pot
   resets to 0. With no winner, the pot rolls over.
4. **Casualties:** each player loses half of their committed creatures —
   the winner `floor(n / 2)`, every other player (all players if there's no winner)
   `ceil(n / 2)`. Casualties are picked uniformly at random from the committed creatures
   using the seeded `Random`. Survivors return to the hand.
5. **Renown:** every player who committed at least 1 creature gains `renownPerBattle`
   renown. Renown is permanent and takes effect in the next turn's income.
6. A **battle log entry** records every player's committed army, strength, casualties,
   VP and renown gained, the winner (or none), and the pot size.

### Configuration
All numbers live in one object; nothing in the rules above is hard-coded elsewhere.
```kotlin
data class GameConfig(
    val minPlayers: Int = 3,
    val maxPlayers: Int = 5,
    val startingHorde: Int = 3,
    val baseCreaturesPerTurn: Int = 1,
    val resourcesPerTurn: Int = 2,
    val renownPerBattle: Int = 1,
    val artifactsToWin: Int = 3,
)
```
A game is created with a `GameConfig` and a seed. The same config, seed, and sequence of
player actions always produce the same game.

## HTTP API
JSON in and out (Ktor + Jackson, already in the build). Player names are trusted — no auth.
Creature and resource names in JSON: `HORDE`, `SHIELD`, `BOW`, `WOOD`, `IRON`.

| Method & path | Body | Success | Purpose |
|---|---|---|---|
| `POST /games` | `{ "players": 4, "host": "anna", "seed": 42 }` (`seed` optional) | `201 { "gameId": "…" }` | Create a game; the host joins it |
| `POST /games/{id}/players` | `{ "name": "ben" }` | `201` | Join; the game starts when full |
| `GET /games/{id}?player={name}` | — | `200` player view (below) | Read the game as that player |
| `POST /games/{id}/players/{name}/upgrades` | `{ "into": "BOW" }` | `200` player view | Upgrade one Horde |
| `POST /games/{id}/players/{name}/ready` | — | `200` player view | Finish build-up |
| `POST /games/{id}/players/{name}/army` | `{ "HORDE": 2, "BOW": 1 }` (`{}` = commit nothing) | `200` player view | Commit to the battle |

**Player view** contains: game id, phase, turn number, pot, `artifactsToWin`; *my* hand
(counts per creature and resource), VP, renown, and whether I've acted this phase; for
every other player: name, VP, renown, hand size, and whether they've acted; the battle
log; and the winner once the game is over. Other players' hands and the current turn's
commits are never included.

**Errors** (JSON `{ "error": "…" }`):
- `404` — unknown game, or a player name that isn't in the game
- `409` — action not allowed now: wrong phase, already ready/committed, game full,
  duplicate name, game over
- `400` — invalid request: player count outside 3–5, blank name, upgrade without a Horde
  or the matching resource, committing more creatures than owned, unknown creature

Games live in memory; restarting the server loses them.

## Tech Stack
- Kotlin 2.1.0 (JVM), Gradle 8.10 wrapper
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
docs/ideas/                         → one-pagers
http/playtest.http                  → scripted local game for manual play
src/main/kotlin/org/battleofkingdoms/
  game/      → Game state, GameConfig, phases, turn & income logic
  battle/    → Army, multi-army Battle resolution, BattleResult / log entry
  cards/     → Card, creatures (Horde, Shield, Bow), resources (Wood, Iron), traits
  player/    → Player state (hand, VP, renown, acted flag)
  server/    → Ktor module, routes, request/response DTOs, GameServer registry
src/main/resources/                 → application.conf (module path fixed), logback.xml
src/test/kotlin/org/battleofkingdoms/
  battle/ cards/ game/              → unit tests per rule
  server/                           → API tests (testApplication)
  e2e/                              → full seeded multiplayer games over HTTP
```
Required fixes: `application.conf` and `build.gradle.kts` (`mainClass`) must point to
`org.battleofkingdoms.server`, where the code actually lives.

## Code Style
Kotlin official style (`kotlin.code.style=official`). Immutable data classes; state
changes return a new state via `copy(...)` instead of mutating `var`s; `when` expressions
over `if` chains; small private helpers named after the rule they implement.

The engine is **pure**: every action is a function from (state, action) to a new state
or a rule violation. Randomness only comes from the `Random` the game was created with.
```kotlin
fun Game.upgrade(playerName: String, into: CreatureType): Game {
    val player = player(playerName)
    val resource = into.upgradeResource ?: throw RuleViolation("$into can't be upgraded into")
    return when {
        phase != Phase.BUILD_UP -> throw WrongPhase(phase)
        player.ready -> throw AlreadyActed(playerName)
        !player.hand.has(HORDE) || !player.hand.has(resource) ->
            throw RuleViolation("$playerName needs 1 Horde and 1 $resource")
        else -> updatePlayer(player.copy(hand = player.hand - HORDE - resource + into))
    }
}
```
The routing layer maps `RuleViolation` → 400, `WrongPhase` / `AlreadyActed` → 409, unknown
game/player → 404. No game logic lives in routes.

## Testing Strategy
- **Unit tests** (JUnit 5, parameterized where a rule has cases): every row of the rules
  tables and every resolution step has at least one test — strength incl. Shield bonus,
  winner / tie / all-zero / single committer, pot rollover and payout, floor/ceil losses,
  random casualties with a fixed seed, renown gain and income, upgrades, game-over.
- **API tests** (`testApplication`): each endpoint's success path and each error status;
  hidden information never leaks into another player's view.
- **End-to-end** (seeded, over HTTP): one full 3-player game and one full 5-player game,
  each including at least one tie/rollover, played to a winner.
- Tests use a fixed seed; no test depends on unseeded randomness.
- Existing 2-player tests are **rewritten**, not kept: `BattleTests`,
  `EndToEnd2PlayerUnitTest`, and the TODO-string assertions in `ApplicationKtTests`.
  `ArmyTests` and `CardTests` are updated to the new costs and kept.

## Boundaries
- **Always:** run `./gradlew test` before every commit; route every number through
  `GameConfig`; take randomness only from the game's seeded `Random`; update this spec
  first when a rule changes.
- **Ask first:** adding dependencies; changing the rules or default numbers; bumping
  Gradle / Kotlin / Ktor / Java versions; deleting tests other than those listed above.
- **Never:** build anything from the one-pager's "Not Doing" list (marketplace, deeper
  upgrades, supporters, diplomacy, artifact powers, auth, persistence, deployment);
  disable or skip tests to get to green; commit secrets.

## Success Criteria
1. `mise install && ./gradlew build` succeeds on a fresh checkout on this machine.
2. `./gradlew run` starts the server on port 8080 (the module path is correct).
3. The seeded 3-player and 5-player end-to-end tests play complete games over HTTP, each
   with at least one rollover, and end in `GAME_OVER` with the correct winner.
4. Every rule in the "Game Rules" section has at least one passing unit test.
5. Running the same seed and actions twice produces identical battle logs.
6. No player view ever contains another player's hand or uncommitted army (asserted in
   API tests).
7. Changing any `GameConfig` value changes behavior without touching any other code.
8. `http/playtest.http` plays a 3-player game from creation through the first two battles.

## Open Questions
None that block Stage 1. Deferred to later specs: themed artifacts without leader
snowball; Stage 2 client (chat bot vs. web page).
