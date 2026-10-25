# Todo: Two Tiers and Trading (Iteration 3)

Plan: [plan.md](plan.md) · Spec: [SPEC.md](../SPEC.md)

## Phase 1 — Engine
- [x] **T1. Remove renown, 5 VP to win**
  - Acceptance: income is `baseCreaturesPerTurn` Horde (+ `startingHorde` on turn 1) and `resourcesPerTurn` resources for everyone; `Player.renown`, `GameConfig.renownPerBattle`, `ArmyResult.gainedRenown` and the view's `renown` / `renownGained` fields are gone; `victoryPointsToWin` defaults to 5; tests that asserted renown are rewritten to assert equal income
  - Verify: `mise run build`; `grep -rni renown src README.md` finds nothing
  - Files: `player/Player.kt`, `game/Game.kt`, `game/GameConfig.kt`, `battle/Battle.kt`, `battle/BattleResult.kt`, `server/Dtos.kt`, `BattleTests.kt`, `BattleTurnTests.kt`, `LobbyTests.kt`, `README.md`

- [x] **T2. Trading**
  - Acceptance: `Game.trade(name, resource)` swaps one resource for one Horde during build-up, any number of times; `RuleViolation` without that resource, `WrongPhase` outside build-up, `AlreadyActed` after ready, `UnknownPlayer`; `POST /games/{id}/players/{name}/trades` `{ "resource": … }` returns my view; a creature or unknown name in `resource` is a 400; `availableActions` lists `TRADE` with `UPGRADE` and `READY` in build-up
  - Verify: `mise run test`
  - Files: `game/Game.kt`, `server/GameRoutes.kt`, `server/Dtos.kt`, `server/GameServer.kt`, `game/BuildUpTests.kt`, `ActionRoutesTests.kt`, `ApplicationKtTests.kt`

- [x] **T3. Tier-2 creatures, one tier per turn**
  - Acceptance: `CreatureType` adds WARBOW, KNIGHT, HIGH_MAGE with `tier` and `line`; tier 2 costs the tier-1 unit of its line + 1 resource of that line (no Horde); a unit upgraded into this turn can't be upgraded again until next turn (`RuleViolation`); `Player.upgradedThisTurn` resets at turn start and appears only in my own view; tier-2 units use the existing flat counter bonus by line until T4
  - Verify: `mise run test`; parameterized cases for each tier-2 upgrade, missing unit, missing resource, same-turn block, next-turn allowed
  - Files: `cards/CreatureType.kt` (+ `Line`), `player/Player.kt`, `game/Game.kt`, `battle/Army.kt`, `server/Dtos.kt`, `CardTests.kt`, `BuildUpTests.kt`, `ApplicationKtTests.kt`

- [x] **T4. Tier bonus table**
  - Acceptance: duel strength per creature = 1 + the single best bonus against the opponent's army, from `Bonuses(tier1VsHorde = 2, tier2VsHorde = 3, sameTierCounter = 1, higherTierCounter = 2)` built from `GameConfig`; lower tier gets nothing against higher; no bonus outside the table; bonuses never add up; `counterBonus` removed; every bonus-table cell for all three lines, the max-not-sum cases and the spec's worked example are unit tests
  - Verify: `mise run build`; `grep -rn counterBonus src` finds nothing
  - Files: `battle/Bonuses.kt` (new), `battle/Army.kt`, `battle/Battle.kt`, `game/GameConfig.kt`, `game/Game.kt`, `ArmyTests.kt`, `BattleTests.kt`, plus tests whose expected strengths change

### Checkpoint A — engine complete; review tiers and bonuses before the end-to-end games

## Phase 2 — Games and play
- [ ] **T5. End-to-end games**
  - Acceptance: seeded 3- and 5-player games over HTTP each include a rollover, at least one trade and at least one tier-2 unit in a committed army, and end in `GAME_OVER` at 5 VP with the expected winner; same-seed replay gives identical battle logs; `FullGameTests` covers the same at engine level
  - Verify: `mise run test`
  - Files: `e2e/EndToEndMultiplayerTest.kt`, `game/FullGameTests.kt`

- [ ] **T6. Playtest and docs**
  - Acceptance: `http/playtest.http` plays a seeded 3-player game through at least two battles with a trade and a tier-2 unit in a committed army, comments matching the actual outcome; README describes trading, both tiers, the bonus table, equal income and 5 VP; the vision doc records renown as removed and the second upgrade tier as adopted
  - Verify: run `mise run server`, replay `http/playtest.http` top to bottom and compare the log with the comments; `grep -rni "renown\|counterBonus" README.md http` finds nothing
  - Files: `http/playtest.http`, `README.md`, `docs/ideas/full-game-vision.md`

### Checkpoint B — all 9 SPEC.md success criteria met
