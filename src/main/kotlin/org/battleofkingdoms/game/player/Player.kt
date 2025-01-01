package org.battleofkingdoms.game.player

import org.battleofkingdoms.game.Game
import org.battleofkingdoms.game.states.GameWaitingForPlayers

class Player(val name: String) {

    fun hostGame(numberOfPlayers: Int): GameWaitingForPlayers {
        return GameWaitingForPlayers(numberOfPlayers, this)
    }

    fun joinGame(game: GameWaitingForPlayers): Game {
        return game.join(this)
    }

}
