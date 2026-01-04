# TASK: Implement and Align Card Types with Design Document

### 1. Objective
To refactor and complete the implementation of the `Card` hierarchy to precisely match the specifications in the `@/src/main/kotlin/org/battleofkingdoms/cards/ai-docs/CARD_DESIGN.md` document, focusing on resource and creature cards.

### 2. Architectural Context
- **Layer:** Domain Layer
- **Primary File(s):** All files within `src/main/kotlin/org/battleofkingdoms/cards/`
- **Description:** The `Card` entities are fundamental to the domain layer. This task ensures they are correctly implemented with the specified attributes, costs, and behaviors before they are integrated into the game logic.
- **Reference:** `@ai-docs/SYSTEM_ARCHITECTURE.md` (Domain Layer)

### 3. Relevant Files for Context
- **To be modified:**
  - `src/main/kotlin/org/battleofkingdoms/cards/Resource.kt`
  - `src/main/kotlin/org/battleofkingdoms/cards/Food.kt`
  - `src/main/kotlin/org/battleofkingdoms/cards/Wood.kt`
  - `src/main/kotlin/org/battleofkingdoms/cards/Iron.kt`
  - `src/main/kotlin/org/battleofkingdoms/cards/Creature.kt`
  - `src/main/kotlin/org/battleofkingdoms/cards/Horde.kt`
  - `src/main/kotlin/org/battleofkingdoms/cards/Shield.kt`
  - `src/main/kotlin/org/battleofkingdoms/cards/Bow.kt`
- **To be created:**
  - `src/test/kotlin/org/battleofkingdoms/cards/CardTests.kt`

### 4. Implementation Plan (Iterative Steps)
1.  **Step 1: Refactor Resource Cards.**
    - Create a failing test in `CardTests.kt` to check if `Food` is a singleton.
    - Modify `Food.kt`, `Wood.kt`, and `Iron.kt` from `data class` to `object` to better represent them as resource types. This aligns the digital twin with the physical game where resource cards are interchangeable. Update `Resource.kt` if necessary.
2.  **Step 2: Generalize Upgrade Cost.**
    - Create a failing test that checks for a `Horde` in an upgrade cost.
    - Modify the `Creature` interface to change the `upgradeCost` map's key from `Resource` to the sealed `Card` type, allowing it to hold any card type as a cost component.
3.  **Step 3: Align Horde Card.**
    - Create a failing test for the `Horde` card's stats.
    - Update `Horde.kt` to match the design doc stats (Attack: 2, Defense: 1).
4.  **Step 4: Align Shield Card.**
    - Create a failing test for the `Shield` card's stats and full upgrade cost.
    - Update `Shield.kt` to match the design doc stats (Attack: 2, Defense: 5) and the full upgrade cost (1 Horde, 1 Food, 1 Iron).
5.  **Step 5: Align Bow Card.**
    - Create a failing test for the `Bow` card's stats and full upgrade cost.
    - Update `Bow.kt` to match the design doc stats (Attack: 4, Defense: 1, Trait: RANGED only) and the full upgrade cost (1 Horde, 1 Food, 1 Wood).
6.  **Step 6: Final Review.**
    - Review all modified card files to ensure they are consistent and correctly typed.
    - Ensure all tests in `CardTests.kt` pass.

### 5. Acceptance Criteria (Definition of Done)
- [ ] `Food`, `Wood`, and `Iron` are implemented as `object`s.
- [ ] The `Creature.upgradeCost` map is capable of holding any `Card` type as a key.
- [ ] `Horde`, `Shield`, and `Bow` data classes have the exact stats, traits, and upgrade costs specified in the `CARD_DESIGN.md`.
- [ ] Unit tests in `CardTests.kt` exist and pass, covering the stats and costs of each creature card.
- [ ] The project's build is successful (`./gradlew build`).
