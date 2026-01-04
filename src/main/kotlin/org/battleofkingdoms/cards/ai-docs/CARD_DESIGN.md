# Card Design Living Documentation

This document outlines the design of the card hierarchy and game economy for *Battle of Kingdoms*. It reflects a **Limited Supply** model where players compete for specialized units to build a synergistic army.

---

## 1. Core Hierarchy

The card system is built on a `sealed interface` named `Card`. This ensures a type-safe environment where only authorized card branches exist.

* **The Economy (`Resource`):** Managed through a shared deck. Players draw these randomly to fuel their kingdom.
* **The Army Base (`Horde`):** Acts as the "Manpower" resource. Every player receives a baseline of Hordes to ensure they always have a presence on the battlefield.
* **The Elite Supply (`Creature`):** Specialized units (Shields, Bows) exist in a **Finite Global Supply**. They are not drawn but are "crafted" by upgrading a Horde.
* **The Artifacts (`Artifact`):** Unique prizes won in battle that stay in the player's hand to provide persistent global buffs.

---

## 2. Card Implementations

### 2.1. Resource Cards
*   **Description:** Used to pay for creature upgrades. When drawn, they go into a player's hand. When spent, they are returned to a central discard pile. `Food` is a universal resource required for all unit upgrades.
*   **Types:** `Food`, `Wood`, `Iron`.
*   **Implementation Note:** To keep the behavior of the digital twin close to the physical card game, where any `Wood` card is interchangeable with another, these cards are implemented as `object` singletons rather than `data class` instances.

### 2.2. Base Unit Card (Manpower)
*   **Description:** The foundational unit of an army, used both for fighting and as a requirement for creating elite units. Drawn each turn from the `Horde Deck`.
*   **Horde**
    *   `Trait`: `INFANTRY`
    *   `Attack`: 2, `Defense`: 1

### 2.3. Elite Creature Cards
*   **Description:** Specialized units that form the core of a player's army. These are not drawn, but are taken from the `General Supply` by spending a `Horde` and resources.
*   **Attributes:**
    *   `Name`: (e.g., "Shield", "Bow")
    *   `Attack`: (Integer) Base damage dealt in battle.
    *   `Defense`: (Integer) Base health/damage absorption.
    *   `Trait`: (Enum: `INFANTRY`, `RANGED`) A keyword that determines combat effectiveness.
*   **Creature Details & Upgrade Costs:**
    *   **Shield (Upgrade from Horde)**
        *   `Upgrade Cost`: 1 Horde, 1 Food, 1 Iron
        *   `Trait`: `INFANTRY`
        *   `Attack`: 2, `Defense`: 5
    *   **Bow (Upgrade from Horde)**
        *   `Upgrade Cost`: 1 Horde, 1 Food, 1 Wood
        *   `Trait`: `RANGED`
        *   `Attack`: 4, `Defense`: 1

### 2.4. Artifact Cards
*   **Description:** Unique items won in battle that provide powerful, passive bonuses. The detailed implementation of the bonus structure is currently deferred.
*   **Attributes:**
    *   `Name`, `EffectDescription`, `Bonus` (e.g., `{ "targetTrait": "RANGED", "stat": "Attack", "value": 2 }`)

---

## 3. Technical Implementation Details (Kotlin)

### 3.1. Upgrade System Logic
To enforce the "Limited Supply" mechanic, the game engine must validate the `GlobalSupply` before processing an upgrade.

```kotlin
/**
 * Logic flow for converting a basic unit into an elite unit.
 */
fun attemptUpgrade(player: Player, targetId: String) {
    val template = CardRegistry.getTemplate(targetId)
    val cost = template.upgradeCost ?: return

    if (player.hand.has(Horde) && player.hand.canAfford(cost)) {
        if (GlobalSupply.isAvailable(targetId)) {
            player.hand.remove(Horde)
            player.hand.pay(cost)
            player.hand.add(GlobalSupply.take(targetId))
        } else {
            throw IllegalStateException("Supply for $targetId is depleted!")
        }
    }
}
```

### 3.2. Combat Trait Resolution
Traits are checked during the BattleResult calculation to apply contextual bonuses.

```kotlin
// Example logic for the Battle Engine
if (attacker.traits.contains(Trait.RANGED) && !defender.traits.contains(Trait.SHIELD)) {
    bonusAttack += 2 
}
```

## 4. Balancing Variables to Test
Supply Ratio: Total number of Elite units available vs. number of players.

Casualty Rate: Percentage of the army lost by the winner vs. the loser.

Resource Density: The frequency of Iron/Wood in the deck compared to Food.
