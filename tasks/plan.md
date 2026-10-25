# Plan: Two Tiers and Trading (Iteration 3)

Spec: [SPEC.md](../SPEC.md) · Tasks: [todo.md](todo.md)

Stage 1 and Iteration 2 are delivered and their plans retired; git history keeps them.

## Architecture decisions
1. **Remove before adding.** Renown goes first, in its own slice, together with the new
   default of 5 VP. It touches player, battle result, config and view, and keeping it apart
   makes the later rule diffs readable.
2. **Trading is one engine function and one route.** `Game.trade(name, resource)` follows the
   shape of `upgrade`: phase and acted checks, then `hand - resource + HORDE`. The view gets a
   `TRADE` action. No new state.
3. **Tiers live on the creature.** `CreatureType` gets `tier` (0 for Horde, 1, 2) and `line`
   (a small `Line` enum: BOW, SWORD, MAGE, with `Line.counters`). `upgradesFrom` is derived:
   Horde for tier 1, the tier-1 unit of the same line for tier 2. The upgrade resource stays
   on the creature. `counters` on `CreatureType` is replaced by the line relation.
4. **"One tier per turn" is player state.** `Player.upgradedThisTurn: Map<CreatureType, Int>`
   counts units created by upgrades this turn. A tier-2 upgrade needs
   `hand.count(from) - upgradedThisTurn[from] >= 1`. It resets with income at turn start,
   and only the owner's view shows it.
5. **Bonuses are a pure function of two creature types plus a value object.**
   `Bonuses(tier1VsHorde, tier2VsHorde, sameTierCounter, higherTierCounter)` lives in
   `battle/` (so `battle/` keeps not depending on `game/`); `GameConfig` builds it. A
   creature's duel strength is `attack + max(bonus(it, t) for t in opponent)`.
   `Battle.resolve(commits, pot, bonuses, random)` replaces the `counterBonus` parameter.
6. **Interim bonus in T3.** Between adding tier-2 creatures (T3) and the bonus table (T4),
   tier-2 units use the existing line-based flat `counterBonus`, so every slice builds and
   passes. T4 then replaces only the bonus calculation.
7. **Seeded expectations are re-derived per task,** never patched until green. The
   end-to-end scripts react to whatever the seed deals, as in Iteration 2.

## Dependency graph
```
T1 remove renown, 5 VP
 └─ T2 trading
     └─ T3 tier-2 creatures + one tier per turn (interim flat bonus)
         └─ T4 tier bonus table (best bonus only, vs Horde, by tier)
             └─ T5 end-to-end games: rollover, trade, tier-2 unit, 5 VP
                 └─ T6 playtest.http + docs
```
T2 and T3 are independent in principle, but both touch `Game`, the view and the upgrade/
build-up tests, so they run in sequence.

## Phases and checkpoints

### Phase 1 — Engine
**T1. Remove renown, raise the target to 5 VP.** Equal income for everyone; no renown in
player, battle result, config, view, tests or README.

**T2. Trading.** `trade` in the engine, `POST …/trades`, the `TRADE` action, error cases.

**T3. Tier-2 creatures.** Warbow, Knight, High Mage with costs; `Line` and `tier`; the
one-tier-per-turn limit and `upgradedThisTurn` in my own view; interim flat counter bonus by
line.

**T4. Tier bonus table.** Best-bonus-only duel strength from `Bonuses`; bonuses against Horde;
lower tier gets nothing against higher; `counterBonus` removed; the spec's worked example and
every cell of the bonus table as unit tests.

**Checkpoint A:** `mise run build` green; every rule in SPEC.md's Game Rules has a unit test;
review the tier and bonus code before the end-to-end games.

### Phase 2 — Games and play
**T5. End-to-end games.** Seeded 3- and 5-player games over HTTP, each with a rollover, a
trade and a tier-2 unit in a committed army, played to 5 VP; same-seed replay gives
identical logs; `FullGameTests` covers the same at engine level.

**T6. Playtest and docs.** `http/playtest.http` re-derived (trade and a tier-2 unit in
battle 2 or later); README rules, tables and curl examples; vision doc (renown removed,
deeper upgrades partly adopted).

**Checkpoint B:** all 9 SPEC.md success criteria met.

## Risks and mitigations
| Risk | Impact | Mitigation |
|---|---|---|
| Random casualties remove the tier-1 unit a script planned to upgrade | E2E/playtest can't field a tier-2 unit | Upgrade into tier 1 in one turn and keep that unit home (don't commit it); upgrade it to tier 2 next turn |
| "Best bonus only" mis-implemented as a sum | Wrong strengths everywhere | Parameterized test per bonus-table cell plus explicit max-not-sum cases and the worked example |
| `upgradedThisTurn` leaks into other players' views | Reveals plans | API test asserts the field is absent from `others` |
| Horde bonus makes any committed Horde a liability | Going wide looks useless in play | Out of scope to change; recorded in SPEC Open Questions; tunable via `tier1VsHorde` / `tier2VsHorde` |
| Seeded expectations shift with new rules | Tests "fixed" until green hide bugs | Re-derive scripted games from the rules; assert what the script forces |
| Gradle needs `~/.gradle`, which the sandbox can't write | Agent-run builds fail | Agent runs Gradle outside the sandbox (prompted), or adjust via `/sandbox` |

## Open questions
None blocking. Record any rule change in SPEC.md first (spec boundary).
