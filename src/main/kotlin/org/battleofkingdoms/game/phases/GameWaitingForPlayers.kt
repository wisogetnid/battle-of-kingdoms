package org.battleofkingdoms.game.phases

import org.battleofkingdoms.game.Game
import org.battleofkingdoms.game.player.Player

class GameWaitingForPlayers(expectedNumberOfPlayers: Int, player: Player) : Game(expectedNumberOfPlayers) {

    val players: MutableSet<Player> = mutableSetOf()

    init {
        players.add(player)
    }

    override fun state() = State.WAIT_FOR_PLAYERS_TO_JOIN

    override fun players(): Set<Player> = players

    fun join(player: Player): Game {
        players.add(player)
        if (players.count() == numberOfPlayers)
            return this.startGame().newTurn()

        return this
    }

    private fun startGame(): GameInPlay {
        return GameInPlay(super.id, players.toHashSet())
    }
}