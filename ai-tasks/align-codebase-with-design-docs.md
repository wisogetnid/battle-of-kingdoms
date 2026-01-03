# TASK: Align Codebase with Game Design v2.0 and System Architecture

### 1. Objective
To refactor the existing codebase to align with the specifications defined in `@ai-docs/GAME_DESIGN.md` and `@ai-docs/SYSTEM_ARCHITECTURE.md`, establishing a solid foundation for future feature development.

### 2. Architectural Context
- **Layer:** Primarily the Domain and Application layers.
- **Primary File(s):** `Game.kt`, `GameState.kt`, `Player.kt`, `Card.kt` (and its hierarchy), `Battle.kt`, `GameServer.kt`.
- **Description:** The current implementation blends application and domain logic and misrepresents core game concepts like decks and game phases. This refactoring will enforce the boundaries described in the architecture document, creating a pure Domain layer and a distinct Application layer that uses DTOs for communication.
- **Reference:** Sections 3 (Layered Architecture) and 4 (Interaction Model) in `@ai-docs/SYSTEM_ARCHITECTURE.md`.

### 3. Relevant Files for Context
#### To be modified:
- `src/main/kotlin/org/battleofkingdoms/game/Game.kt`
- `src/main/kotlin/org/battleofkingdoms/game/GameState.kt`
- `src/main/kotlin/org/battleofkingdoms/player/Player.kt`
- `src/main/kotlin/org/battleofkingdoms/cards/Card.kt`
- `src/main/kotlin/org/battleofkingdoms/cards/Creature.kt`
- `src/main/kotlin/org/battleofkingdoms/cards/Horde.kt`
- `src/main/kotlin/org/battleofkingdoms/cards/Shield.kt`
- `src/main/kotlin/org/battleofkingdoms/cards/Bow.kt`
- `src/main/kotlin/org/battleofkingdoms/cards/Resource.kt`
- `src/main/kotlin/org/battleofkingdoms/cards/Wood.kt`
- `src/main/kotlin/org/battleofkingdoms/cards/Iron.kt`
- `src/main/kotlin/org/battleofkingdoms/cards/Food.kt`
- `src/main/kotlin/org/battleofkingdoms/cards/Artifact.kt`
- `src/main/kotlin/org/battleofkingdoms/battle/Battle.kt`
- `src/main/kotlin/org/battleofkingdoms/server/GameServer.kt`
- `src/test/kotlin/org/battleofkingdoms/e2e/EndToEnd2PlayerUnitTest.kt`
- `src/test/kotlin/org/battleofkingdoms/cards/CardTests.kt`
- `src/test/kotlin/org/battleofkingdoms/battle/BattleTests.kt`
- `src/test/kotlin/org/battleofkingdoms/battle/ArmyTests.kt`

### 4. Implementation Plan (Iterative Steps)

1.  **[DONE] Step 1: Refactor Card Hierarchy.**
    - Create failing tests for card attributes.
    - Modify `Card.kt` and implement `Creature`, `Resource`, and `Artifact` base types.
    - Implement specific cards (`Horde`, `Shield`, `Bow`, `Wood`, `Iron`, `Food`) with correct attributes (attack, defense, traits, upgrade costs) as per `GAME_DESIGN.md`.
2.  **[PENDING] Step 2: Refactor GameState.**
    - Create a failing test for the new `GameState` structure.
    - Modify `GameState.kt` to include separate `resourceDeck`, `creatureDeck`, and `artifactStack`.
    - Add `currentTurn`, `maxTurns`, and `currentPhase` (`BUILD_UP`, `BATTLE`) properties.
3.  **[PENDING] Step 3: Refactor Player State.**
    - Create a failing test for player stats.
    - Modify `Player.kt` to remove `committedArmy` and `state`, and add `battlesWon` and `victoryPoints`.
4.  **[PENDING] Step 4: Refactor Game Logic for Turn Management.**
    - Create failing tests for the new turn flow.
    - Modify `Game.kt` to implement the turn sequence from the design doc: reveal artifact, draw one resource and one creature card per player.
5.  **[PENDING] Step 5: Refactor Battle Logic.**
    - Create failing tests for the specified battle survivor logic.
    - Modify `Battle.kt` to implement the casualty rules where the loser retains a core part of their army.
6.  **[PENDING] Step 6: Refactor `GameServer` and Introduce DTOs.**
    - Create a failing test that asserts `getGameState` returns a DTO, not a domain object.
    - Create `GameStateView` and `PlayerView` DTOs.
    - Modify `GameServer.kt` to mediate between the client and the `Game` domain object, mapping domain state to DTOs for responses.
    - Introduce structured commands (e.g., `CommitArmyCommand`) in the application layer.
7.  **[PENDING] Step 7: Update End-to-End Test.**
    - Modify `EndToEnd2PlayerUnitTest.kt` to reflect all the above changes, ensuring the game flow works as expected from the presentation layer's perspective.

### 5. Acceptance Criteria (Definition of Done)
- [ ] Card, Player, and GameState data structures match the `GAME_DESIGN.md` specification.
- [ ] The game turn flow (card drawing, phases) matches the `GAME_DESIGN.md` specification.
- [ ] Battle survivor logic is updated to be less punitive for the loser.
- [ ] The `GameServer` in the Application Layer exclusively returns DTOs to the client.
- [ ] The Domain layer (`Game`, `Player`, etc.) is pure and has no knowledge of the Application or Presentation layers.
- [ ] All existing and new unit tests pass.
- [ ] The `EndToEnd2PlayerUnitTest.kt` passes, demonstrating the new, aligned architecture
