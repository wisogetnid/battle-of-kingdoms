# Todo: Artifacts (Iteration 5)

Plan: [plan.md](plan.md) · Spec: [SPEC.md](../SPEC.md)

## Phase 0 — Playtest rebalance
- [x] **B1. New bonus table in the engine**
  - Acceptance: `Bonuses` takes six values — `tier1VsHorde`, `tier1VsCountered`, `tier2VsHorde`, `tier2VsCounteredTier1`, `tier2VsOtherTier1`, `tier2VsCounteredTier2` — replacing `sameTierCounter` / `higherTierCounter`; `GameConfig` defaults 1, 1, 4, 4, 2, 2; every cell of the spec's bonus table passes for all three lines, including tier 2 +2 against any other tier-1 unit (its own line's too) and nothing for tier 2 against an uncountered tier 2; the worked example gives duels 7:5, 5:7, 3:7, scores 0, −6, +6, cleo wins; the equal-cost duel gives tier 2 5 > tier 1 + Horde 3 = 3 Horde 3; the custom-table test sets each of the six values; `GameConfigTests` pins the defaults; tests whose point a new number broke are re-derived to keep that point (fielding Horde: 3 Horde vs 2 Bows, 3:4)
  - Verify: `mise run build`; `grep -rn "sameTierCounter\|higherTierCounter" src` finds nothing
  - Files: `battle/Bonuses.kt`, `game/GameConfig.kt`, `battle/ArmyTests.kt`, `battle/BattleTests.kt`, `game/GameConfigTests.kt`

- [x] **B2. Bonus docs and playtest comments**
  - Acceptance: README's bonus table, example and equal-cost paragraph show +1 / +4 / +2; the core-loop one-pager's note mentions the rebalance; `http/playtest.http`'s header and battle comments match the new numbers (battle 2: 3:6, 3:3, 6:3, ben wins; battle 3: 5:7, 5:4, 6:2, ben wins the game)
  - Verify: run `mise run server`, replay `http/playtest.http` and compare each battle with its comment; `grep -rnE "\+5|\+3 vs" README.md http` finds nothing stale
  - Files: `README.md`, `docs/ideas/battle-of-kingdoms-core-loop.md`, `http/playtest.http`
  - Status: replayed against a running server; every battle matches its comment, including the expected 400

- [x] **B3. Only committed armies fight**
  - Acceptance: a player who commits nothing sits out — no duels, score 0, no casualties, and nobody scores against them; duels are a round robin among committed players only; a lone committer wins uncontested (score 0, no duels, loses `floor(n / 2)`); nobody committing means no winner; the playtest's case (anna 3 Horde vs bob Horde + Mage, cleo empty) is a 3:3 tie with no winner; the battle log page shows "cleo sat out"; tests that counted duels against empty armies are rewritten to the new rule
  - Verify: `mise run build`
  - Files: `battle/Battle.kt`, `web/Pages.kt`, `BattleTests.kt`, `ActionRoutesTests.kt`, `e2e/EndToEndMultiplayerTest.kt`, `web/WebRoutesTests.kt`, plus SPEC.md, SPEC-web-ui.md, README.md, `http/playtest.http`

### Checkpoint 0 — build green; the bonus table in SPEC.md, README and the playtest agree; empty armies sit out

## Phase 1 — Rules in the engine
- [ ] **T1. A tie gives nothing; the game awards the VP**
  - Acceptance: `GameConfig.victoryPointsPerBattle = 1`; `pot` is always that value (no rollover, no growth); a tie changes nobody's VP and the next battle is worth 1; `Battle.resolve` no longer takes a pot and `Game` sets the winner's `vpGained`; `BattleResult.rolledOver` is gone; rollover tests are rewritten to "a tie gives nothing"; a lone committer's uncontested win (B3) also scores 1 VP; the playtest's "rolls over" and "double pot" comments stay stale until T7 re-derives it
  - Verify: `mise run build`; `grep -rn rolledOver src` finds nothing
  - Files: `game/GameConfig.kt`, `game/Game.kt`, `battle/Battle.kt`, `battle/BattleResult.kt`, `BattleTests.kt`, `BattleTurnTests.kt`, `LobbyTests.kt`, `FullGameTests.kt`, `e2e/EndToEndMultiplayerTest.kt`

