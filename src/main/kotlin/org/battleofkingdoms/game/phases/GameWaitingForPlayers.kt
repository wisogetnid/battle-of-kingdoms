package org.battleofkingdoms.game.phases

import org.battleofkingdoms.game.Game
import org.battleofkingdoms.player.Player

data class GameWaitingForPlayers(val expectedNumberOfPlayers: Int, val players: Set<Player> = emptySet()) :
    Game(expectedNumberOfPlayers) {

    override fun state() = State.WAIT_FOR_PLAYERS_TO_JOIN

    override fun players(): Set<Player> = players

    fun join(player: Player): Game {
        val game = this.copy(players = players + player)
        return when (game.players.count()) {
            numberOfPlayers -> game.startGame().newTurn()
            else -> game
        }
    }

    private fun startGame(): GameInPlay {
        return GameInPlay(super.id, players.toHashSet())
    }
}