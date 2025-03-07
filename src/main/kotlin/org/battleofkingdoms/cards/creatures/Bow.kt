package org.battleofkingdoms.cards.creatures

import org.battleofkingdoms.cards.creatures.traits.Infantry
import org.battleofkingdoms.cards.creatures.traits.Ranged
import org.battleofkingdoms.cards.resources.Resource
import org.battleofkingdoms.cards.resources.Wood
import org.battleofkingdoms.battle.Army

class Bow() : Creature, Infantry, Ranged {
    override fun attack(opposingArmy: Army): Int = 2

    override fun cost(): Map<Resource, Int> = mapOf(Wood() to 1)
    override fun value(): Int = 3

    override fun title(): String = "Bow"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
