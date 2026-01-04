# Game Design Document: Battle of Kingdoms (Version 2.0)

## 1. Game Overview

*   **Game Title:** Battle of Kingdoms
*   **Genre:** Multiplayer Strategy, Deck-Building, Resource Management
*   **Player Count:** 2-4 Players
*   **Core Concept:** Players are rulers who draw cards from shared decks to build armies and manage resources. They engage in strategic battles, where army composition is key to victory and minimizing losses. The goal is to claim powerful artifacts and end the game with the most victory points.
*   **Winning Condition:** The player with the highest total score, calculated from artifacts owned, battles won, and the strength of their final army.

## 2. Core Concepts & Entities

### 2.1. Central Game Components

*   **2.1.1. Horde Deck (Shared)**
    *   **Description:** A shared deck containing `Horde` cards, the base unit for all armies. Players draw from this deck every turn to build up their manpower.
*   **2.1.2. Resource Deck (Shared)**
    *   **Description:** A single, shared deck containing all resource cards used for upgrades. All players draw from this deck.
    *   **Card Types:** `Food`, `Wood`, `Iron`.
*   **2.1.3. General Supply (Shared)**
    *   **Description:** A shared, finite pool of elite creature cards (`Shield`, `Bow`). These cards are not drawn but are taken from the supply when a player upgrades a `Horde`.
*   **2.1.4. Artifact Stack**
    *   **Description:** A predefined, ordered stack of Artifact cards. The top card is revealed each turn as the prize for that turn's battle.

### 2.2. Card Types

For a detailed breakdown of all card stats, costs, and attributes, see the [@/src/main/kotlin/org/battleofkingdoms/cards/ai-docs/CARD_DESIGN.md](@/src/main/kotlin/org/battleofkingdoms/cards/ai-docs/CARD_DESIGN.md) document.

### 2.3. Player State

*   `PlayerID`: Unique identifier.
*   `Hand`: A list of cards (Creature and Resource) the player currently holds. This is the player's primary pool of assets.
*   `GameStats`:
    *   `BattlesWon`: (Integer)
    *   `VictoryPoints`: (Integer)

### 2.4. Game State

*   `Players`: A list of all `Player State` objects.
*   `CurrentTurn`: (Integer) The current turn number (e.g., 1 through 5).
*   `MaxTurns`: (Integer) Set to 5.
*   `CurrentPhase`: (Enum: `BUILD_UP`, `BATTLE`).
*   `TurnArtifact`: The `ArtifactCard` available to be won in the current turn.

## 3. Game Flow & Turn Structure

### 3.1. Game Setup

1.  The shared `ResourceDeck` and `HordeDeck` are created and shuffled.
2.  Each player starts with an empty `Hand`.
3.  Each player draws an initial hand: 3 `Horde` cards and 2 Resource cards.

### 3.2. Turn Sequence (Repeated `MaxTurns` times)

**Turn Start:**
1.  Increment `CurrentTurn`. Reveal the `TurnArtifact` from the Artifact Stack.
2.  Each player draws one card from the `ResourceDeck` and one `Horde` card, adding them to their `Hand`.

**Phase 1: Build-Up (`BUILD_UP`)**
*   **Player Actions:** Players can perform the following actions in any order, as many times as they wish:
    *   **Upgrade Creature:**
        1.  Choose an elite creature to build (e.g., `Shield` or `Bow`).
        2.  Pay the full `Upgrade Cost` by selecting the required cards (e.g., one `Horde`, one `Food`, one `Iron` for a Shield) from their `Hand`.
        3.  Discard the spent cards.
        4.  Take the corresponding elite creature card from the general supply and add it to their `Hand`.
    *   **Trade:** Players may trade cards from their `Hand` with other players.

**Phase 2: Battle (`BATTLE`)**
*   **Action 1: Army Formation**
    *   Each player secretly assembles an `Army` by selecting a subset of `CreatureCards` and `ArtifactCards` from their `Hand`.
    *   Creatures and Artifacts not selected for the army remain safely in the player's `Hand`.

*   **Action 2: Battle Resolution**
    1.  All armies are revealed simultaneously.
    2.  For each battle between two armies, a `BattleResult` is calculated.
    3.  **Contextual Strength Calculation:** The total `Attack` of an army is calculated not just by summing the base stats, but by applying bonuses based on creature traits. The formula is `Army A Strength = sum(creature.attack) + sum(bonuses from Army A's artifacts) + sum(contextual_bonuses(Army A vs Army B))`.
        *   *Example Contextual Bonus: `RANGED` creatures gain +2 Attack when fighting an army that contains only `INFANTRY` creatures.*
    4.  The army with the higher final `Attack` score wins the battle. Ties are resolved by the higher `Defense` score.
    5.  The winner receives the `TurnArtifact` card, adding it to their `Hand`. Their `BattlesWon` stat is incremented.

*   **Action 3: Post-Battle Casualties & Recovery**
    1.  **Casualties are calculated for all participating armies.** Creatures lost in battle are permanently removed from the game (e.g., moved to a "graveyard" pile).
    2.  The surviving creatures from each army are returned to their owner's `Hand`.
    3.  **The formula for determining survivors is a key balancing mechanic.**
        *   **Intent for Winner:** The winner should suffer some losses to prevent a snowball effect, but will always retain a significant portion of their army.
        *   **Intent for Loser:** The loser suffers heavier casualties but should retain a core of their army, allowing them to remain competitive in future turns.

**Turn End:**
1.  Check if `CurrentTurn` equals `MaxTurns`. If so, proceed to Game End.

### 3.3. Game End & Scoring

*   **Scoring Calculation:**
    *   **Artifacts:** 2 VP for each `ArtifactCard` in the player's final `Hand`.
    *   **Battles Won:** 1 VP for each battle won.
    *   **Standing Army:** 1 VP for every 5 total `Attack` power across all creatures in a player's final `Hand`.

## 4. Key Areas for Game Balancing

*   **Survivor Formula:** The exact formula for calculating which and how many creatures survive a battle is the most critical balancing point. This will require extensive playtesting to feel fair and strategic.
*   **Card Ratios:** The ratio of Resource to Creature cards in the shared decks will heavily influence the game's economy and pacing.
*   **Contextual Bonuses:** The specific values for trait-based combat bonuses must be carefully tuned to ensure no single strategy becomes dominant.
*   **Artifact Power:** Artifacts should be powerful and desirable, but not so powerful that a single artifact win guarantees victory.
