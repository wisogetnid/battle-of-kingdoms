package org.battleofkingdoms.player

import org.battleofkingdoms.cards.Hand

data class Player(
    val name: String,
    val hand: Hand = Hand(),
    val vp: Int = 0,
    val acted: Boolean = false,
)
