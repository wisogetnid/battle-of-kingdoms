package org.battleofkingdoms.game

import org.battleofkingdoms.cards.Card
import org.battleofkingdoms.cards.Horde
import org.battleofkingdoms.cards.Iron
import org.battleofkingdoms.cards.Wood
import org.battleofkingdoms.player.Player
import java.util.*

data class GameState(
    val resourceDeck: List<Card>,
    val creatureDeck: List<Card> = emptyList(),
    val id: UUID = UUID.randomUUID(),
    var playernameToPlayer: Map<String, Player> = mutableMapOf(),
    var state: State = State.IN_PLAY,
    val finishedPlayersInPhase: Set<Player> = emptySet()
) {

    companion object {
        fun wood(count: Int): List<Card> = (1..count).map { Wood }
        fun iron(count: Int): List<Card> = (1..count).map { Iron }
        fun horde(count: Int): List<Card> = (1..count).map { Horde() }

        fun withTestResources(): GameState = GameState(
            resourceDeck = (horde(2) + Wood + Iron +
                    horde(2) + Wood + Iron +
                    wood(18) + iron(18) + horde(16)).toMutableList()
        )
    }

    enum class State { IN_PLAY, BATTLE }
}
