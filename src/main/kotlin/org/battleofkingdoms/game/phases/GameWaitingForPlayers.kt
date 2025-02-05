package org.battleofkingdoms.game.phases

import org.battleofkingdoms.game.Game

data class GameWaitingForPlayers(val expectedNumberOfPlayers: Int) :
    Game(expectedNumberOfPlayers) {

    override fun state() = State.WAIT_FOR_PLAYERS_TO_JOIN
}