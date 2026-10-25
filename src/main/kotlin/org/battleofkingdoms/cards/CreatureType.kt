package org.battleofkingdoms.cards

import org.battleofkingdoms.cards.ResourceType.ESSENCE
import org.battleofkingdoms.cards.ResourceType.IRON
import org.battleofkingdoms.cards.ResourceType.WOOD

/**
 * Every creature has attack 1. Each upgraded creature [counters] one other: Bow → Mage,
 * Sword → Bow, Mage → Sword. The Horde comes as income, counters nothing and is never countered.
 */
enum class CreatureType(val upgradeResource: ResourceType?) : Card {
    HORDE(upgradeResource = null),
    BOW(upgradeResource = WOOD),
    SWORD(upgradeResource = IRON),
    MAGE(upgradeResource = ESSENCE);

    val attack: Int get() = 1

    /** A getter, because enum entries can't reference later entries in their constructor arguments. */
    val counters: CreatureType?
        get() = when (this) {
            HORDE -> null
            BOW -> MAGE
            SWORD -> BOW
            MAGE -> SWORD
        }
}
