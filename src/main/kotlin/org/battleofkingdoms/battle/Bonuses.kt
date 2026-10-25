package org.battleofkingdoms.battle

import org.battleofkingdoms.cards.CreatureType
import org.battleofkingdoms.cards.CreatureType.HORDE

/**
 * The bonus table: what an upgraded creature gets against one opponent creature. Against a
 * Horde it gets [tier1VsHorde] or [tier2VsHorde]; against a creature of the line it counters,
 * [sameTierCounter] at the same tier or [higherTierCounter] one tier below. A Horde, a lower
 * tier against a higher one, and every other matchup get nothing.
 */
data class Bonuses(
    val tier1VsHorde: Int,
    val tier2VsHorde: Int,
    val sameTierCounter: Int,
    val higherTierCounter: Int,
) {
    fun against(attacker: CreatureType, target: CreatureType): Int = when {
        attacker == HORDE -> 0
        target == HORDE -> vsHorde(attacker.tier)
        attacker.line?.counters != target.line -> 0
        attacker.tier == target.tier -> sameTierCounter
        attacker.tier > target.tier -> higherTierCounter
        else -> 0
    }

    private fun vsHorde(tier: Int): Int = when (tier) {
        1 -> tier1VsHorde
        else -> tier2VsHorde
    }
}
