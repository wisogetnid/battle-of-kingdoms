package org.battleofkingdoms.cards.creatures

import org.battleofkingdoms.cards.creatures.traits.Infantry
import org.battleofkingdoms.cards.resources.Resource
import org.battleofkingdoms.battle.Army

class Horde : Creature, Infantry {
    override fun attack(opposingArmy: Army): Int = 1
    override fun cost(): Map<Resource, Int> = emptyMap()
    override fun value(): Int = 1

    override fun title(): String = "Horde"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
