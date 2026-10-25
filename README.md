# Battle of Kingdoms
In the world of somewhere, some unnamed tribes build small kingdoms,
battle for artifacts and hope that one day become the overlords of all other tribes.

This is a try on a deck building/strategy card game, built digitally first to allow for easier testing and playing.

The current version (Iteration 3, **two tiers and trading**) is the core loop for 3–5 players,
played over a JSON HTTP API: build up your army, then bid it blindly for the victory point.
The full rules and API contract live in [SPEC.md](SPEC.md).

# How to play
Every turn has two phases:

1. **Build-up.** Everyone receives 1 Horde and draws 2 resources (Wood, Iron, or Essence);
   income is the same for every player, and everyone starts with nothing else. Trade and
   upgrade as often as you like, then mark yourself ready:
   - **Trade** any resource for 1 Horde.
   - **Tier 1:** Horde + Wood → **Bow**, Horde + Iron → **Sword**, Horde + Essence → **Mage**.
   - **Tier 2:** Bow + Wood → **Warbow**, Sword + Iron → **Knight**, Mage + Essence → **High Mage**.
   - A unit climbs one tier per turn: a Bow you build this turn can become a Warbow next turn.
2. **Battle.** One victory point (VP) is added to the pot. Everyone secretly commits any part of
   their creatures (or nothing). Once all have committed, the armies are revealed and every
   pair of players fights a **duel**:
   - In each duel, every creature has attack 1 plus its **single best bonus** against
     *that* opponent's army (see the table below). Bonuses never add up.
   - Your **score** is the sum of your margins over all your duels (win a duel 5:2 → +3).
   - The single highest score wins the whole pot.
   - A tie at the top, or nobody committing, means no winner: the pot rolls over.
   - Everyone loses half of what they committed, picked at random. The winner rounds down,
     everyone else rounds up. Survivors go back to your hand.

The lines form a counter triangle: **Bow beats Mage, Sword beats Bow, Mage beats Sword.**

| Bonus | vs Horde | vs the countered line, tier 1 | vs the countered line, tier 2 |
|---|---|---|---|
| Horde | — | — | — |
| Tier 1 (Bow, Sword, Mage) | **+2** | **+1** | — (a lower tier gets nothing against a higher one) |
| Tier 2 (Warbow, Knight, High Mage) | **+3** | **+2** | **+1** |

For example, a Warbow against an army with a Mage and Horde gets +3 (not +3 and +2), so it
attacks for 4. Any Horde you commit hands every upgraded opponent its Horde bonus, while
Horde are also your cheapest casualties. Upgraded units hit harder but every random
casualty among them hurts more, and resources traded for Horde are gone for tier 2.
Everyone sees all armies after each battle, so use what you learned about each opponent.
The first player to **5 VP** wins. All numbers are in `GameConfig` so they're easy to tune.

# Running a session
Games live in memory on one server; restarting it ends every game. Player names are trusted
(there's no login), so play with people you know.

## 1. Start the server
```bash
mise trust && mise install   # once: installs Java 21
mise run server              # serves on http://localhost:8080
```
Other players need to reach this address: on the same network use your machine's IP
instead of `localhost`, or share it through a tunnel of your choice.

## 2. Play
**Scripted playtest in IntelliJ:** open [`http/playtest.http`](http/playtest.http) and run
the requests top to bottom. It plays a seeded 3-player game through two battles: a trade and
a tie that rolls the pot over, then a win decided by a High Mage built over two turns.

**A real game with curl:** each player sends their own requests. Replace `$GAME` with the id
returned when creating the game.

```bash
# Host creates a game for 3 players (add "seed": 42 for a repeatable game)
curl -X POST localhost:8080/games -H 'Content-Type: application/json' \
     -d '{"players": 3, "host": "anna"}'
# → {"gameId":"…"}

# The others join; the game starts when it is full
curl -X POST localhost:8080/games/$GAME/players -H 'Content-Type: application/json' -d '{"name": "ben"}'

# Look at the game as yourself: your hand, the pot, others' VP, what you can do next
curl "localhost:8080/games/$GAME?player=anna"

# Build-up: trade resources for Horde and upgrade as often as you can afford, then mark yourself ready
curl -X POST localhost:8080/games/$GAME/players/anna/trades -H 'Content-Type: application/json' -d '{"resource": "IRON"}'
curl -X POST localhost:8080/games/$GAME/players/anna/upgrades -H 'Content-Type: application/json' -d '{"into": "BOW"}'
# … next turn, the Bow can climb to tier 2
curl -X POST localhost:8080/games/$GAME/players/anna/upgrades -H 'Content-Type: application/json' -d '{"into": "WARBOW"}'
curl -X POST localhost:8080/games/$GAME/players/anna/ready

# Battle: secretly commit creatures ({} commits nothing)
curl -X POST localhost:8080/games/$GAME/players/anna/army -H 'Content-Type: application/json' \
     -d '{"HORDE": 2, "WARBOW": 1}'
```

Your view's `availableActions` tells you what you can do right now. After each battle, the
`battleLog` in everyone's view shows all armies, every duel's strengths, the scores,
casualties and the winner. When
`phase` is `GAME_OVER`, `winner` names the champion.

| Request | Does |
|---|---|
| `POST /games` `{players, host, seed?}` | Create a game; the host joins it |
| `POST /games/{id}/players` `{name}` | Join |
| `GET /games/{id}?player={name}` | Your view of the game |
| `POST /games/{id}/players/{name}/trades` `{resource}` | Trade one resource for one Horde |
| `POST /games/{id}/players/{name}/upgrades` `{into}` | Upgrade one unit by one tier |
| `POST /games/{id}/players/{name}/ready` | Finish build-up |
| `POST /games/{id}/players/{name}/army` `{"HORDE": n, …}` | Commit to the battle |

Errors come back as `{"error": "…"}`: `400` for an invalid request (e.g. an upgrade you can't
afford, or a second tier in the same turn), `404` for an unknown game or player, `409` for an action not allowed right now
(e.g. acting twice or in the wrong phase).

# Development
```bash
mise run build               # compile and run all tests (./gradlew build)
mise run test                # tests only (./gradlew test)
mise run gradle -- <args>    # any other Gradle invocation
```
- [SPEC.md](SPEC.md): current rules, API, architecture and boundaries
- [tasks/](tasks/): implementation plan and task list
- [docs/ideas/](docs/ideas/): the core-loop one-pager and the long-term game vision

# Where it's heading
Ideas beyond the current iteration, deliberately left out until the core loop proves fun (see
[the full game vision](docs/ideas/full-game-vision.md)):

- **Marketplace:** trade resources and artifacts with other players
- **Artifacts with powers** that theme your army
- **Deeper upgrades:** a third tier, weapons, armour
- **Diplomacy:** alliances, treaties and whatnots
- **Supporters**, used before or after battles, e.g.
  - attracting crowds (for more creatures)
  - resource gatherer (for more resources)
  - healer (for increased army recovery after battles)
  - tactician (allowing to use different battle tactics)
