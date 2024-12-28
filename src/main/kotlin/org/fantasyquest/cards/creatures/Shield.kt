package org.fantasyquest.cards.creatures

import org.fantasyquest.cards.creatures.traits.Infantry
import org.fantasyquest.cards.resources.Resource
import org.fantasyquest.cards.resources.Wood
import org.fantasyquest.game.battle.Army

class Shield : Creature, Infantry {
    override fun attack(opposingArmy: Army): Int = when {
        opposingArmy.creatures.keys.contains(Bow()) -> 2
        else -> 1
    }
    override fun cost(): Map<Resource, Int> = mapOf(Wood() to 1)
    override fun title(): String = "Sword and Shield"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
