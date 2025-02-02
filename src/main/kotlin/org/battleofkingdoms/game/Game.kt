package org.battleofkingdoms.game

import org.battleofkingdoms.cards.Card
import org.battleofkingdoms.cards.creatures.Horde
import org.battleofkingdoms.cards.resources.Iron
import org.battleofkingdoms.cards.resources.Wood
import org.battleofkingdoms.player.Player
import java.util.*

open class Game(val numberOfPlayers: Int, val id: UUID = UUID.randomUUID()) {
    open fun state(): State = State.NOT_INITIALIZED
    open fun players(): List<Player> = emptyList()
    var board: Board = Board.withTestResources()

    constructor(existingGame: Game) : this(existingGame.numberOfPlayers, existingGame.id)

    fun setToWaiting(playerName: String): List<Player> {
        return players()
            .map {
                if (it.name.equals(playerName)) {
                    it.copy(state = Player.State.WAITING)
                } else {
                    it
                }
            }
    }

    fun removeCards(playerName: String, vararg cards: Card): List<Player> {
        return players()
            .map {
                if (it.name.equals(playerName)) {
                    it.copy(hand = it.hand - cards)
                } else {
                    it
                }
            }
    }

    enum class State { WAIT_FOR_PLAYERS_TO_JOIN, IN_PLAY, NOT_INITIALIZED, BATTLE }
}