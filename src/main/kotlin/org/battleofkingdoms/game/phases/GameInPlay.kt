package org.battleofkingdoms.game.phases

import org.battleofkingdoms.game.Game
import org.battleofkingdoms.player.Player
import java.util.*

private const val CARD_DRAW_ON_NEW_TURN = 4

class GameInPlay(id: UUID, var players: List<Player>) : Game(players.size, id) {
    override fun state(): State = State.IN_PLAY
    override fun players(): List<Player> = players

    // TODO updates resourceDeck and players
    fun newTurn(): GameInPlay {
        players
            .map {
                it.copy(
                    hand = it.hand + resourceDeck.take(CARD_DRAW_ON_NEW_TURN),
                    state = Player.State.ACTIVE
                )
            }
            .also { players = it }
        players.forEach {
            resourceDeck.drop(CARD_DRAW_ON_NEW_TURN).also { resourceDeck = it }
        }
        return this
    }

    // TODO updates players
    fun finishBuildUp(playerName: String): Game {
        players = setToWaiting(playerName)

        return when (players.all { Player.State.WAITING == it.state }) {
            true -> GameBattle(this, players).newBattle()
            else -> this
        }
    }

}