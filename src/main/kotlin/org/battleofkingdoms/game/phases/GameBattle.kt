package org.battleofkingdoms.game.phases

import org.battleofkingdoms.game.Game
import org.battleofkingdoms.player.Player

data class GameBattle(val previousPhase: Game, val players: List<Player>) : Game(previousPhase) {

    override fun state(): State = State.BATTLE
    override fun players(): List<Player> = players
    fun newBattle(): GameBattle {
        return this.copy( players = players.map { it.copy( state = Player.State.ACTIVE) })
    }
}
