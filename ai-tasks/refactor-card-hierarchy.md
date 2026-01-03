# TASK: Refactor Card Hierarchy

### 1. Objective
To refactor the card data structures to align with the specifications in `@ai-docs/GAME_DESIGN.md`. This involves establishing a clear, sealed hierarchy for card types and implementing each specific card with its correct attributes.

### 2. Context
This is the first implementation step from the parent task `align-codebase-with-design-docs.md`. The current card implementation is outdated and does not reflect the game design.

### 3. Implementation Plan

1.  **Step 1: Create Failing Unit Tests**
    - Create a new test file: `src/test/kotlin/org/battleofkingdoms/cards/CardTests.kt`.
    - Add unit tests that assert the correct attributes for each creature card based on `GAME_DESIGN.md`:
        - **Horde:** `attack`=1, `defense`=1, `trait`="INFANTRY".
        - **Shield:** `attack`=1, `defense`=4, `trait`="INFANTRY", `upgradeCost`={Wood: 2, Iron: 1}.
        - **Bow:** `attack`=3, `defense`=1, `trait`="RANGED", `upgradeCost`={Wood: 1, Iron: 2}.
    - Run the tests and confirm they fail to compile, as the properties (`.attack`, `.defense`, etc.) do not yet exist on the current classes.

2.  **Step 2: Define the Core Card Hierarchy**
    - Modify `src/main/kotlin/org/battleofkingdoms/cards/Card.kt` to be a `sealed interface`.
    - Create `src/main/kotlin/org/battleofkingdoms/cards/Creature.kt` as an interface inheriting from `Card`. Define the required properties: `attack: Int`, `defense: Int`, `trait: String`, `upgradeCost: Map<Resource, Int>`.
    - Create `src/main/kotlin/org/battleofkingdoms/cards/Resource.kt` as an interface inheriting from `Card`.
    - Create `src/main/kotlin/org/battleofkingdoms/cards/Artifact.kt` as an interface inheriting from `Card`.
    - Move `Creature.kt` and `Resource.kt` from their sub-packages into the `cards` package to satisfy the `sealed` interface requirement.

3.  **Step 3: Implement Creature Cards**
    - Refactor `Horde.kt`, `Shield.kt`, and `Bow.kt` to be `data class`es that implement the `Creature` interface.
    - Add the properties defined in the interface with the correct values from the game design document.

4.  **Step 4: Implement Resource Cards**
    - Refactor `Wood.kt`, `Iron.kt`, and `Food.kt` to be `data class`es that implement the `Resource` interface.

5.  **Step 5: Clean Up and Verify**
    - Delete the now-unused directory and files: `src/main/kotlin/org/battleofkingdoms/cards/creatures/traits/`.
    - Run the tests in `CardTests.kt` and ensure they all pass.
    - Fix any compilation errors in other files that arise from these changes (e.g., incorrect import paths).

### 4. Definition of Done
- [ ] `Card.kt` is a sealed interface.
- [ ] `Creature.kt`, `Resource.kt`, and `Artifact.kt` interfaces are defined and inherit from `Card`.
- [ ] `Horde`, `Shield`, `Bow`, `Wood`, `Iron`, and `Food` are implemented as data classes with the correct attributes.
- [ ] The `traits` directory is removed.
- [ ] All tests in `CardTests.kt` pass.
