package org.battleofkingdoms.battle

import org.battleofkingdoms.cards.creatures.Creature

data class Army(val creatures: List<Creature> = emptyList()) {
    fun attackStrength(opposingArmy: Army): Int {
        return creatures.sumOf { it.attack(opposingArmy) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Army

        return creatures == other.creatures
    }

    override fun hashCode(): Int {
        return creatures.hashCode()
    }

    override fun toString(): String {
        return "Army(creatures=$creatures)"
    }

    companion object {
        fun of(vararg creatures: Creature): Army = Army(creatures.toList())
    }
}
