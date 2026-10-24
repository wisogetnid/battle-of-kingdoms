# Plan: Core Loop MVP (Stage 1)

Implements [SPEC.md](../SPEC.md). Task checklist: [todo.md](todo.md).

## Architecture decisions
1. **Cards become value types.** `CreatureType` (HORDE, SHIELD, BOW: attack, ranged,
   upgrade resource) and `ResourceType` (WOOD, IRON) enums replace the per-card classes'
   behaviour. A `Hand` holds counts per type. Reason: the API speaks in counts
   (`{ "HORDE": 2 }`), equality-by-class hacks go away, and random casualties are a
   simple draw from an expanded list. `Food` is removed.
2. **Strength takes all other armies.** `Army.strength(otherArmies: List<Army>)` so Shield's
   bonus ("any other committed army has a Ranged creature") is local to the army.
3. **Battle resolution is a pure function** —
   `Battle.resolve(commits: Map<String, Army>, pot: Int, random: Random): BattleResult`
   — independent of `Game`, so every rule is testable in isolation.
4. **`Game` is an immutable data class** holding config, phase, turn, pot, players,
   battle log, and a seeded `Random`. Every action returns a new `Game` or throws a
   `GameException` (`RuleViolation`, `WrongPhase`, `AlreadyActed`, `UnknownPlayer`, ...).
   `Random` isn't a value, so determinism is tested by replaying the same seed + actions,
   not by comparing `Game` instances.
5. **No new dependencies.** Error → status mapping is a small wrapper in the routing
   layer, not Ktor's StatusPages plugin (that would be a new artifact: ask first).
6. **`GameServer` stays the in-memory registry** (`Map<UUID, Game>`), now with
   create/join/act methods that look up the game, apply the engine function, and
   store the result.

## Dependency graph
```
T1 toolchain
 └─ T2 card model + strength
     └─ T3 GameConfig + Game + lobby + income
         ├─ T4 build-up (upgrade, ready)
         │   └─ T6 battle wiring (commit, resolve, renown, next turn, game over)
         │        ▲
         │   T5 Battle.resolve (pure) ── depends on T2 only, parallel to T3/T4
         └─ T7 API: create / join / view
              └─ T8 API: upgrade / ready / army      (needs T4, T6)
                   └─ T9 HTTP e2e: 3p + 5p, determinism
                        └─ T10 playtest.http + manual run
```

## Phases and checkpoints

### Phase 0 — Foundation
**T1. Toolchain and launch configuration.** `.mise.toml` pins Java 21 (Temurin);
`application.conf` module path and `mainClass` point to `org.battleofkingdoms.server`.
Existing tests stay green.

**Checkpoint A:** `mise install && ./gradlew build` green; `./gradlew run` serves
`GET /game` on :8080.

### Phase 1 — Engine (pure, unit-tested)
**T2. Card model and army strength.** Enums + `Hand` + new `Army`; Bow costs Iron;
Shield bonus against other armies. `CardTests` and `ArmyTests` updated to the new model.

**T3. GameConfig, Game state, lobby, and income.** `create(config, seed, players, host)`,
`join(name)` with duplicate/full/count checks; when full → turn 1 income
(`startingHorde + baseCreaturesPerTurn + renown` Horde, `resourcesPerTurn` seeded 50/50
resources), pot 1, phase `BUILD_UP`. Old `Game`, `Player`, `Board` replaced;
`GameServer` adapted; `EndToEnd2PlayerUnitTest` deleted (rewrite comes in T9, as the
spec allows).

**T4. Build-up.** `upgrade(name, into)` and `ready(name)` with all error cases;
all ready → `BATTLE`.

**T5. Battle resolution (pure).** Strength per army, single winner / tie / all-zero /
single committer, pot payout vs rollover, floor/ceil casualties drawn from the seeded
`Random`, survivors, renown flags, log entry. Replaces two-army `Battle`,
`BattleResult`, and `BattleTests`.

**T6. Battle wiring and turn loop.** `commitArmy(name, army)` validates ownership, hides
commits until all are in, then applies `Battle.resolve`: VP, renown, survivors back to
hands, log appended; next turn (pot + 1, income with renown) or `GAME_OVER`. Includes an
engine-level full 3-player game test and a same-seed replay test.

**Checkpoint B:** a full seeded 3-player game, including a rollover, plays to
`GAME_OVER` through engine calls alone; replaying it yields an identical battle log.
Review the engine API before building routes on it.

### Phase 2 — HTTP API (vertical slices: route + DTO + tests)
**T7. Create, join, view.** `POST /games`, `POST /games/{id}/players`,
`GET /games/{id}?player=`; player-view DTO with `availableActions`; error wrapper (404/409/400). Replaces the
TODO routes and `ApplicationKtTests`. Asserts that one player's view never contains
another's hand.

**T8. Upgrade, ready, army.** The three action routes with their error statuses;
asserts that a committed army is invisible to others until resolution, then appears in
the battle log.

**T9. HTTP end-to-end.** Seeded 3-player and 5-player games over HTTP, each with a forced
tie (equal commits) and played to a winner; same-seed replay over HTTP gives identical
logs.

**T10. Playtest script.** `http/playtest.http` plays a 3-player game through two battles;
run it manually against `./gradlew run`.

**Checkpoint C:** all nine success criteria in SPEC.md checked off.

## Risks and mitigations
| Risk | Impact | Mitigation |
|---|---|---|
| The asdf hook in `~/.zshrc` (line 82) resets `JAVA_HOME` before every prompt, overriding mise | T1 builds fail in your shell even with mise | Remove that line before T1; T1 verifies `echo $JAVA_HOME` points into mise |
| `mise install` needs network access (blocked in the agent sandbox) | T1 can't finish unattended | Run `mise install` yourself, or approve the network access when asked |
| Gradle needs `~/.gradle`, which the sandbox can't write | Agent-run builds fail | Agent runs Gradle outside the sandbox (prompted), or adjust via `/sandbox` |
| Jackson can't deserialize Kotlin data classes / enum-keyed maps | T7/T8 requests fail | Check in T7 that `ktor-serialization-jackson` brings `jackson-module-kotlin`; if not, ask before adding it |
| T3 replaces `Game`/`Player`/`Board` in one go, breaking `GameServer` and the old e2e test | A large, red intermediate state | T3 adapts `GameServer` in the same task and deletes only the e2e test the spec lists for rewrite |
| Random casualties make tests brittle | Flaky or seed-coupled tests | Unit tests assert counts and invariants (survivors ⊆ committed); exact picks are asserted only in dedicated fixed-seed tests |

## Parallelism
T5 depends only on T2 and can run alongside T3/T4. Everything else is sequential.
For a single developer the order T1 → T10 is simplest.

## Open questions
None blocking. Record any rule change in SPEC.md first (spec boundary).
