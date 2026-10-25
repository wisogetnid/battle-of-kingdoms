# Plan: Decisive Tier 2 (Iteration 4)

Spec: [SPEC.md](../SPEC.md) · Tasks: [todo.md](todo.md)

Stage 1, Iteration 2 and Iteration 3 are delivered and their plans retired; git history keeps
them. The web UI has its own plan in [web-ui/](web-ui/) and isn't touched here.

## Architecture decisions
1. **Config only.** The engine already reads every bonus and the VP target from `GameConfig`
   (Iteration 3, success criterion 6). Iteration 4 changes three defaults:
   `tier2VsHorde = 5`, `higherTierCounter = 3`, `victoryPointsToWin = 3`. No production code
   outside `GameConfig.kt` changes; a diff that touches `Bonuses`, `Army`, `Battle` or `Game`
   is a sign something went wrong.
2. **One slice per rule change.** The tier-2 bonuses and the VP target are independent, so
   each gets its own task and commit with its own re-derived tests. Each task leaves the
   build green.
3. **Battle tests keep their own `Bonuses` literal.** `ArmyTests` and `BattleTests` build
   `Bonuses(…)` directly, so `battle/` tests don't depend on `game/`. They are updated to
   the new numbers, and a game-level test pins `GameConfig()` defaults to the spec values,
   so the literals and the config can't drift apart unnoticed.
4. **The equal-cost duel is the iteration's headline test.** Three cards against 3 Horde:
   one tier-2 unit (6) > a tier-1 unit and a Horde (4) > 3 Horde (3). It states the reason
   for the change as an executable rule.
5. **Seeded expectations are re-derived, never patched until green.** The worked example is
   recomputed in SPEC.md already (8:7, 3:9, 3:9; scores −5, −7, +12). The end-to-end
   scripts react to the seeded deals and assert what the script forces.

## Dependency graph
```
T1 tier 2: +5 vs Horde, +3 vs the tier-1 unit it counters
T2 race to 3 VP
 (T1 and T2 are independent; both edit GameConfig.kt, so they run in sequence)
        └─ T3 playtest.http + docs (needs both numbers)
```

## Phases and checkpoints

### Phase 1 — Numbers
**T1. Stronger tier 2.** `tier2VsHorde` 3 → 5, `higherTierCounter` 2 → 3; bonus-table cells,
tier-2 strength cases, the worked example and the equal-cost duel as unit tests; a test
pinning the config defaults.

**T2. Race to 3 VP.** `victoryPointsToWin` 5 → 3; view test, engine full game and the
end-to-end games play to 3 VP with a rollover, a trade and a tier-2 unit.

**Checkpoint A:** `mise run build` green; `git diff --stat` since the spec shows no
production change outside `GameConfig.kt`.

### Phase 2 — Play and docs
**T3. Playtest and docs.** `http/playtest.http` re-derived for the new numbers and played to
`GAME_OVER` at 3 VP (the game is short enough now); README bonus table, example and VP
target; idea docs note Iteration 4.

**Checkpoint B:** all 11 SPEC.md success criteria met.

## Risks and mitigations
| Risk | Impact | Mitigation |
|---|---|---|
| With 3 VP, a scripted game ends before anna fields a tier-2 unit | E2E can't show tier 2 | The others always tie each other (1 Horde each) while anna fields nothing, so nobody can win until anna fields tier 2; assert it |
| A turn-2 win with a double pot ends the game before the asserted battles exist | Index errors in log assertions | Assert only what the script forces (turn 1 tie, then the pot grows until anna wins) |
| Casualties remove ben's High Mage before playtest turn 3 | Playtest can't finish as written | Derive turn 3 from the actual seed-42 state on a running server; script whichever army wins |
| Bonus literals in battle tests drift from `GameConfig` | Tests pass with the wrong defaults | T1 adds a test pinning `GameConfig()` to the spec values |
| `WebRoutesTests` uses `victoryPointsToWin = 5` in a hand-built view | Looks stale | It's a fixture independent of the config and belongs to the web UI work; leave it |
| Gradle needs `~/.gradle`, which the sandbox can't write | Agent-run builds fail | Run Gradle outside the sandbox (prompted) |

## Open questions
None blocking. The balance questions to watch in play are in SPEC.md's Open Questions.
