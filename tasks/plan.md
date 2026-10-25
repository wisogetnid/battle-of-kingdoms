# Plan: Counter Triangle (Iteration 2)

Spec: [SPEC.md](../SPEC.md) · Tasks: [todo.md](todo.md)

Stage 1 (T1–T10) is delivered and its plan retired; git history keeps it.

## Architecture decisions
1. **Rename first, as its own slice.** "Artifact" → "victory point" (`victoryPointsToWin`,
   view field) touches config, view and tests but no rule. Doing it alone keeps the later
   rule diffs readable.
2. **Duels before creatures.** The round-robin scoring is the riskiest change, so it comes
   first and is proven with the existing Stage 1 creatures. `Army.duelStrength(opponent)`
   replaces `Army.strength(otherArmies)`; the Shield's bonus temporarily applies per duel.
   The creature swap then only changes *which* bonus applies, not *how* battles resolve.
3. **Counters live on the creature.** `CreatureType.counters: CreatureType?` (a `when`
   getter, since enum entries can't reference later entries in constructor arguments).
   `Army.duelStrength(opponent, counterBonus)` = size + `counterBonus` × the number of my
   creatures whose `counters` appears in the opponent's army. `Trait` is deleted.
4. **Battle stays pure and Game-independent:**
   `Battle.resolve(commits, pot, counterBonus, random): BattleResult`. Duels are the pairs
   `(i, j)` with `i < j` in join order; `BattleResult` gains `duels: List<Duel>` and each
   `ArmyResult` gets `score` instead of `strength`. Casualty draws keep their current order
   (per player, join order), after all duels.
5. **Resource draw:** `ResourceType.entries[random.nextInt(entries.size)]`. This changes every
   seeded sequence, so seeded expectations are re-derived per task, never patched to pass.
6. **The view mirrors the log:** `ArmyResultView.score`, `BattleLogEntryView.duels` as
   `{ players: [a, b], strengths: { a: x, b: y } }`, exactly as in the spec.

## Dependency graph
```
T1 rename artifact → victory point
 └─ T2 duels and scores (Stage 1 creatures)
     └─ T3 counter triangle + Essence (engine and all tests)
         └─ T4 e2e games with a counter-decided battle
             └─ T5 playtest.http + docs
```
Strictly sequential: each task changes code the next one builds on.

## Phases and checkpoints

### Phase 1 — Engine
**T1. Rename.** `artifactsToWin` → `victoryPointsToWin` in `GameConfig`, `Game`, the view DTO,
tests and README. No behavior change.

**T2. Duels and scores.** Round-robin resolution with summed margins, ties / all-zero /
single committer, the `duels` and `score` fields in the log and the view. Stage 1 creatures,
so existing end-to-end tests should keep their winners; re-derive them if not.

**T3. Counter triangle and Essence.** New creatures and costs, `counterBonus` in config,
`Trait` and `SHIELD` gone, 1/3 resource draws. Every test that names old units or relies on
seeded draws is rewritten; the spec's worked example and the going-wide case become tests.

**Checkpoint A:** `mise run build` green; every rule in SPEC.md's Game Rules has a unit test;
review the duel and counter code before touching the end-to-end games.

### Phase 2 — Games and play
**T4. End-to-end games.** Seeded 3- and 5-player games over HTTP, each with a rollover and at
least one battle decided by a counter; same-seed replay gives identical logs.

**T5. Playtest and docs.** `http/playtest.http` re-derived for the new seed outcomes, with one
battle decided by a counter; README rules and examples, the vision doc (Shield → triangle,
Essence adopted), and a note in the core-loop one-pager that Iteration 2 changed its rules.

**Checkpoint B:** all 9 SPEC.md success criteria met.

## Risks and mitigations
| Risk | Impact | Mitigation |
|---|---|---|
| Seeded expectations shift with the third resource | Tests "fixed" by tweaking numbers until green hide real bugs | Re-derive each scripted game from the rules, assert outcomes the script forces (ties, counters), not incidental numbers |
| Duel margins make one big army dominate 5-player games | Counters feel irrelevant | `counterBonus` is configurable; record the observation in SPEC Open Questions rather than changing defaults (ask first) |
| Scripted games can't force a counter win with seeded draws | T4/T5 criteria unmet | Script upgrades from whatever resources the seed deals; pick seeds whose deals allow it, documented in the test |
| Enum forward references for `counters` | Compile error or null at init | `when`-based getter, covered by a test per counter pair |
| Gradle needs `~/.gradle`, which the sandbox can't write | Agent-run builds fail | Agent runs Gradle outside the sandbox (prompted), or adjust via `/sandbox` |

## Open questions
None blocking. Record any rule change in SPEC.md first (spec boundary).
