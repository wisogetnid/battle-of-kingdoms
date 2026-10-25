# Battle of Kingdoms — Core Loop MVP

> **Note:** this one-pager is the original Stage 1 idea and is kept as written. Iteration 2
> changed its upgrade and battle rules: Wood / Iron / Essence upgrade into a Bow / Sword / Mage
> counter triangle, and battles resolve as duels between every pair of players. Iteration 3
> added a second upgrade tier and trading resources for Horde, and removed renown. Iteration 4
> made tier 2 decisive (+5 vs Horde, +3 vs the tier-1 unit it counters) and shortened the game
> to 3 VP. The current rules are in [SPEC.md](../../SPEC.md).

## Problem Statement
How might we get 3–5 friends playing Battle of Kingdoms digitally, with minimal tech,
so we learn within ~3 months whether the build-up → battle-for-the-artifact loop is
fun — and tune its balance?

## Recommended Direction
Strip the game to a single core loop and solve the multiplayer battle with **blind
bidding**. Each turn, every player receives one basic creature (Horde) plus one per
point of renown, draws two resources, and can spend resources to level creatures up (Wood → Shield, Iron → Bow).
An artifact is revealed, everyone secretly commits an army at the same time, and the
strongest army takes the artifact (1 victory point). **All** participants — winner
included — lose half of what they committed, with random casualties. Committing nothing
is allowed, but every battle fought earns a point of **renown**, which is permanent and
stacks: each point adds one creature to every future turn. A tie
means nobody wins and the artifact rolls over into a double pot. First to 3 artifacts
wins. This replaces the current two-army `first()`/`last()` battle in `Game.commitArmy`
with a mechanic that scales from 3 to 5 players with no pairing rules, and adds
bluffing and tension.

The Ktor server exposes the engine as a JSON HTTP API — that API is the contract.
**Stage 1** is playing it locally over HTTP (IntelliJ `.http` files / curl), plus an
end-to-end test that plays a full 3–5 player game, so the rules are proven before anyone
else touches it. **Stage 2** adds one thin client — a chat bot (Discord/Telegram, DMs for
hidden hands) or a minimal server-rendered page — chosen once Stage 1 works. Neither
choice rules out the other later.

Everything else in the README (marketplace, deeper upgrades, supporters, diplomacy)
waits. If the core isn't fun, those features won't save it; if it is, they'll be easier
to judge against a working baseline.

## Rules (MVP)
| Rule | Value |
|---|---|
| Players | 3–5 |
| Creatures per turn | 1 Horde + 1 Horde per renown point |
| Resources drawn per turn | 2 |
| Upgrades | Horde + Wood → Shield, Horde + Iron → Bow |
| Battle | Simultaneous secret commit; highest strength wins the artifact |
| Zero commit | Allowed — keeps everything, can't win |
| Losses | Half of committed creatures; losers round up, the winner rounds down |
| Casualties | Random among the committed creatures |
| Renown | Committed ≥1 creature → +1 renown, permanent and stacking |
| Tie | Nobody wins; artifact rolls over, next winner takes both (2 VP) |
| Victory | First to 3 artifacts |

All numbers live in one game-config object, not scattered constants, so balancing is
turning knobs instead of editing code. Randomness (casualties, card draw) comes from an
injected, seedable `Random` so tests are deterministic and battles replayable.

## Key Assumptions to Validate
- [ ] **Blind bidding creates real decisions** — not "always all-in" or "only the leader
      bids." Test: play all seats via HTTP; then 3+ friend sessions, ask "when did you
      hesitate?"
- [ ] **The loss rule produces strategy** — half-losses make overcommitting costly.
      Test: check that the cheapest winning bid isn't obvious in played games.
- [ ] **Stacking renown beats hoarding without breaking the game.** Sitting out now
      forfeits compounding income, but a token 1-creature commit is the cheapest way to
      earn renown, so expect everyone to commit at least 1 every turn. Test: check
      whether token commits become a no-brainer; fallback is renown requiring ≥2
      committed.
- [ ] **Creature counts stay manageable.** Renown grows income every turn, while
      resources stay flat at 2, so late-game armies may be mostly un-upgraded Horde.
      Test: track army sizes and the Horde ratio by turn; cap renown or scale resources
      if upgrades stop mattering.
- [ ] **Double pots are exciting, not game-deciding.** With 3 to win, a 2-artifact pot
      can end a game early. Test: count how often a rollover decides the winner.
- [ ] **"First to 3" ends a game in <30 min** with 3–5 players. Test: time real sessions.
- [ ] **Friends will actually play.** Test: zero-setup Stage 2 client; schedule one
      session within two weeks of Stage 1 finishing.

## MVP Scope
**In:**
- 3–5 players, joining via the HTTP API
- Turn loop: receive creatures + draw resources → upgrade → reveal artifact →
  secret simultaneous commit → resolve
- The rules table above, driven by a single game config
- JSON HTTP API replacing the TODO routes in `Application.kt`
- An end-to-end test playing a complete multiplayer game (seeded)
- Stage 2: one thin client (bot or page)

**Out:** everything under "Not Doing."

## Not Doing (and Why)
- **Marketplace / trading** — obscures whether the core loop works; human interaction
  comes free from bluffing for now
- **Upgrades beyond Horde → Shield/Bow** (weapons, armour, multi-level) — keep one tier
  until base numbers are tuned
- **Supporters, diplomacy, alliances** — the most complex multiplayer layer; only worth
  it on a fun core
- **Artifact powers / themed armies** — the most exciting next step, deliberately
  deferred: it introduces leader snowball, which needs its own design pass
- **Auth, persistence, deployment** — friends on localhost + a tunnel is enough;
  player names are trusted
- **Bot-simulated balancing** — balancing something unproven optimizes the wrong thing;
  revisit after friends confirm it's fun
- **Paper playtest** — skipped by choice; the HTTP stage plays that role

## Open Questions
- **Next iteration — themed artifacts without snowball.** Artifacts that let players
  theme their armies and help the winner. Candidate counters to leader snowball:
  - artifacts that power up a creature *type* (theming) rather than raw strength
  - catch-up draw for trailing players
  - the artifact holder publicly reveals part of their next commit
  - artifacts that can be stolen in later battles
- **Stage 2 client:** chat bot vs. minimal web page — decide after Stage 1
