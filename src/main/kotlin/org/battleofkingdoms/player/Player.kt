package org.battleofkingdoms.player

import org.battleofkingdoms.cards.Card

class Player(val name: String, private var hand: List<Card> = emptyList(), private var state: State = State.WAITING) {

    // TODO updates hand
    fun giveCards(cards: List<Card>) {
        hand = hand + cards
    }

    fun hand(): List<Card> = hand

    fun state(): State = state

    // TODO updates state
    fun setActive() = run { state = State.ACTIVE }

    // TODO updates xtate
    fun setWaiting() = run { state = State.WAITING}

    enum class State { ACTIVE, WAITING }
}
