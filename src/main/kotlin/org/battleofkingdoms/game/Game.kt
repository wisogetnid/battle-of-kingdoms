package org.battleofkingdoms.game

import org.battleofkingdoms.battle.Army
import org.battleofkingdoms.cards.Card
import org.battleofkingdoms.player.Player
import java.util.*

const val CARD_DRAW_ON_NEW_TURN = 4

open class Game(val numberOfPlayers: Int, var playernameToPlayer: Map<String, Player> = mutableMapOf(), val id: UUID = UUID.randomUUID(), var state: State = State.NOT_INITIALIZED) {
    open fun state(): State = state
    open fun players(): Map<String, Player> = playernameToPlayer
    var board: Board = Board.withTestResources()

    constructor(existingGame: Game) : this(existingGame.numberOfPlayers, id = existingGame.id)

    fun setPlayerStateTo(playerName: String, playerState: Player.State): Map<String, Player> {
        return playernameToPlayer + mapOf(
            playerName to playernameToPlayer[playerName]!!.copy(state = playerState)
        )
    }

    fun removeCards(playerName: String, cards: List<Card>): Map<String, Player> {
        return playernameToPlayer + mapOf(
            playerName to playernameToPlayer[playerName]!!.copy(hand = playernameToPlayer[playerName]!!.hand - cards)
        )
    }

    fun join(player: Player): Game {
        playernameToPlayer = playernameToPlayer + mapOf(player.name to player)
        return when (playernameToPlayer.count()) {
            numberOfPlayers -> startGame().newTurn()
            else -> this
        }
    }

    private fun startGame(): Game {
        return this
    }


    // TODO updates resourceDeck and players
    fun newTurn(): Game {
        playernameToPlayer = playernameToPlayer.values
            .map {
                it.name to it.copy(
                    hand = it.hand + board.resourceDeck.take(CARD_DRAW_ON_NEW_TURN),
                    state = Player.State.ACTIVE
                )
            }.toMap()
        playernameToPlayer.forEach {
            board = Board(board.resourceDeck.drop(CARD_DRAW_ON_NEW_TURN))
        }
        state = State.IN_PLAY
        return this
    }

    // TODO updates players
    fun finishBuildUp(playerName: String): Game {
        playernameToPlayer = setPlayerStateTo(playerName, Player.State.WAITING)

        return when (playernameToPlayer.all { Player.State.WAITING == it.value.state }) {
            true -> this.newBattle()
            else -> this
        }
    }

    fun newBattle(): Game {
        playernameToPlayer = playernameToPlayer.values
            .map { it.name to it.copy( state = Player.State.ACTIVE ) }
            .fold(emptyMap()) { acc, entry -> acc + entry}
        state = State.BATTLE
        return this
    }

    fun commitArmy(playerName: String, army: Army): Game {
        playernameToPlayer = setPlayerStateTo(playerName, Player.State.WAITING)
        playernameToPlayer = removeCards(playerName, army.creatures)

        //TODO Battle(firstPlayer.committedArmy(), secondPlayer.committedArmy()).resolve()
        return when (playernameToPlayer.all { Player.State.WAITING == it.value.state }) {
            true -> this.newTurn()
            else -> this
        }
    }

    enum class State { WAIT_FOR_PLAYERS_TO_JOIN, IN_PLAY, NOT_INITIALIZED, BATTLE }
}