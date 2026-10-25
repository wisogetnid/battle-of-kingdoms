package org.battleofkingdoms.battle

import org.battleofkingdoms.cards.CreatureType
import org.battleofkingdoms.cards.CreatureType.HORDE

/**
 * The bonus table: what an upgraded creature gets against one opponent creature. A tier-1 unit
 * gets [tier1VsHorde] against a Horde and [tier1VsCountered] against the tier-1 unit it counters.
 * A tier-2 unit gets [tier2VsHorde] against a Horde, [tier2VsCounteredTier1] against the tier-1
 * unit it counters, [tier2VsOtherTier1] against any other tier-1 unit, and [tier2VsCounteredTier2]
 * against the tier-2 unit it counters. A Horde, a lower tier against a higher one, and every other
 * matchup get nothing.
 */
data class Bonuses(
    val tier1VsHorde: Int,
    val tier1VsCountered: Int,
    val tier2VsHorde: Int,
    val tier2VsCounteredTier1: Int,
    val tier2VsOtherTier1: Int,
    val tier2VsCounteredTier2: Int,
) {
    fun against(attacker: CreatureType, target: CreatureType): Int {
        val counters = attacker.line?.counters == target.line
        return when {
            attacker == HORDE -> 0
            attacker.tier == 1 -> when {
                target == HORDE -> tier1VsHorde
                target.tier == 1 && counters -> tier1VsCountered
                else -> 0
            }
            target == HORDE -> tier2VsHorde
            target.tier == 1 -> if (counters) tier2VsCounteredTier1 else tier2VsOtherTier1
            counters -> tier2VsCounteredTier2
            else -> 0
        }
    }
}
