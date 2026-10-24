package org.battleofkingdoms.battle

import org.battleofkingdoms.cards.CreatureType

data class Army(val creatures: Map<CreatureType, Int> = emptyMap()) {
    init {
        require(creatures.values.all { it > 0 }) { "Army counts must be positive: $creatures" }
    }

    val size: Int get() = creatures.values.sum()

    val hasRanged: Boolean get() = creatures.keys.any { it.ranged }

    fun strength(otherArmies: List<Army>): Int {
        val facingRanged = otherArmies.any { it.hasRanged }
        return creatures.entries.sumOf { (type, count) -> type.attack(facingRanged) * count }
    }

    companion object {
        fun of(vararg creatures: Pair<CreatureType, Int>): Army = Army(creatures.toMap().filterValues { it > 0 })
    }
}
