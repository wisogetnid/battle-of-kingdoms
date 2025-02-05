package org.battleofkingdoms.game

import org.battleofkingdoms.cards.Card
import org.battleofkingdoms.game.phases.GameInPlay
import org.battleofkingdoms.player.Player
import java.util.*

open class Game(val numberOfPlayers: Int, val playerList: MutableList<Player> = mutableListOf(), val id: UUID = UUID.randomUUID()) {
    open fun state(): State = State.NOT_INITIALIZED
    open fun players(): List<Player> = playerList.toList()
    var board: Board = Board.withTestResources()

    constructor(existingGame: Game) : this(existingGame.numberOfPlayers, id = existingGame.id)

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

    fun join(player: Player): Game {
        playerList.add(player)
        return when (playerList.count()) {
            numberOfPlayers -> startGame().newTurn()
            else -> this
        }
    }

    private fun startGame(): GameInPlay {
        return GameInPlay(id, players())
    }

    enum class State { WAIT_FOR_PLAYERS_TO_JOIN, IN_PLAY, NOT_INITIALIZED, BATTLE }
}