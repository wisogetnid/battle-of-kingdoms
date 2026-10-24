package org.battleofkingdoms.cards

import org.battleofkingdoms.cards.ResourceType.IRON
import org.battleofkingdoms.cards.ResourceType.WOOD
import org.battleofkingdoms.cards.Trait.INFANTRY
import org.battleofkingdoms.cards.Trait.RANGED

enum class CreatureType(
    val attack: Int,
    val traits: Set<Trait>,
    val upgradeResource: ResourceType?,
    private val attackAgainstRanged: Int = attack,
) : Card {
    HORDE(attack = 1, traits = setOf(INFANTRY), upgradeResource = null),
    SHIELD(attack = 1, traits = setOf(INFANTRY), upgradeResource = WOOD, attackAgainstRanged = 2),
    BOW(attack = 2, traits = setOf(INFANTRY, RANGED), upgradeResource = IRON);

    val ranged: Boolean get() = RANGED in traits

    fun attack(facingRanged: Boolean): Int = when {
        facingRanged -> attackAgainstRanged
        else -> attack
    }
}
