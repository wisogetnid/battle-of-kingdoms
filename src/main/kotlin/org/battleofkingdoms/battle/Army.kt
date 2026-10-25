package org.battleofkingdoms.battle

import org.battleofkingdoms.cards.CreatureType

data class Army(val creatures: Map<CreatureType, Int> = emptyMap()) {
    init {
        require(creatures.values.all { it > 0 }) { "Army counts must be positive: $creatures" }
    }

    val size: Int get() = creatures.values.sum()

    /**
     * Strength in a duel: each creature's attack, plus [counterBonus] for each of my creatures
     * whose countered creature is in this one [opponent]'s army.
     */
    fun duelStrength(opponent: Army, counterBonus: Int): Int =
        creatures.entries.sumOf { (type, count) -> (type.attack + bonus(type, opponent, counterBonus)) * count }

    private fun bonus(type: CreatureType, opponent: Army, counterBonus: Int): Int {
        val countered = type.counters ?: return 0
        return when (countered) {
            in opponent.creatures -> counterBonus
            else -> 0
        }
    }

    companion object {
        fun of(vararg creatures: Pair<CreatureType, Int>): Army = Army(creatures.toMap().filterValues { it > 0 })
    }
}
