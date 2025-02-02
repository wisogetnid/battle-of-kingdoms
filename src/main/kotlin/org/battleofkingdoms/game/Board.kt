package org.battleofkingdoms.game

import org.battleofkingdoms.cards.Card
import org.battleofkingdoms.cards.creatures.Horde
import org.battleofkingdoms.cards.resources.Iron
import org.battleofkingdoms.cards.resources.Wood

data class Board(val resourceDeck: List<Card>) {

    companion object {
        fun wood(count: Int): List<Card> = (1..count).map { Wood() }
        fun iron(count: Int): List<Card> = (1..count).map { Iron() }
        fun horde(count: Int): List<Card> = (1..count).map { Horde() }

        fun withTestResources(): Board = Board(horde(2) + Wood() + Iron() +
                horde(2) + Wood() + Iron() +
                wood(18) + iron(18) + horde(16))
    }
}
