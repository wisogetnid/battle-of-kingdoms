package org.battleofkingdoms.battle

import org.battleofkingdoms.cards.CreatureType

data class Army(val creatures: Map<CreatureType, Int> = emptyMap()) {
    init {
        require(creatures.values.all { it > 0 }) { "Army counts must be positive: $creatures" }
    }

    val size: Int get() = creatures.values.sum()

    /**
     * Strength in a duel: each creature's attack plus its single best bonus against any
     * creature in this one [opponent]'s army. Bonuses never add up.
     */
    fun duelStrength(opponent: Army, bonuses: Bonuses): Int =
        creatures.entries.sumOf { (type, count) -> (type.attack + bestBonus(type, opponent, bonuses)) * count }

    private fun bestBonus(type: CreatureType, opponent: Army, bonuses: Bonuses): Int =
        opponent.creatures.keys.maxOfOrNull { bonuses.against(type, it) } ?: 0

    companion object {
        fun of(vararg creatures: Pair<CreatureType, Int>): Army = Army(creatures.toMap().filterValues { it > 0 })
    }
}
