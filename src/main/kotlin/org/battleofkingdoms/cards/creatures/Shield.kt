package org.battleofkingdoms.cards.creatures

import org.battleofkingdoms.cards.creatures.traits.Infantry
import org.battleofkingdoms.cards.creatures.traits.Ranged
import org.battleofkingdoms.cards.resources.Resource
import org.battleofkingdoms.cards.resources.Wood
import org.battleofkingdoms.game.battle.Army

class Shield : Creature, Infantry {
    override fun attack(opposingArmy: Army): Int = when {
        opposingArmy.creatures.any { it is Ranged } -> 2
        else -> 1
    }
    override fun cost(): Map<Resource, Int> = mapOf(Wood() to 1)
    override fun value(): Int = 2

    override fun title(): String = "Shield"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
