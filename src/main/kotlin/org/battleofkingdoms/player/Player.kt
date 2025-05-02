package org.battleofkingdoms.player

import org.battleofkingdoms.battle.Army
import org.battleofkingdoms.cards.Card

data class Player(val name: String, val hand: MutableList<Card> = mutableListOf(), var state: State = State.ACTIVE, val committedArmy: Army = Army()) {
    enum class State { ACTIVE, WAITING }
}
