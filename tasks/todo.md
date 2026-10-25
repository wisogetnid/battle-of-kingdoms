# Todo: Decisive Tier 2 (Iteration 4)

Plan: [plan.md](plan.md) · Spec: [SPEC.md](../SPEC.md)

## Phase 1 — Numbers
- [x] **T1. Stronger tier 2**
  - Acceptance: `GameConfig` defaults to `tier2VsHorde = 5` and `higherTierCounter = 3`; every tier-2 cell of the bonus table (vs Horde +5, vs the tier-1 unit it counters +3, vs the tier-2 unit it counters +1) passes for all three lines; the tier-2 strength cases (one bonus against many Horde, best of several, never the sum) and the worked example (duels 8:7, 3:9, 3:9; scores −5, −7, +12; cleo wins) are re-derived; the equal-cost duel test passes (one tier-2 unit 6 > tier 1 + Horde 4 > 3 Horde 3); a test pins `GameConfig()` bonus defaults to the spec
  - Verify: `mise run build`; `git diff --stat` shows `GameConfig.kt` as the only production file
  - Files: `game/GameConfig.kt`, `battle/ArmyTests.kt`, `battle/BattleTests.kt`, `game/GameConfigTests.kt` (new, config defaults), plus any seeded test whose outcome changes

- [x] **T2. Race to 3 VP**
  - Acceptance: `GameConfig` defaults to `victoryPointsToWin = 3`; the player view shows 3; `FullGameTests` and both end-to-end games (3 and 5 players) end in `GAME_OVER` with anna on at least 3 VP, a turn-1 rollover, at least one trade and a tier-2 unit in a committed army; same-seed replays give identical logs; test names say 3 VP
  - Verify: `mise run build`
  - Files: `game/GameConfig.kt`, `game/GameConfigTests.kt`, `server/ApplicationKtTests.kt`, `game/FullGameTests.kt`, `e2e/EndToEndMultiplayerTest.kt`

### Checkpoint A — build green; no production change outside `GameConfig.kt`

## Phase 2 — Play and docs
- [x] **T3. Playtest and docs**
  - Acceptance: `http/playtest.http` (seed 42) states the new bonuses and 3 VP in its header, its battle comments match the actual outcome under the new numbers, and it plays to `GAME_OVER` with a trade and a tier-2 unit in a committed army; README's bonus table, best-bonus example and VP target show +5 / +3 / 3 VP; the core-loop one-pager's note and the vision doc mention Iteration 4
  - Verify: run `mise run server`, replay `http/playtest.http` top to bottom and compare with the comments; `grep -rnE "5 VP|\+3 vs Horde" README.md http docs` finds nothing stale
  - Files: `http/playtest.http`, `README.md`, `docs/ideas/battle-of-kingdoms-core-loop.md`, `docs/ideas/full-game-vision.md`

### Checkpoint B — all 11 SPEC.md success criteria met
