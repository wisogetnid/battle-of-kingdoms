# Spec: Battle of Kingdoms — Artifacts (Iteration 5)

Source: [docs/ideas/battle-of-kingdoms-core-loop.md](docs/ideas/battle-of-kingdoms-core-loop.md) ·
Beyond this iteration: [docs/ideas/full-game-vision.md](docs/ideas/full-game-vision.md) ·
Web UI: [SPEC-web-ui.md](SPEC-web-ui.md)

**Status:** Stage 1 (the 3–5 player core loop over HTTP), Iteration 2 (the counter triangle,
duels and scores), Iteration 3 (trading, two tiers, tier bonuses, no renown, no starting
Horde) and Iteration 4 (tier 2 +5 / +3, 3 VP) are delivered. Iteration 5 replaces the
rollover pot with **artifacts**: every battle is worth 1 VP, a tie gives nothing, and the
winner chooses between that VP and an artifact only they have seen. A playtest also
rebalanced the bonus table: tier 1 gets +1, tier 2 +4 against Horde and the tier-1 unit it
counters and +2 against other tier-1 units and the tier-2 unit it counters. Everything not
mentioned under [Changes from Iteration 4](#changes-from-iteration-4) stays as it is.

## Objective
Turn a battle win into a decision: **a point now, or a lasting advantage.** The winner looks
at one artifact drawn from a small face-down stack and takes either it or the VP. Artifacts
help their owner for the rest of the game but cost a step toward 3 VP, so they can't simply
snowball the leader. A declined artifact goes back in secret, so the winner learns
something about the stack that nobody else knows.

**Who:** the developer playing all seats over HTTP, then 3–5 friends.

**Why:** with 3 VP, a turn-1 tie rolled the pot over and made turn 2 worth two thirds of
the game, before any tier-2 unit could exist. Without a rollover every battle is worth the
same, and artifacts make winning more interesting than collecting a point.

**Scope rule:** start small to prove the mechanic. Exactly three artifacts, one of each, and
nothing that makes the game longer to play through than it needs to be.

**Done means:** a complete game with artifacts can be played over HTTP from start to
finish, and seeded tests play full 3- and 5-player games in which an artifact is taken, an
artifact is declined, and a player reaches 3 VP.

### User stories
- As a host, I create a game for N (3–5) players and get a game id to share.
- As a player, I join a game by id with a unique name; the game starts when it is full.
- As a player, I see my own hand (including which units I upgraded this turn), everyone's
  VP, readiness and artifacts, how many artifacts are left in the stack, and the log of past
  battles — but never other players' hands, uncommitted armies, or an artifact someone
  else drew.
- As a player, I see which actions I can take right now.
- As a player, I trade resources for Horde, upgrade Horde into tier-1 units and tier-1 units
  I've held since an earlier turn into tier-2 units, then mark myself ready.
- As a player, I secretly commit any part of my creatures (including nothing) to the battle.
- As a player, I see the battle result once everyone has committed: all armies, every duel's
  strengths, everyone's score, the winner (or tie), and casualties.
- As the winner of a battle, I see the drawn artifact and take either it or the VP.
- As a player, I see what each winner took: the VP, or which artifact.
- As a player, I see when the game is over and who won.

## Game Rules

### Turn structure
```
WAITING_FOR_PLAYERS ──(game full)──▶ BUILD_UP ──(all ready)──▶ BATTLE ──(all committed)──▶ resolve
                                       ▲                                                    │
                                       │                    winner, stack not empty         │
                                       │                  ┌──────────────────────────────── ┤
                                       │                  ▼                                 │
                                       │                CLAIM ──(winner takes a prize)──▶ award
                                       │                                                    │
                                       └────────── next turn ◀─── tie, or nobody at 3 VP ───┤
                                                                                            ▼
                                                                                        GAME_OVER
```

1. **Income** (start of every turn): each player receives `baseCreaturesPerTurn` Horde and
   `resourcesPerTurn` resources, plus `crownHorde` Horde if they own the Crown. On turn 1,
   each player additionally receives `startingHorde` Horde (0 by default, so turn 1 deals
   1 Horde and 2 resources).
2. **Build-up:** a player may trade and upgrade any number of times, in any order, then marks
   ready. Once ready, a player can't trade or upgrade again this turn.
3. **Battle:** every player secretly commits a subset of their creatures (may be empty).
   Nothing is revealed until all players have committed.
4. **Resolve:** see [Battle resolution](#battle-resolution).
5. **Claim** (only if the battle has a winner and the artifact stack isn't empty): see
   [Prize and artifacts](#prize-and-artifacts). Everyone else waits.
6. **Game over** when a player reaches `victoryPointsToWin` VP. Only one player can win a
   battle and only a VP prize scores, so the winner is unambiguous.

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

| Attacker | vs Horde | vs the tier-1 unit it counters | vs any other tier-1 unit | vs the tier-2 unit it counters |
|---|---|---|---|---|
| Horde | — | — | — | — |
| Tier 1 (Bow, Sword, Mage) | **+`tier1VsHorde`** (1) | **+`tier1VsCountered`** (1) | — | — (lower tier gets nothing against higher) |
| Tier 2 (Warbow, Knight, High Mage) | **+`tier2VsHorde`** (4) | **+`tier2VsCounteredTier1`** (4) | **+`tier2VsOtherTier1`** (2) | **+`tier2VsCounteredTier2`** (2) |

"Any other tier-1 unit" includes the tier-1 unit of the attacker's own line (a Warbow gets
+2 against a Bow). Any matchup not in the table (e.g. a Bow against a Sword, or a Warbow
against a Knight) gives no bonus. For example, a Warbow in a duel against an army with a
Bow and a Horde gets max(+2, +4) = +4, so its strength is 5; against an army with only
Swords it gets +2, so its strength is 3. Artifacts never change duel strength.

### Battle resolution
1. **Duels:** only players who committed at least one creature take part. Every pair of
   them fights one duel — 3 duels with 3 committed armies, 10 with 5. A player who committed
   nothing **sits out**: no duels, score 0, no casualties, and nobody scores against them.
2. **Duel strength** of my army against one opponent = the sum of my creatures' strengths
   (attack + best bonus) against that opponent's army. A bonus against one opponent doesn't
   apply in my other duels.
3. **Score:** in each duel, my margin = my duel strength − my opponent's duel strength.
   My score = the sum of my margins over all my duels (0 if I sat out). Scores always add
   up to 0.
4. **Winner:** if nobody commits anything, there is **no winner**. If exactly one player
   commits anything, they **win uncontested** (no duels, score 0). Otherwise the winner is
   the single committed player with the highest score; if two or more share it, there is no
   winner. Because the scores of the committed players add up to 0, a single highest score
   among two or more of them is always positive.
5. **No winner:** nobody gains anything, no artifact is drawn, and the next battle is again
   worth `victoryPointsPerBattle` (1). Nothing rolls over.
6. **Casualties** are worked out once, after all duels: each player loses half of their
   committed creatures — the winner `floor(n / 2)`, every other player (all players if
   there's no winner) `ceil(n / 2)` — adjusted by the artifacts they own (below).
   Casualties are picked uniformly at random from the committed creatures using the seeded
   `Random`. Survivors return to the hand.
7. A **battle log entry** records every player's committed army, score, casualties and VP
   gained; both strengths of every duel; the winner (or none); the VP at stake; and the
   prize the winner took, once they've taken it.

**Worked example** (3 players, default config, nobody owns an artifact):

| | anna: 1 Warbow, 2 Horde | ben: 2 Mage, 1 Horde | cleo: 1 Knight, 1 Bow |
|---|---|---|---|
| anna vs ben | **7** = Warbow 5 (+4 vs Horde or vs Mage) + 2 Horde | 5 = 2 Mage × 2 (+1 vs Horde) + 1 Horde | |
| anna vs cleo | 5 = Warbow 3 (+2 vs the Bow, nothing vs the Knight) + 2 Horde | | **7** = Knight 5 (+4 vs Horde beats +2 vs Warbow) + Bow 2 (+1 vs Horde) |
| ben vs cleo | | 3 = 2 Mage × 1 (nothing vs a Bow or a higher-tier Knight) + 1 Horde | **7** = Knight 5 (+2 vs Mage, +4 vs Horde) + Bow 2 (+1 vs Mage or Horde) |
| **Score** | +2 − 2 = **0** | −2 − 4 = **−6** | +2 + 4 = **+6** |

cleo wins: her Knight gets its big Horde bonus against both opponents, while ben's Mages
can't touch a Bow or a Knight. anna's Warbow beats ben's two Mages and his Horde on its own,
but loses to cleo.

### Prize and artifacts
The **artifact stack** starts with the artifacts in `GameConfig.artifacts`: by default one
each of the Resilient Banner, the Amulet of Luck and the Crown, face down.

1. **Draw:** when a battle has a winner and the stack isn't empty, one artifact is drawn
   uniformly at random from the stack with the seeded `Random` (the same as taking the top
   of a shuffled stack and shuffling a declined artifact back in). The game enters
   **CLAIM**. Only the winner sees the drawn artifact.
2. **Claim:** the winner takes **either** the artifact **or** `victoryPointsPerBattle` VP.
   - **Artifact:** it leaves the stack and belongs to the winner for the rest of the game.
     Owned artifacts are public. The winner gains no VP for this battle.
   - **VP:** the winner gains the VP, and the artifact goes back into the stack. Everyone
     sees that the winner took the VP; only the winner knows which artifact they declined.
3. **Empty stack:** once all artifacts are owned, a winner gains the VP straight away and
   there is no CLAIM phase.
4. After the prize, the game ends if the winner has `victoryPointsToWin` VP; otherwise the
   next turn starts.

| Artifact | Effect for its owner, from the turn after it's taken |
|---|---|
| **Resilient Banner** | Casualties always round down: `floor(n / 2)`, win or lose. |
| **Amulet of Luck** | In every battle, the first casualty drawn survives: one creature fewer is lost (never fewer than 0). |
| **Crown** | `crownHorde` (1) extra Horde in every turn's income. |

The Banner and the Amulet combine: the Banner rounds down first, then the Amulet saves one.

| Committed (n) | Won? | No artifact | Banner | Amulet | Banner + Amulet |
|---|---|---|---|---|---|
| 1 | lost | 1 | 0 | 0 | 0 |
| 3 | lost | 2 | 1 | 1 | 0 |
| 3 | won | 1 | 1 | 0 | 0 |
| 4 | lost | 2 | 2 | 1 | 1 |
| 5 | lost | 3 | 2 | 2 | 1 |

### Configuration
All numbers live in one object; nothing in the rules above is hard-coded elsewhere.
```kotlin
data class GameConfig(
    val minPlayers: Int = 3,
    val maxPlayers: Int = 5,
    val startingHorde: Int = 0,
    val baseCreaturesPerTurn: Int = 1,
    val resourcesPerTurn: Int = 2,
    val tier1VsHorde: Int = 1,
    val tier1VsCountered: Int = 1,
    val tier2VsHorde: Int = 4,
    val tier2VsCounteredTier1: Int = 4,
    val tier2VsOtherTier1: Int = 2,
    val tier2VsCounteredTier2: Int = 2,
    val victoryPointsPerBattle: Int = 1,
    val victoryPointsToWin: Int = 3,
    val artifacts: List<Artifact> = Artifact.entries,   // the starting stack
    val crownHorde: Int = 1,
)
```
A game is created with a `GameConfig` and a seed. The same config, seed, and sequence of
player actions always produce the same game.

## HTTP API
JSON in and out (Ktor + Jackson, already in the build). Player names are trusted — no auth.
Creature and resource names in JSON: `HORDE`, `BOW`, `SWORD`, `MAGE`, `WARBOW`, `KNIGHT`,
`HIGH_MAGE`, `WOOD`, `IRON`, `ESSENCE`. Artifact names: `RESILIENT_BANNER`,
`AMULET_OF_LUCK`, `CROWN`.

| Method & path | Body | Success | Purpose |
|---|---|---|---|
| `POST /games` | `{ "players": 4, "host": "anna", "seed": 42 }` (`seed` optional) | `201 { "gameId": "…" }` | Create a game; the host joins it |
| `POST /games/{id}/players` | `{ "name": "ben" }` | `201` | Join; the game starts when full |
| `GET /games/{id}?player={name}` | — | `200` player view (below) | Read the game as that player |
| `POST /games/{id}/players/{name}/trades` | `{ "resource": "IRON" }` | `200` player view | Trade one resource for one Horde |
| `POST /games/{id}/players/{name}/upgrades` | `{ "into": "WARBOW" }` | `200` player view | Upgrade one unit by one tier |
| `POST /games/{id}/players/{name}/ready` | — | `200` player view | Finish build-up |
| `POST /games/{id}/players/{name}/army` | `{ "HORDE": 2, "KNIGHT": 1 }` (`{}` = commit nothing) | `200` player view | Commit to the battle |
| `POST /games/{id}/players/{name}/claim` | `{ "take": "ARTIFACT" }` or `{ "take": "VICTORY_POINT" }` | `200` player view | The winner takes their prize |

The upgrade's `into` names the target; the unit it upgrades from follows from the table
(Horde for tier 1, the tier-1 unit of the same line for tier 2).

**Player view** contains: game id, phase (`WAITING_FOR_PLAYERS`, `BUILD_UP`, `BATTLE`,
`CLAIM`, `GAME_OVER`), turn number, `pot` (the VP the next or current battle is worth,
always `victoryPointsPerBattle`; the name stays for compatibility), `victoryPointsToWin`,
`artifactsInStack` (how many are left); *my* hand (counts per creature and resource),
`upgradedThisTurn` (counts per creature I upgraded into this turn), VP, whether I've acted
this phase, my `artifacts`, and `drawnArtifact` (only while I'm the winner choosing during
CLAIM, otherwise `null`); for every other player: name, VP, hand size, whether they've
acted, and their `artifacts`; the battle log; the winner once the game is over; and
`availableActions`, the actions I can take right now: `TRADE`, `UPGRADE` and `READY` during
build-up until I'm ready, `COMMIT_ARMY` during the battle until I've committed, `CLAIM`
during CLAIM if I won the battle, otherwise none. `TRADE` and `UPGRADE` are listed by phase
only, even if I can't currently afford one; `affordableUpgrades` lists the creatures I can
upgrade into right now (I hold the unit and the resource, a tier-2 upgrade starts from a
unit I held at the start of the turn, it's build-up and I'm not ready), otherwise it's
empty. Other players' hands, their `upgradedThisTurn`,
the current turn's commits, and an artifact someone else drew are never included.

**Battle log entry** in the player view:
```json
{
  "pot": 1,
  "winner": "cleo",
  "prize": "CROWN",
  "armies": {
    "cleo": { "army": { "BOW": 1, "KNIGHT": 1 }, "score": 12, "casualties": { "BOW": 1 },
              "survivors": { "KNIGHT": 1 }, "vpGained": 0 }
  },
  "duels": [
    { "players": ["anna", "ben"], "strengths": { "anna": 8, "ben": 7 } }
  ]
}
```
(`armies` lists every player, with an empty `army` and score 0 for those who sat out;
`duels` lists every pair of committed players, in join order. `prize` is `null`
with no winner or while the winner is still choosing, `"VICTORY_POINT"` when they took the
VP — which doesn't reveal the declined artifact — or the artifact's name.)

**Errors** (JSON `{ "error": "…" }`):
- `404` — unknown game, or a player name that isn't in the game
- `409` — action not allowed now: wrong phase, already ready/committed, game full,
  duplicate name, game over, claiming when I'm not the battle's winner
- `400` — invalid request: player count outside 3–5, blank name, trading a resource I don't
  have or something that isn't a resource, upgrading without the unit or resource it needs,
  upgrading a tier-1 unit I upgraded into this turn, upgrading into a Horde, committing more
  creatures than owned, unknown creature or resource, a `take` other than `ARTIFACT` or
  `VICTORY_POINT`

Games live in memory; restarting the server loses them.

## Changes from Iteration 4
| Area | Iteration 4 | Iteration 5 |
|---|---|---|
| Pot | 1 VP per turn; a tie rolls it over and it grows | Every battle is worth `victoryPointsPerBattle` (1); a tie gives nothing |
| Battle win | The winner gains the pot | The winner draws an artifact and takes it or the VP; with an empty stack, the VP |
| Phases | … → BATTLE → next turn | … → BATTLE → CLAIM (winner, stack not empty) → next turn |
| Casualties | floor / ceil by win or loss | the same, adjusted by the Resilient Banner and the Amulet of Luck |
| Income | 1 Horde + 2 resources | + `crownHorde` Horde for the Crown's owner |
| Who fights (playtest) | Every player, including empty armies at strength 0 | Only players who committed something; the others sit out (score 0, nobody scores against them); a lone committer wins uncontested |
| Bonuses (playtest) | tier 1: +2 vs Horde, +1 vs countered tier 1; tier 2: +5 vs Horde, +3 vs countered tier 1, +1 vs countered tier 2 | tier 1: +1 vs Horde, +1 vs countered tier 1; tier 2: +4 vs Horde, +4 vs countered tier 1, **+2 vs any other tier 1 (new)**, +2 vs countered tier 2 |
| API | — | `POST …/claim`; `CLAIM` phase and action; `artifactsInStack`, `me.artifacts`, `me.drawnArtifact`, `others[].artifacts`, battle log `prize` |
| Config | `tier1VsHorde`, `tier2VsHorde`, `sameTierCounter`, `higherTierCounter` | `victoryPointsPerBattle`, `artifacts`, `crownHorde`; bonuses: `tier1VsHorde`, `tier1VsCountered`, `tier2VsHorde`, `tier2VsCounteredTier1`, `tier2VsOtherTier1`, `tier2VsCounteredTier2` |

Unchanged: creatures, upgrades, trading, the single best bonus, duel strength and margins,
the 3-VP target, and every existing route and view field (`pot` keeps its name).

Seeded expectations change: rollover tests are rewritten for "a tie gives nothing", and the
artifact draw consumes the seeded `Random` after every won battle, so the end-to-end games
and `http/playtest.http` are re-derived, not patched.

## Architecture
Three layers; dependencies point downwards only.
```
Presentation  .http files, curl, e2e tests, web UI (web/) — stateless, render the player view
     │
Application   server/  Ktor routes, request/response DTOs, GameServer registry (Map<UUID, Game>)
     │
Domain        game/ battle/ cards/ player/  pure engine: rules, state, seeded randomness
```
- **Domain** knows nothing about HTTP, JSON, or Ktor: no imports from `server/`, Ktor, or
  Jackson. Every rule lives here, including trading, tiers, bonuses, duel scoring, the
  artifact stack, the claim and every artifact effect.
- **Application** looks up the game, calls one engine function per request, stores the
  result, and maps it to a DTO. It maps `GameException`s to status codes and holds no rules.
- **Responses are DTOs only.** Domain objects are never serialized directly. The player-view
  DTO is built per player, which is how hidden information — including the drawn
  artifact — stays hidden.
- **Presentation** holds no state; everything it shows comes from the player view. The web
  UI is specified and built separately ([SPEC-web-ui.md](SPEC-web-ui.md)).

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
SPEC.md                             → this spec (SPEC-web-ui.md: the web UI)
docs/ideas/                         → one-pagers and the long-term vision
http/playtest.http                  → scripted local game for manual play
src/main/kotlin/org/battleofkingdoms/
  game/      → Game state, GameConfig, phases, turn, income, trading, upgrade and claim rules
  battle/    → Army, duel strength and bonuses, round-robin Battle resolution, casualties, BattleResult / log entry
  cards/     → Card, creatures (lines, tiers, counters), resources (Wood, Iron, Essence), artifacts
  player/    → Player state (hand, units upgraded this turn, VP, artifacts, acted flag)
  server/    → Ktor module, routes, request/response DTOs, GameServer registry
  web/       → web UI (separate spec)
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
  single committer, a tie giving nothing and the next battle still worth 1, floor/ceil
  losses, every row of the artifact casualty table, random casualties with a fixed seed,
  equal income for everyone, the Crown's extra Horde from the turn after it's taken, the
  1/3 resource draw, the draw only with a winner and a non-empty stack, taking the artifact
  vs taking the VP, a declined artifact going back, an empty stack giving the VP without a
  CLAIM phase, game-over at 3 VP only after a VP prize.
- **The worked example** above is a unit test with exactly those duel strengths and scores.
- **Equal-cost duels** pin down why tier 2 is worth building: against 3 Horde, 3 cards spent
  as one tier-2 unit (5) beat a tier-1 unit and a Horde (2 + 1 = 3), which only tie 3 Horde
  (3). Tier 1 pays off against the unit it counters, not against Horde.
- **API tests** (`testApplication`): each endpoint's success path and each error status,
  including the trade and claim routes; `upgradedThisTurn` and `drawnArtifact` appear only
  in my own view, and `drawnArtifact` only while I'm the winner choosing; a declined
  artifact's name never appears in any other player's view or the battle log; owned
  artifacts appear in everyone's view; `availableActions` matches the phase, whether the
  player has acted, and who won.
- **End-to-end** (seeded, over HTTP): one full 3-player game and one full 5-player game, each
  including at least one tie, at least one trade, at least one tier-2 unit in a committed
  army, at least one artifact taken and at least one declined, played to a winner at 3 VP.
- Tests use a fixed seed; no test depends on unseeded randomness.
- Iteration 4 tests that encode the rollover are **rewritten** to "a tie gives nothing",
  keeping what each test is about, not deleted.

## Boundaries
- **Always:** run `./gradlew test` before every commit; route every number and the starting
  stack through `GameConfig`; take randomness only from the game's seeded `Random`; update
  this spec first when a rule changes.
- **Ask first:** adding dependencies; changing the rules or default numbers; bumping
  Gradle / Kotlin / Ktor / Java versions; deleting tests rather than rewriting them;
  changing files under `web/` beyond what keeps the build compiling.
- **Never:** build anything from the one-pager's "Not Doing" list (marketplace, supporters,
  diplomacy, auth, persistence, deployment) or the out-of-scope items of this iteration:
  more than three artifacts or more than one copy of any, artifacts that change duel
  strength or are committed to armies, trading, stealing or losing artifacts, any
  rollover, changes to tiers, bonuses, trading or the VP target, a third tier, simulation
  tooling, web UI features; let more than one player score in a battle; raise base attacks
  above 1; add bonuses together; disable or skip tests to get to green; commit secrets.

## Success Criteria
1. `mise run build` passes with every test green.
2. Every rule in the "Game Rules" section has at least one passing unit test, including every
   cell of the bonus table, every row of the artifact casualty table and the worked example.
3. The seeded 3-player and 5-player end-to-end tests play complete games over HTTP, each with
   at least one tie, one trade, one tier-2 unit in a committed army, one artifact taken and
   one declined, and end in `GAME_OVER` at 3 VP with the correct winner.
4. Running the same seed and actions twice produces identical battle logs.
5. No player view ever contains another player's hand, `upgradedThisTurn`, uncommitted
   army, or drawn artifact, and no view or log entry names a declined artifact (asserted in
   API tests).
6. Changing any `GameConfig` value, including the starting stack, changes behavior without
   touching any other code; an empty stack plays exactly like VP-only battles.
7. `http/playtest.http` plays a 3-player game to `GAME_OVER`, including a tie, a trade, a
   tier-2 unit, an artifact taken, an artifact declined, and at least one artifact effect
   visible in a battle or in income.
8. Every player view lists exactly the actions that player can take right now.
9. A tie never changes anyone's VP, and the next battle is worth 1 VP.
10. README, `http/playtest.http` and the idea docs describe artifacts and no longer mention a
    rollover.

## Open Questions
None that block this iteration. To watch in play:
- Is any artifact worth a VP? If everyone always takes the VP, the effects are too weak; if
  everyone always takes the artifact, games drag.
- Does the Crown snowball like renown did, now that it costs a VP?
- Does the private knowledge of a declined artifact change anyone's later choice?
- With tier 2 at +4 against Horde, does any Horde in an army become a liability, so armies
  split into "all Horde" and "no Horde"?

Deferred to later specs: more artifacts, artifacts with duel effects.
