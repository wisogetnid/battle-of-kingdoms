# Plan: Artifacts (Iteration 5)

Spec: [SPEC.md](../SPEC.md) · Tasks: [todo.md](todo.md)

Iterations up to 4 are delivered and their plans retired; git history keeps them. The web UI
has its own plan in [web-ui/](web-ui/); this plan only keeps it compiling. A playtest
rebalance of the bonus table (Phase 0) lands before the artifact work.

## Architecture decisions
1. **Artifacts are an enum in `cards/`.** `Artifact { RESILIENT_BANNER, AMULET_OF_LUCK, CROWN }`
   is not a `Card` (it never sits in a hand). `Player.artifacts: Set<Artifact>` holds what a
   player owns.
2. **The game awards VP, not the battle.** `Battle.resolve` keeps duels, scores, winner and
   casualties, and stops taking a pot. `Game` decides the prize and records it on the log
   entry (`BattleResult.prize`, the winner's `vpGained`). This moves first (T1), together with
   the end of the rollover, so the claim (T4) only adds a choice.
3. **The stack is a set, drawn at random.** `Game.artifactStack: Set<Artifact>` starts from
   `config.artifacts`. A draw picks uniformly from the set with the game's `Random` — the
   same distribution as a shuffled stack with a declined artifact shuffled back, without a
   stack order to keep. The `Random` is only consumed after a battle with a winner, so
   turn-1 deals don't change.
4. **CLAIM is a phase.** `Phase.CLAIM` and `Action.CLAIM` sit between BATTLE and the next
   turn. `Game.drawnArtifact` is hidden state like `commits`: only the winner's view shows
   it, and the log only ever says `VICTORY_POINT` or the name of an artifact that was taken.
   Claiming as anyone but the winner throws a new `NotTheWinner` (409).
5. **Casualty effects live in `Battle`.** `Battle.resolve` gets each player's artifacts and
   adjusts `losses(n, won)`: the Banner always floors, then the Amulet subtracts one (not
   below 0). Fewer casualties are drawn; the seeded draw stays a single shuffle.
6. **The Crown is income.** `payIncome` adds `config.crownHorde` for the Crown's owner.
   Claims happen before the next turn starts, so the Crown pays from the next turn on.
7. **Interim empty stack.** T4 adds the claim to the engine with `GameConfig.artifacts`
   defaulting to empty, so every win is an immediate VP and the HTTP flows and scripted games
   stay green until the claim route exists (T5). T6 switches the default to all three and
   teaches the scripted games to claim.
8. **`pot` keeps its name** in `Game`, the view and the log, as the VP a battle is worth
   (always `victoryPointsPerBattle`), so the web UI keeps compiling and rendering.
9. **Six named bonuses, still one table.** The rebalance gives tier 1 and tier 2 different
   same-tier counter bonuses and adds tier 2 against any other tier-1 unit, which four values
   can't express. `Bonuses` takes six named values (one per non-zero cell) and keeps the
   single `against(attacker, target)` lookup; `GameConfig` keeps every number. Only
   `Bonuses.kt` and `GameConfig.kt` change in production code; the web UI prints whatever
   strengths the view reports, so it needs nothing.
10. **The web UI is only kept compiling.** Adding `Phase.CLAIM` breaks the exhaustive `when`s
   in `web/Pages.kt`; T4 adds plain `CLAIM` branches there and nothing else. The claim form
   and artifact display are the web UI agent's work.

## Dependency graph
```
B1 new bonus table in the engine
 └─ B2 bonus docs + playtest comments
B3 only committed armies fight (independent of B1/B2)
B1, B3 ─▶ T1 (T1 rewrites the same BattleTests, so they land first)

T1 a tie gives nothing; the game awards the VP
 ├─ T2 Banner + Amulet casualties
 ├─ T3 Crown income
 └─ T4 claim in the engine (interim empty stack)
     └─ T5 claim over HTTP + view
         └─ T6 default stack of three; full games take and decline artifacts
             └─ T7 playtest + docs
```
T2 and T3 only need `Player.artifacts` and are independent of T4–T5; they run in sequence
because they share `Game.kt`.

## Phases and checkpoints

### Phase 0 — Playtest rebalance
**B1. New bonus table.** Tier 1 +1 vs Horde and the tier-1 unit it counters; tier 2 +4 vs
Horde and the tier-1 unit it counters, +2 vs any other tier-1 unit and the tier-2 unit it
counters. Six named values in `Bonuses` and `GameConfig`; every table cell, the worked
example and the equal-cost duel re-derived.

**B2. Docs and playtest.** README table and examples, one-pager note, playtest comments
recomputed for battles 2 and 3 (same winners, so casualties and hands don't change).

**B3. Only committed armies fight.** Empty armies sit out of the round robin (score 0, no
duels, nobody scores against them); a lone committer wins uncontested. Only `Battle.resolve`
changes, plus a "sat out" line in the web UI's battle log.

**Checkpoint 0:** build green; SPEC.md, README and the playtest show the same table; the
playtest replays as commented.

### Phase 1 — Rules in the engine
**T1. A tie gives nothing.** Every battle is worth `victoryPointsPerBattle`; no rollover;
`Game` awards the VP.

**T2. Resilient Banner and Amulet of Luck.** `Artifact`, `Player.artifacts`, adjusted
casualties; the spec's casualty table as a parameterized test.

**T3. Crown.** Extra Horde in income for its owner.

**Checkpoint A:** build green; every artifact effect has a unit test.

### Phase 2 — The claim
**T4. Claim in the engine.** Stack, draw, CLAIM phase, `claim`, prize in the log, empty stack
→ immediate VP, game over after a VP prize.

**T5. Claim over HTTP.** Route, request, view fields, hidden-information API tests.

**T6. Three artifacts by default.** Default stack; `FullGameTests` and both end-to-end games
take one artifact and decline at least one, with a tie, a trade, a tier-2 unit, to 3 VP.

**Checkpoint B:** build green; criteria 1–6, 8 and 9 met.

### Phase 3 — Play and docs
**T7. Playtest and docs.** `http/playtest.http` to `GAME_OVER` with a tie, a trade, a
tier-2 unit, an artifact taken, one declined and a visible effect; README, one-pager note
(artifact powers are no longer "Not Doing"), vision doc.

**Checkpoint C:** all 10 SPEC.md success criteria met.

## Risks and mitigations
| Risk | Impact | Mitigation |
|---|---|---|
| Tier 1 + Horde now only ties 3 Horde (3 = 3) | Tier 1 looks pointless against Horde | Intended by the playtest: tier 1 pays off against the unit it counters; the spec's equal-cost rule says so; watch in play |
| B1's new numbers shift seeded outcomes T1–T7 re-derive | Scripts written against the old table | B1 lands first; the scripted games already react to the deals and stayed green under B1 |
| The drawn artifact leaks to other players or into the log | Breaks the core of the mechanic | `drawnArtifact` only on `MyView` and only for the winner during CLAIM; API test scans every other view and the log for the declined artifact's name |
| `Phase.CLAIM` breaks the web UI build | Other agent's work stops compiling | T4 adds plain `CLAIM` branches in `web/Pages.kt` in the same commit; nothing more |
| Adding the claim stalls the scripted HTTP games (no route yet) | E2E red between tasks | Interim empty stack in T4 (decision 7) |
| New `MyView` / `OtherPlayerView` fields break web UI tests that build views by hand | Other agent's tests stop compiling | New fields go last, with defaults |
| The artifact draw shifts the seeded `Random` | Seeded games change | Scripts react to the seed; re-derive, never patch |
| `apiTest` always uses the default config | API tests can't claim before T6 | `module()` takes an optional `GameServer`; `apiTest` takes an optional config |
| Gradle needs `~/.gradle`, which the sandbox can't write | Agent-run builds fail | Run Gradle outside the sandbox (prompted) |

## Open questions
None blocking. Balance questions to watch in play are in SPEC.md's Open Questions.
