# Spec: Battle of Kingdoms — Counter Triangle (Iteration 2)

Source: [docs/ideas/battle-of-kingdoms-core-loop.md](docs/ideas/battle-of-kingdoms-core-loop.md) ·
Beyond this iteration: [docs/ideas/full-game-vision.md](docs/ideas/full-game-vision.md)

**Status:** Stage 1 (the 3–5 player core loop over HTTP, tasks T1–T10) is delivered.
Iteration 2 changes the resources, the upgrades, and how battles are resolved. Everything
not mentioned under [Changes from Stage 1](#changes-from-stage-1) stays as it is.

## Objective
Make the build-up phase a real decision. In Stage 1 an upgrade adds at most +1 attack and the
Shield only matters against Bows, so the cheapest way to get stronger is to pile up Horde.
Iteration 2 replaces the two upgrades with a **counter triangle** of three units, adds a third
resource, **Essence**, and resolves battles as **duels between every pair of players**, so a
unit's bonus counts against the specific opponent it counters.

**Who:** the developer playing all seats over HTTP, then 3–5 friends.

**Why:** the first battle should be a surprise; after it, players remember what the others
fielded and adapt their armies against specific opponents. Going wide with many Horde must
stay a viable alternative to upgrading.

**Done means:** a complete game under the new rules can be played over HTTP from start to
finish, and seeded tests play full 3- and 5-player games with the counters in effect.

### User stories
- As a host, I create a game for N (3–5) players and get a game id to share.
- As a player, I join a game by id with a unique name; the game starts when it is full.
- As a player, I see my own hand, everyone's VP / renown / readiness, the current pot,
  and the log of past battles — but never other players' hands or uncommitted armies.
- As a player, I see which actions I can take right now.
- As a player, I upgrade Horde into Bows, Swords, or Mages with Wood, Iron, or Essence,
  then mark myself ready.
- As a player, I secretly commit any part of my creatures (including nothing) to the battle.
- As a player, I see the battle result once everyone has committed: all armies, every duel's
  strengths, everyone's score, the winner (or tie), casualties, and VP / renown changes —
  so I can plan my next army against what each opponent fielded.
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
3. **Victory point:** one victory point (VP) is added to the pot each turn (pot starts at 0,
   so a fresh pot is 1; after a tie it carries over and grows by 1 per turn).
4. **Battle:** every player secretly commits a subset of their creatures (may be empty).
   Nothing is revealed until all players have committed.
5. **Resolve:** see below.
6. **Game over** after the resolution in which any player reaches `victoryPointsToWin` VP.
   Only one player can win a battle, so the winner is unambiguous.

### Creatures and upgrades
Every creature has attack 1. Each upgraded creature **counters** one other upgraded creature
and gets `counterBonus` extra attack in a duel against an army that contains it.

| Creature | Cost (upgrade) | Attack | Counters |
|---|---|---|---|
| Horde | — (income only) | 1 | — (and is never countered) |
| Bow | 1 Horde + 1 Wood | 1, **+`counterBonus`** against an army with a Mage | Mage |
| Sword | 1 Horde + 1 Iron | 1, **+`counterBonus`** against an army with a Bow | Bow |
| Mage | 1 Horde + 1 Essence | 1, **+`counterBonus`** against an army with a Sword | Sword |

```
        Bow
      ▲     ╲
     ╱       ▼          A ──▶ B : A counters B
  Sword ◀─── Mage
```

An upgrade removes one Horde and one matching resource from the hand and adds the new
creature. Resources are drawn from an **infinite supply**: each draw is Wood, Iron, or Essence
with equal probability (1/3 each) from the game's seeded `Random`. Resources and creatures
carry over between turns; there is no hand limit.

### Battle resolution
1. **Duels:** every pair of players fights one duel — 3 duels with 3 players, 10 with 5.
   Every player takes part, including those who committed nothing: an empty army has
   strength 0 and gives its opponent no bonus.
2. **Duel strength** of my army against one opponent = the number of my creatures, plus
   `counterBonus` for **each** of my creatures whose countered creature appears at least once
   in that opponent's army. A bonus earned against one opponent doesn't apply in my other duels.
3. **Score:** in each duel, my margin = my duel strength − my opponent's duel strength.
   My score = the sum of my margins over all my duels. A decisive duel counts for more than a
   narrow one. Scores always add up to 0 across all players.
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
7. **Renown:** every player who committed at least 1 creature gains `renownPerBattle`
   renown. Renown is permanent and takes effect in the next turn's income.
8. A **battle log entry** records every player's committed army, score, casualties, VP and
   renown gained; both strengths of every duel; the winner (or none); and the pot size.

Without any counters, a player's score is `players × own army size − all armies' total size`,
so the largest army wins — going wide works. Counters shift the result at the edges.

**Worked example** (3 players, `counterBonus = 1`):

| | anna: 2 Bow, 1 Horde | ben: 2 Sword | cleo: 1 Mage, 2 Horde |
|---|---|---|---|
| anna vs ben | 3 (ben has no Mage) | **4** (Swords vs anna's Bows) | |
| anna vs cleo | **5** (Bows vs cleo's Mage) | | 3 (anna has no Sword) |
| ben vs cleo | | 2 (cleo has no Bow) | **4** (Mage vs ben's Swords) |
| **Score** | −1 + 2 = **+1** | +1 − 2 = **−1** | −2 + 2 = **0** |

anna wins the pot with the only positive score.

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
    val counterBonus: Int = 1,
    val victoryPointsToWin: Int = 3,
)
```
A game is created with a `GameConfig` and a seed. The same config, seed, and sequence of
player actions always produce the same game.

## HTTP API
JSON in and out (Ktor + Jackson, already in the build). Player names are trusted — no auth.
Creature and resource names in JSON: `HORDE`, `BOW`, `SWORD`, `MAGE`, `WOOD`, `IRON`,
`ESSENCE`.

| Method & path | Body | Success | Purpose |
|---|---|---|---|
| `POST /games` | `{ "players": 4, "host": "anna", "seed": 42 }` (`seed` optional) | `201 { "gameId": "…" }` | Create a game; the host joins it |
| `POST /games/{id}/players` | `{ "name": "ben" }` | `201` | Join; the game starts when full |
| `GET /games/{id}?player={name}` | — | `200` player view (below) | Read the game as that player |
| `POST /games/{id}/players/{name}/upgrades` | `{ "into": "MAGE" }` | `200` player view | Upgrade one Horde |
| `POST /games/{id}/players/{name}/ready` | — | `200` player view | Finish build-up |
| `POST /games/{id}/players/{name}/army` | `{ "HORDE": 2, "SWORD": 1 }` (`{}` = commit nothing) | `200` player view | Commit to the battle |

**Player view** contains: game id, phase, turn number, pot, `victoryPointsToWin`; *my* hand
(counts per creature and resource), VP, renown, and whether I've acted this phase; for
every other player: name, VP, renown, hand size, and whether they've acted; the battle
log; the winner once the game is over; and `availableActions`, the actions I can take
right now: `UPGRADE` and `READY` during build-up until I'm ready, `COMMIT_ARMY` during the
battle until I've committed, otherwise none. `UPGRADE` is listed by phase only, even if I
can't currently afford one. Other players' hands and the current turn's commits are never
included.

**Battle log entry** in the player view:
```json
{
  "pot": 1,
  "winner": "anna",
  "armies": {
    "anna": { "army": { "BOW": 2, "HORDE": 1 }, "score": 1, "casualties": { "BOW": 1 },
              "survivors": { "BOW": 1, "HORDE": 1 }, "vpGained": 1, "renownGained": true }
  },
  "duels": [
    { "players": ["anna", "ben"], "strengths": { "anna": 3, "ben": 4 } }
  ]
}
```
(`armies` lists every player; `duels` lists every pair, in join order.)

**Errors** (JSON `{ "error": "…" }`):
- `404` — unknown game, or a player name that isn't in the game
- `409` — action not allowed now: wrong phase, already ready/committed, game full,
  duplicate name, game over
- `400` — invalid request: player count outside 3–5, blank name, upgrade without a Horde
  or the matching resource, committing more creatures than owned, unknown creature
  (including the retired `SHIELD`)

Games live in memory; restarting the server loses them.

## Changes from Stage 1
| Area | Stage 1 | Iteration 2 |
|---|---|---|
| Resources | Wood, Iron; draws 50/50 | Wood, Iron, **Essence**; draws 1/3 each |
| Creatures | Horde 1; Shield 1 (2 vs Ranged); Bow 2 | Horde, Bow, Sword, Mage all 1; +`counterBonus` per counter |
| Upgrade costs | Shield = Wood, Bow = Iron | Bow = Wood, Sword = Iron, Mage = Essence |
| Traits | Infantry, Ranged | Removed; the counter relation replaces them |
| Strength | one strength per army against all others | one duel strength per opponent |
| Winner | highest total strength | highest score (sum of duel margins) |
| Naming | artifact, `artifactsToWin` | victory point, `victoryPointsToWin` |
| Config | — | new `counterBonus` |
| Battle log | strength per army | score per army, plus every duel's strengths |

Seeded resource draws change with the third resource, so every seeded expectation (engine
full-game test, end-to-end tests, `http/playtest.http`) is re-derived, not patched.

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
  Jackson. Every rule lives here, including the counter relation and duel scoring.
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
  game/      → Game state, GameConfig, phases, turn & income logic
  battle/    → Army, duel strength, round-robin Battle resolution, BattleResult / log entry
  cards/     → Card, creatures (Horde, Bow, Sword, Mage) and their counters, resources (Wood, Iron, Essence)
  player/    → Player state (hand, VP, renown, acted flag)
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
fun upgrade(playerName: String, into: CreatureType): Game {
    val player = player(playerName)
    val resource = into.upgradeResource ?: throw RuleViolation("Nothing can be upgraded into $into")
    return when {
        phase != BUILD_UP -> throw WrongPhase(phase)
        player.acted -> throw AlreadyActed(playerName)
        !player.hand.has(HORDE) || !player.hand.has(resource) ->
            throw RuleViolation("$playerName needs 1 Horde and 1 $resource to upgrade into $into")
        else -> updatePlayer(player.copy(hand = player.hand - HORDE - resource + into))
    }
}
```
The routing layer maps `RuleViolation` → 400, `WrongPhase` / `AlreadyActed` /
`DuplicatePlayer` → 409, unknown game/player → 404. No game logic lives in routes.

## Testing Strategy
- **Unit tests** (JUnit 5, parameterized where a rule has cases): every row of the rules
  tables and every resolution step has at least one test — each upgrade and its cost, each
  of the three counter pairs, Horde never countered, one bonus per countering creature
  (not per countered one), bonuses applying only in the duel against the countered
  opponent, duel margins summed into scores, scores adding up to 0, winner / tie / all-zero /
  single committer, pot rollover and payout, floor/ceil losses, random casualties with a
  fixed seed, renown gain and income, the 1/3 resource draw, game-over.
- **The worked example** above is a unit test with exactly those duel strengths and scores.
- **Going wide:** a test where an all-Horde army wins against upgraded armies that don't
  counter each other enough to overcome its size.
- **API tests** (`testApplication`): each endpoint's success path and each error status;
  `SHIELD` is rejected as an unknown creature; the battle log shows scores and duels;
  hidden information never leaks into another player's view; `availableActions` matches
  the phase and whether the player has acted.
- **End-to-end** (seeded, over HTTP): one full 3-player game and one full 5-player game,
  each including at least one tie/rollover and at least one battle decided by a counter,
  played to a winner.
- Tests use a fixed seed; no test depends on unseeded randomness.
- Stage 1 tests that encode the old rules (Shield, Bow attack 2, Ranged trait, 50/50 draws,
  `artifactsToWin`) are **rewritten** to the new rules, not deleted.

## Boundaries
- **Always:** run `./gradlew test` before every commit; route every number through
  `GameConfig`; take randomness only from the game's seeded `Random`; update this spec
  first when a rule changes.
- **Ask first:** adding dependencies; changing the rules or default numbers; bumping
  Gradle / Kotlin / Ktor / Java versions; deleting tests rather than rewriting them.
- **Never:** build anything from the one-pager's "Not Doing" list (marketplace, upgrades
  beyond the single Horde → Bow / Sword / Mage tier, supporters, diplomacy, artifact powers,
  auth, persistence, deployment); let more than one player score in a battle; raise base
  attacks above 1; disable or skip tests to get to green; commit secrets.

## Success Criteria
1. `mise run build` passes with every test green.
2. Every rule in the "Game Rules" section has at least one passing unit test, including the
   worked example and the going-wide case.
3. The seeded 3-player and 5-player end-to-end tests play complete games over HTTP, each
   with at least one rollover and one battle won through a counter, and end in `GAME_OVER`
   with the correct winner.
4. Running the same seed and actions twice produces identical battle logs.
5. No player view ever contains another player's hand or uncommitted army (asserted in
   API tests).
6. Changing any `GameConfig` value, including `counterBonus`, changes behavior without
   touching any other code.
7. `http/playtest.http` plays a 3-player game through two battles, one of which is
   decided by a counter.
8. Every player view lists exactly the actions that player can take right now.
9. No `SHIELD`, `Trait`, `RANGED`, or `artifactsToWin` remains in code, API, or README.

## Open Questions
None that block this iteration. To watch in play: whether all-Horde armies dominate or
never win; whether the first battle feels like a fair surprise; whether duel margins make
one big army too strong in 5-player games (tunable via `counterBonus`). Deferred to later
specs: themed artifacts without leader snowball; Stage 2 client (chat bot vs. web page).
