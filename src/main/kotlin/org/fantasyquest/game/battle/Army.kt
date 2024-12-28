package org.fantasyquest.game.battle

import org.fantasyquest.cards.creatures.Creature
import org.fantasyquest.game.battle.BattleResult.BattleOutcome

class Army(val creatures: Map<Creature, Int>) {
    fun attackStrength(opposingArmy: Army): Int {
        return creatures.entries.sumOf { it.key.attack(opposingArmy) * it.value }
    }

    fun attack(opposingArmy: Army): BattleResult {
        return BattleResult(emptyArmy(), emptyArmy(), BattleOutcome.DRAW)
    }

    private fun emptyArmy() = Army(emptyMap<Creature, Int>())
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Army

        return creatures == other.creatures
    }

    override fun hashCode(): Int {
        return creatures.hashCode()
    }


}