- [ ] **T2. Resilient Banner and Amulet of Luck**
  - Acceptance: `cards/Artifact.kt` (RESILIENT_BANNER, AMULET_OF_LUCK, CROWN); `Player.artifacts`; `Battle.resolve` takes each player's artifacts; the Banner always floors, the Amulet loses one fewer (not below 0), both combine; every row of the spec's casualty table passes for winners and losers; a player who sat out (B3) loses nothing whatever they own
  - Verify: `mise run test`
  - Files: `cards/Artifact.kt` (new), `player/Player.kt`, `battle/Battle.kt`, `game/Game.kt`, `BattleTests.kt`

- [ ] **T3. Crown**
  - Acceptance: `GameConfig.crownHorde = 1`; the Crown's owner gets that many extra Horde in every turn's income; nobody else does
  - Verify: `mise run test`
  - Files: `game/GameConfig.kt`, `game/Game.kt`, `game/LobbyTests.kt` (IncomeTests)

### Checkpoint A — build green; every artifact effect has a unit test

## Phase 2 — The claim
- [ ] **T4. Claim in the engine**
  - Acceptance: `GameConfig.artifacts` (interim default: empty); `Game.artifactStack` and `drawnArtifact`; a battle with a winner and a non-empty stack draws one artifact at random and enters `Phase.CLAIM`; `Game.claim(name, take)` — `ARTIFACT` moves it to the winner (no VP), `VICTORY_POINT` gives the VP and leaves the stack unchanged; `BattleResult.prize` records `VICTORY_POINT` or the artifact; an empty stack gives the VP with no CLAIM; game over only after a VP prize; `Action.CLAIM` only for the winner during CLAIM; `NotTheWinner` (409) for anyone else, `WrongPhase` outside CLAIM; `web/Pages.kt` gets plain `CLAIM` branches so it compiles
  - Verify: `mise run build`; new `ClaimTests` with a config holding all three artifacts
  - Files: `game/Game.kt`, `game/GameConfig.kt`, `game/GameException.kt`, `battle/BattleResult.kt`, `server/GameRoutes.kt` (status mapping), `web/Pages.kt` (compile only), `game/ClaimTests.kt` (new)

- [ ] **T5. Claim over HTTP**
  - Acceptance: `POST /games/{id}/players/{name}/claim` with `{ "take": "ARTIFACT" | "VICTORY_POINT" }` returns my view; 409 for a non-winner or outside CLAIM, 400 for another `take`; the view adds `artifactsInStack`, `me.artifacts`, `me.drawnArtifact` (winner during CLAIM only), `others[].artifacts`, and the log's `prize` (new fields last, with defaults); API tests show the drawn and the declined artifact never reach another view or the log, and owned artifacts reach everyone
  - Verify: `mise run build`
  - Files: `server/GameRoutes.kt`, `server/GameServer.kt`, `server/Dtos.kt`, `server/Application.kt` (optional `GameServer`), `TestApi.kt` (optional config), `server/ClaimRoutesTests.kt` (new), `ApplicationKtTests.kt`

- [ ] **T6. Three artifacts by default; full games claim**
  - Acceptance: `GameConfig.artifacts` defaults to all three; `FullGameTests` and both end-to-end games (3 and 5 players) include a tie, a trade, a tier-2 unit in a committed army, one artifact taken and at least one declined, and end in `GAME_OVER` at 3 VP; same-seed replays are identical
  - Verify: `mise run build`
  - Files: `game/GameConfig.kt`, `game/GameConfigTests.kt`, `game/FullGameTests.kt`, `e2e/EndToEndMultiplayerTest.kt`

### Checkpoint B — build green; SPEC.md criteria 1–6, 8 and 9 met

## Phase 3 — Play and docs
- [ ] **T7. Playtest and docs**
  - Acceptance: `http/playtest.http` (seed 42) plays to `GAME_OVER` with a tie, a trade, a tier-2 unit, an artifact taken, one declined and at least one artifact effect visible, comments matching the real outcome; README explains the prize choice, the three artifacts and the claim route; the core-loop one-pager's note and the vision doc's artifact rows reflect Iteration 5; no rollover is mentioned
  - Verify: run `mise run server`, replay `http/playtest.http` and compare with the comments; `grep -rni "roll" README.md http` finds no rollover
  - Files: `http/playtest.http`, `README.md`, `docs/ideas/battle-of-kingdoms-core-loop.md`, `docs/ideas/full-game-vision.md`

### Checkpoint C — all 10 SPEC.md success criteria met
