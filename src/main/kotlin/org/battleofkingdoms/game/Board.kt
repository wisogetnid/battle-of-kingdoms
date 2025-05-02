package org.battleofkingdoms.game

import org.battleofkingdoms.cards.Card
import org.battleofkingdoms.cards.creatures.Horde
import org.battleofkingdoms.cards.resources.Iron
import org.battleofkingdoms.cards.resources.Wood
import org.battleofkingdoms.player.Player
import java.util.*

data class Board(
    val resourceDeck: MutableList<Card>,
    val id: UUID = UUID.randomUUID(),
    var playernameToPlayer: Map<String, Player> = mutableMapOf(),
    var state: State = State.IN_PLAY
) {

    companion object {
        fun wood(count: Int): List<Card> = (1..count).map { Wood() }
        fun iron(count: Int): List<Card> = (1..count).map { Iron() }
        fun horde(count: Int): List<Card> = (1..count).map { Horde() }

        fun withTestResources(): Board = Board(
            resourceDeck = (horde(2) + Wood() + Iron() +
                    horde(2) + Wood() + Iron() +
                    wood(18) + iron(18) + horde(16)).toMutableList()
        )
    }

    enum class State { IN_PLAY, BATTLE }
}
