package org.battleofkingdoms.player

import org.battleofkingdoms.cards.Card

data class Player(val name: String, val hand: List<Card> = emptyList(), val state: State = State.WAITING) {
    enum class State { ACTIVE, WAITING }
}
