Game Design Document: Battle of Kingdoms (Version 2.0)**

#### **1. Game Overview**

*   **Game Title:** Battle of Kingdoms
*   **Genre:** Multiplayer Strategy, Deck-Building, Resource Management
*   **Player Count:** 2-4 Players
*   **Core Concept:** Players are rulers who draw cards from shared decks to build armies and manage resources. They engage in strategic battles, where army composition is key to victory and minimizing losses. The goal is to claim powerful artifacts and end the game with the most victory points.
*   **Winning Condition:** The player with the highest total score, calculated from artifacts owned, battles won, and the strength of their final army.

#### **2. Core Concepts & Entities**

##### **2.1. Central Game Components**

*   **2.1.1. Resource Deck (Shared)**
    *   **Description:** A single, shared deck containing all resource cards. All players draw from this deck.
    *   **Card Types:** `Food`, `Wood`, `Iron`.
*   **2.1.2. Creature Deck (Shared)**
    *   **Description:** A single, shared deck containing all creature cards. All players draw from this deck.
    *   **Card Types:** `Horde`, `Shield`, `Bow`.
*   **2.1.3. Artifact Stack**
    *   **Description:** A predefined, ordered stack of Artifact cards. The top card is revealed each turn as the prize for that turn's battle.

##### **2.2. Card Types**

*   **2.2.1. Resource Cards**
    *   **Description:** Used to pay for creature upgrades. When drawn, they go into a player's hand. When spent, they are returned to a central discard pile.
*   **2.2.2. Creature Cards**
    *   **Description:** The units that form a player's army. Each creature has stats and traits.
    *   **Attributes:**
        *   `Name`: (e.g., "Horde", "Shield", "Bow")
        *   `Attack`: (Integer) Base damage dealt in battle.
        *   `Defense`: (Integer) Base health/damage absorption.
        *   `Trait`: (Enum: `INFANTRY`, `RANGED`) A keyword that determines combat effectiveness.
    *   **Creature Details & Upgrade Costs:**
        *   **Horde**
            *   `Trait`: `INFANTRY`
            *   `Attack`: 1, `Defense`: 1
        *   **Shield (Upgrade from Horde)**
            *   `Upgrade Cost`: 2 Wood, 1 Iron
            *   `Trait`: `INFANTRY`
            *   `Attack`: 1, `Defense`: 4
        *   **Bow (Upgrade from Horde)**
            *   `Upgrade Cost`: 1 Wood, 2 Iron
            *   `Trait`: `RANGED`
            *   `Attack`: 3, `Defense`: 1
*   **2.2.3. Artifact Cards**
    *   **Description:** Unique items won in battle that provide powerful, passive bonuses.
    *   **Attributes:**
        *   `Name`, `EffectDescription`, `Bonus` (e.g., `{ "targetTrait": "RANGED", "stat": "Attack", "value": 2 }`)

##### **2.3. Player State**

*   `PlayerID`: Unique identifier.
*   `Hand`: A list of cards (Creature and Resource) the player currently holds. This is the player's primary pool of assets.
*   `GameStats`:
    *   `BattlesWon`: (Integer)
    *   `VictoryPoints`: (Integer)

##### **2.4. Game State**

*   `Players`: A list of all `Player State` objects.
*   `CurrentTurn`: (Integer) The current turn number (e.g., 1 through 5).
*   `MaxTurns`: (Integer) Set to 5.
*   `CurrentPhase`: (Enum: `BUILD_UP`, `BATTLE`).
*   `TurnArtifact`: The `ArtifactCard` available to be won in the current turn.

#### **3. Game Flow & Turn Structure**

##### **3.1. Game Setup**

1.  The shared `ResourceDeck` and `CreatureDeck` are created and shuffled.
2.  Each player starts with an empty `Hand`.
3.  Each player draws an initial hand: 3 Creature cards and 2 Resource cards.

##### **3.2. Turn Sequence (Repeated `MaxTurns` times)**

**Turn Start:**
1.  Increment `CurrentTurn`. Reveal the `TurnArtifact` from the Artifact Stack.
2.  Each player draws one card from the `ResourceDeck` and one card from the `CreatureDeck`, adding them to their `Hand`.

**Phase 1: Build-Up (`BUILD_UP`)**
*   **Player Actions:** Players can perform the following actions in any order, as many times as they wish:
    *   **Upgrade Creature:**
        1.  Select one `Horde` card from their `Hand`.
        2.  Select the required Resource cards from their `Hand` to pay the `Upgrade Cost`.
        3.  Discard the `Horde` and the spent Resource cards.
        4.  Take a corresponding `Shield` or `Bow` card from the general supply and add it to their `Hand`.
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

##### **3.3. Game End & Scoring**

*   **Scoring Calculation:**
    *   **Artifacts:** 2 VP for each `ArtifactCard` in the player's final `Hand`.
    *   **Battles Won:** 1 VP for each battle won.
    *   **Standing Army:** 1 VP for every 5 total `Attack` power across all creatures in a player's final `Hand`.

#### **4. Key Areas for Game Balancing**

*   **Survivor Formula:** The exact formula for calculating which and how many creatures survive a battle is the most critical balancing point. This will require extensive playtesting to feel fair and strategic.
*   **Card Ratios:** The ratio of Resource to Creature cards in the shared decks will heavily influence the game's economy and pacing.
*   **Contextual Bonuses:** The specific values for trait-based combat bonuses must be carefully tuned to ensure no single strategy becomes dominant.
*   **Artifact Power:** Artifacts should be powerful and desirable, but not so powerful that a single artifact win guarantees victory.
