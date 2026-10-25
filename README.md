# Battle of Kingdoms
In the world of somewhere, some unnamed tribes build small kingdoms,
battle for artifacts and hope that one day become the overlords of all other tribes.

This is a try on a deck building/strategy card game, built digitally first to allow for easier testing and playing.

The current version (Stage 1) is the **core loop** for 3–5 players, played over a JSON HTTP
API: build up your army, then bid it blindly for the victory point. The full rules and API contract
live in [SPEC.md](SPEC.md).

# How to play (Stage 1)
Every turn has two phases:

1. **Build-up.** Everyone receives 1 Horde plus 1 per point of renown, and draws 2 resources
   (Wood or Iron). On turn 1 you also get 3 extra Horde. Upgrade as often as you like, then
   mark yourself ready:
   - Horde + Wood → **Shield**
   - Horde + Iron → **Bow**
2. **Battle.** One victory point (VP) is added to the pot. Everyone secretly commits any part of their
   creatures (or nothing). Once all have committed, the armies are revealed:
   - The single strongest army wins the whole pot.
   - A tie at the top, or nobody committing, means no winner: the pot rolls over.
   - Everyone loses half of what they committed, picked at random. The winner rounds down,
     everyone else rounds up. Survivors go back to your hand.
   - Committing at least 1 creature earns 1 **renown**: a permanent extra Horde every turn.

| Creature | Attack | Notes |
|---|---|---|
| Horde | 1 | Basic unit, comes as income |
| Shield | 1, or **2** if another army in the battle has a Bow | Counters ranged |
| Bow | 2 | Ranged |

The first player to **3 VP** wins. All numbers are in `GameConfig` so they're easy to tune.

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
the requests top to bottom. It plays a seeded 3-player game through two battles, one tie and
one win, so you can watch every phase.

**A real game with curl:** each player sends their own requests. Replace `$GAME` with the id
returned when creating the game.

```bash
# Host creates a game for 3 players (add "seed": 42 for a repeatable game)
curl -X POST localhost:8080/games -H 'Content-Type: application/json' \
     -d '{"players": 3, "host": "anna"}'
# → {"gameId":"…"}

# The others join; the game starts when it is full
curl -X POST localhost:8080/games/$GAME/players -H 'Content-Type: application/json' -d '{"name": "ben"}'

# Look at the game as yourself: your hand, the pot, others' VP and renown, what you can do next
curl "localhost:8080/games/$GAME?player=anna"

# Build-up: upgrade (SHIELD or BOW) as often as you can afford, then mark yourself ready
curl -X POST localhost:8080/games/$GAME/players/anna/upgrades -H 'Content-Type: application/json' -d '{"into": "BOW"}'
curl -X POST localhost:8080/games/$GAME/players/anna/ready

# Battle: secretly commit creatures ({} commits nothing)
curl -X POST localhost:8080/games/$GAME/players/anna/army -H 'Content-Type: application/json' \
     -d '{"HORDE": 2, "BOW": 1}'
```

Your view's `availableActions` tells you what you can do right now. After each battle, the
`battleLog` in everyone's view shows all armies, strengths, casualties and the winner. When
`phase` is `GAME_OVER`, `winner` names the champion.

| Request | Does |
|---|---|
| `POST /games` `{players, host, seed?}` | Create a game; the host joins it |
| `POST /games/{id}/players` `{name}` | Join |
| `GET /games/{id}?player={name}` | Your view of the game |
| `POST /games/{id}/players/{name}/upgrades` `{into}` | Upgrade one Horde |
| `POST /games/{id}/players/{name}/ready` | Finish build-up |
| `POST /games/{id}/players/{name}/army` `{"HORDE": n, …}` | Commit to the battle |

Errors come back as `{"error": "…"}`: `400` for an invalid request (e.g. an upgrade you can't
afford), `404` for an unknown game or player, `409` for an action not allowed right now
(e.g. acting twice or in the wrong phase).

# Development
```bash
mise run build               # compile and run all tests (./gradlew build)
mise run test                # tests only (./gradlew test)
mise run gradle -- <args>    # any other Gradle invocation
```
- [SPEC.md](SPEC.md): Stage 1 rules, API, architecture and boundaries
- [tasks/](tasks/): implementation plan and task list
- [docs/ideas/](docs/ideas/): the core-loop one-pager and the long-term game vision

# Where it's heading
Ideas beyond Stage 1, deliberately left out until the core loop proves fun (see
[the full game vision](docs/ideas/full-game-vision.md)):

- **Marketplace:** trade resources and artifacts with other players
- **Artifacts with powers** that theme your army
- **Deeper upgrades:** weapons, armour, multi-level creatures; new resources like _Essence_
  for magical stuff
- **Diplomacy:** alliances, treaties and whatnots
- **Supporters**, used before or after battles, e.g.
  - attracting crowds (for more creatures)
  - resource gatherer (for more resources)
  - healer (for increased army recovery after battles)
  - tactician (allowing to use different battle tactics)
