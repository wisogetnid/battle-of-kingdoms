# Todo: Counter Triangle (Iteration 2)

Plan: [plan.md](plan.md) · Spec: [SPEC.md](../SPEC.md)

## Phase 1 — Engine
- [x] **T1. Rename artifact → victory point**
  - Acceptance: `GameConfig.victoryPointsToWin` replaces `artifactsToWin`; the player view field is `victoryPointsToWin`; no `artifact` naming remains in code, tests, or README; no behavior change
  - Verify: `mise run build`; `grep -rni artifactsToWin src README.md` finds nothing
  - Files: `game/GameConfig.kt`, `game/Game.kt`, `server/Dtos.kt`, `BattleTurnTests.kt`, `ApplicationKtTests.kt`, `README.md`

- [x] **T2. Duels and scores**
  - Acceptance: `Army.duelStrength(opponent)` replaces `strength(otherArmies)`; `Battle.resolve` fights every pair once (join order), sums margins into scores that add up to 0; single highest score wins, ties at the top and all-zero have no winner, a single committer wins; empty armies fight at strength 0; `BattleResult` has `duels` and per-army `score`; the view shows `score` and `duels` as in the spec; casualties, renown, pot unchanged
  - Verify: `mise run test`; `BattleTests` has parameterized cases for margins, zero-sum, tie, all-zero, single committer, bonus only in the duel against the countered opponent
  - Files: `battle/Army.kt`, `battle/Battle.kt`, `battle/BattleResult.kt`, `server/Dtos.kt`, `BattleTests.kt`, `ArmyTests.kt`, `ActionRoutesTests.kt`

- [x] **T3. Counter triangle and Essence**
  - Acceptance: `ResourceType` WOOD, IRON, ESSENCE drawn 1/3 each; `CreatureType` HORDE, BOW (Wood), SWORD (Iron), MAGE (Essence), all attack 1, with `counters` Bow→Mage, Sword→Bow, Mage→Sword; `+counterBonus` per countering creature in a duel; `GameConfig.counterBonus = 1`; `Trait` and `SHIELD` removed; `SHIELD` in a request is a 400; the spec's worked example and a going-wide case are unit tests
  - Verify: `mise run build`; `grep -rn "SHIELD\|Trait\|RANGED" src` finds nothing
  - Files: `cards/CreatureType.kt`, `cards/ResourceType.kt`, `cards/Trait.kt` (delete), `battle/Army.kt`, `battle/Battle.kt`, `game/GameConfig.kt`, `game/Game.kt`, plus the tests naming old units: `CardTests`, `ArmyTests`, `BattleTests`, `BuildUpTests`, `IncomeTests`, `FullGameTests`, `ActionRoutesTests`, `EndToEndMultiplayerTest` (kept green only; rewritten in T4)

### Checkpoint A — engine complete; review duels and counters before the end-to-end games

## Phase 2 — Games and play
- [ ] **T4. End-to-end games with counters**
  - Acceptance: seeded 3- and 5-player games over HTTP each include a rollover and at least one battle whose winner is decided by a counter bonus (asserted from the log's duel strengths), and end in `GAME_OVER` with the expected winner; same-seed replay gives identical battle logs; `FullGameTests` covers the same at engine level
  - Verify: `mise run test`
  - Files: `e2e/EndToEndMultiplayerTest.kt`, `game/FullGameTests.kt`

- [ ] **T5. Playtest and docs**
  - Acceptance: `http/playtest.http` plays a seeded 3-player game through two battles under the new rules, one decided by a counter, with comments matching the actual outcome; README rules, creature table and curl examples describe the triangle; the vision doc marks Shield as replaced and Essence as adopted; the core-loop one-pager notes that Iteration 2 changed its rules
  - Verify: run `mise run server`, replay `http/playtest.http` top to bottom, check both battles in the log match the comments; `grep -rn "SHIELD\|Shield\|artifactsToWin" README.md http` finds nothing
  - Files: `http/playtest.http`, `README.md`, `docs/ideas/full-game-vision.md`, `docs/ideas/battle-of-kingdoms-core-loop.md`

### Checkpoint B — all 9 SPEC.md success criteria met
