# Battle of Kingdoms — Full Game Vision (beyond Stage 1)

Consolidated from the retired `ai-docs/` (Game Design v2.0, Card Design, High-Level Design,
System Architecture, Agent Workflow) and `ai-tasks/`. **[SPEC.md](../../SPEC.md) is the source
of truth** for what is built; this file only keeps ideas worth revisiting once the core loop
([battle-of-kingdoms-core-loop.md](battle-of-kingdoms-core-loop.md)) has proven fun.

Status legend: **replaced** — Stage 1 solves this differently · **deferred** — candidate for a
later stage · **dropped** — not planned.

## Game design ideas

| Idea (v2.0 design) | Status | Stage 1 instead / notes |
|---|---|---|
| 2–4 players, pairwise battles between armies | replaced | 3–5 players, one blind-bid battle for all |
| Shared, shuffled Horde and Resource decks | replaced | Infinite supply; income per turn, resources drawn 50/50 from the seeded `Random` |
| `Food` as a universal upgrade resource | dropped | Upgrades cost 1 Horde + Wood (Shield) or Iron (Bow) |
| **Limited supply**: finite shared pool of Shields and Bows players compete for | deferred | Elite units are unlimited; revisit as a scarcity lever once numbers are tuned |
| `Defense` stat; ties broken by higher Defense | dropped | Strength is attack only; ties roll the pot over |
| Ranged +2 attack against infantry-only armies | replaced | Shield attacks for 2 if another army has a Ranged creature |
| Winner loses some, loser keeps "a core" of its army | replaced | Winner loses `floor(n/2)`, everyone else `ceil(n/2)`, picked at random |
| **Artifact stack**: ordered, revealed each turn, each artifact with a passive bonus (e.g. +2 attack for Ranged) | deferred | Artifacts are plain VP in a pot. Themed artifacts without leader snowball is the main open question of the core-loop one-pager |
| Artifacts committed to armies alongside creatures | deferred | Depends on artifact powers |
| **Trading** cards between players during build-up | deferred | Marketplace is on the spec's "Never" list for Stage 1 |
| Fixed 5 turns, end-of-game scoring: 2 VP per artifact, 1 VP per battle won, 1 VP per 5 attack in the final hand | replaced | First to `artifactsToWin` VP wins |
| Balancing levers: survivor formula, deck ratios, trait bonuses, artifact power, supply ratio | replaced | All numbers live in `GameConfig`; tune them there |

Further flavour ideas (supporters, diplomacy, weapons/armour, Essence) live in the
[README](../../README.md).

## Architecture ideas

Adopted into SPEC.md: the three-layer architecture with a pure domain and DTO-only views, and
the list of available actions in the player view.

| Idea (System Architecture) | Status | Notes |
|---|---|---|
| Deterministic setup by passing exact deck orders and starting hands | replaced | A game is reproduced from config + seed + actions |
| "God-mode" observer endpoint with the unredacted game state | deferred | Useful for debugging and balancing; must never share a route with player views |
| Structured event stream (`CardDrawn`, `BattleResolved`, …) as JSON for analysis | deferred | Stage 1 has the battle log only |
| Scriptable AI players and mass simulation for balancing | deferred | "Bot-simulated balancing" is out until friends confirm the game is fun |
| Structured command objects (`CommitArmyCommand`, …) in the application layer | replaced | One engine function per action, one route per action |
| Timeouts for inactive players | deferred | Needed once real players can stall a game |
| Persistence (Redis, PostgreSQL) behind the `GameServer` registry | deferred | Games live in memory in Stage 1 |
| Asynchronous updates (WebSockets) for a real-time client | deferred | Decide with the Stage 2 client |

## Development workflow

`AGENT_WORKFLOW.md` (clarify → context packet in `ai-tasks/` → strict red-green-refactor) is
**replaced** by the spec-driven flow: `SPEC.md` → `tasks/plan.md` → `tasks/todo.md` →
incremental implementation with tests per task. Neither its clarify-first rule nor strict
test-first was adopted; the spec's "Ask first" boundaries cover decisions that need the user.
