# Todo: Core Loop MVP (Stage 1)

Plan: [plan.md](plan.md) · Spec: [SPEC.md](../SPEC.md)

## Phase 0 — Foundation
- [x] **T1. Toolchain and launch configuration**
  - Acceptance: `.mise.toml` pins Java 21 (Temurin) and defines `gradle`/`build`/`test`/`server` tasks (done); `application.conf` module and `mainClass` point to `org.battleofkingdoms.server`; existing tests pass
  - Verify: `mise run build`; `mise run server`, then `curl localhost:8080/game` returns 200
  - Files: `.mise.toml`, `build.gradle.kts`, `src/main/resources/application.conf`
  - Prerequisites (you): asdf hook removed from `~/.zshrc` (done); `mise trust && mise install`

### Checkpoint A — build green, server starts

## Phase 1 — Engine
- [x] **T2. Card model and army strength**
  - Acceptance: `CreatureType` (Horde 1, Shield 1/2, Bow 2 + Ranged) and `ResourceType` (Wood, Iron) enums; `Hand` with counts; `Army.strength(otherArmies)`; Shield +1 if any other army has a Ranged creature; Bow upgrades with Iron, Shield with Wood; `Food` removed
  - Verify: `./gradlew test --tests '*CardTests' --tests '*ArmyTests'`
  - Files: `cards/**`, `battle/Army.kt`, `CardTests.kt`, `ArmyTests.kt`

- [x] **T3. GameConfig, Game state, lobby, income**
  - Acceptance: immutable `Game` with config, seeded `Random`, phase, turn, pot, players, log; create with 3–5 players (else `RuleViolation`); join rejects duplicates and full games; when full → turn 1 income (`startingHorde + base + renown` Horde, `resourcesPerTurn` seeded resources), pot 1, `BUILD_UP`; `GameServer` adapted; old 2-player e2e test deleted
  - Verify: `./gradlew test --tests 'org.battleofkingdoms.game.*'`
  - Files: `game/Game.kt`, `game/GameConfig.kt`, `game/GameException.kt`, `player/Player.kt`, `server/GameServer.kt` (+ remove `game/Board.kt`, `EndToEnd2PlayerUnitTest.kt`)

- [x] **T4. Build-up: upgrade and ready**
  - Acceptance: upgrade consumes 1 Horde + matching resource; fails with `RuleViolation` when either is missing, `WrongPhase` outside build-up, `AlreadyActed` after ready; all ready → `BATTLE`
  - Verify: `./gradlew test --tests 'org.battleofkingdoms.game.*'`
  - Files: `game/Game.kt`, `game/BuildUpTests.kt`

- [x] **T5. Battle resolution (pure)** — can run in parallel with T3/T4
  - Acceptance: `Battle.resolve(commits, pot, random)` returns winner or none (ties at the top, all-zero), single committer wins, payout vs rollover, winner loses `floor(n/2)` and others `ceil(n/2)` picked by the seeded `Random`, survivors, renown flags (committed ≥ 1), log entry
  - Verify: `./gradlew test --tests '*BattleTests'` (parameterized cases for every rule)
  - Files: `battle/Battle.kt`, `battle/BattleResult.kt`, `BattleTests.kt`

- [ ] **T6. Battle wiring and turn loop**
  - Acceptance: `commitArmy` validates ownership (`RuleViolation`), phase, and double commits; commits are hidden until all are in; resolution applies VP, renown, survivors, log; next turn adds 1 to the pot and pays income including renown; reaching `artifactsToWin` → `GAME_OVER` with winner
  - Verify: `./gradlew test`; includes a full seeded 3-player engine game with a rollover, plus a same-seed replay with an identical log
  - Files: `game/Game.kt`, `game/BattleTurnTests.kt`, `game/FullGameTests.kt`

### Checkpoint B — full game playable through the engine; review the engine API

## Phase 2 — HTTP API
- [ ] **T7. API: create, join, view**
  - Acceptance: `POST /games` → 201 `{gameId}`; `POST /games/{id}/players` → 201; `GET /games/{id}?player=` → player view incl. `availableActions` per phase/acted; 404/409/400 per spec as `{error}`; one player's view never contains another's hand; Jackson handles data classes and enum-keyed maps
  - Verify: `./gradlew test --tests 'org.battleofkingdoms.server.*'`
  - Files: `server/Application.kt`, `server/GameRoutes.kt`, `server/Dtos.kt`, `server/GameServer.kt`, `ApplicationKtTests.kt` (rewritten)

- [ ] **T8. API: upgrade, ready, army**
  - Acceptance: the three action routes return the updated view; every error status per spec; a committed army is invisible to others until resolution and then appears in the battle log
  - Verify: `./gradlew test --tests 'org.battleofkingdoms.server.*'`
  - Files: `server/GameRoutes.kt`, `server/Dtos.kt`, `server/ActionRoutesTests.kt`

- [ ] **T9. HTTP end-to-end games**
  - Acceptance: seeded 3-player and 5-player games over HTTP, each with a forced tie/rollover, end in `GAME_OVER` with the expected winner; same-seed replay gives identical battle logs
  - Verify: `./gradlew test --tests 'org.battleofkingdoms.e2e.*'`
  - Files: `e2e/EndToEndMultiplayerTest.kt`

- [ ] **T10. Playtest script**
  - Acceptance: `http/playtest.http` creates a 3-player game and plays it through two battles
  - Verify: run `./gradlew run`, execute the file top to bottom in IntelliJ, and check the second battle appears in the log
  - Files: `http/playtest.http`

### Checkpoint C — all 9 SPEC.md success criteria met
