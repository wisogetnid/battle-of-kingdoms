package org.battleofkingdoms.player

import org.battleofkingdoms.cards.CreatureType
import org.battleofkingdoms.cards.Hand

/** [upgradedThisTurn] counts the units this player upgraded into this turn; they can't climb again until next turn. */
data class Player(
    val name: String,
    val hand: Hand = Hand(),
    val vp: Int = 0,
    val acted: Boolean = false,
    val upgradedThisTurn: Map<CreatureType, Int> = emptyMap(),
) {
    /** Units of [type] held since before this turn, so free to upgrade. */
    fun settled(type: CreatureType): Int = hand.count(type) - (upgradedThisTurn[type] ?: 0)
}
