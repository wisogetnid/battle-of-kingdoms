package org.battleofkingdoms.game

import org.battleofkingdoms.cards.Card
import org.battleofkingdoms.cards.creatures.Creature
import org.battleofkingdoms.player.Player
import java.util.*

const val CARD_DRAW_ON_NEW_TURN = 4

open class Game(val numberOfPlayers: Int, var playerList: MutableList<Player> = mutableListOf(), val id: UUID = UUID.randomUUID(), var state: State = State.NOT_INITIALIZED) {
    open fun state(): State = state
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

    private fun startGame(): Game {
        state = State.IN_PLAY
        return this
    }


    // TODO updates resourceDeck and players
    fun newTurn(): Game {
        playerList = playerList
            .map {
                it.copy(
                    hand = it.hand + board.resourceDeck.take(CARD_DRAW_ON_NEW_TURN),
                    state = Player.State.ACTIVE
                )
            }
            .toMutableList()
        playerList.forEach {
            board = Board(board.resourceDeck.drop(CARD_DRAW_ON_NEW_TURN))
        }
        return this
    }

    // TODO updates players
    fun finishBuildUp(playerName: String): Game {
        playerList = setToWaiting(playerName).toMutableList()

        return when (playerList.all { Player.State.WAITING == it.state }) {
            true -> this.newBattle()
            else -> this
        }
    }

    fun newBattle(): Game {
        playerList = playerList.map { it.copy( state = Player.State.ACTIVE) }.toMutableList()
        state = State.BATTLE
        return this
    }

    fun commitArmy(playerName: String, vararg creatures: Creature): Game {
        playerList = setToWaiting(playerName).toMutableList()
        playerList = removeCards(playerName, *creatures).toMutableList()
        return this
    }

    enum class State { WAIT_FOR_PLAYERS_TO_JOIN, IN_PLAY, NOT_INITIALIZED, BATTLE }
}