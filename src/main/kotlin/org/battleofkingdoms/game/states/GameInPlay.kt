package org.battleofkingdoms.game.states

import org.battleofkingdoms.game.Game
import org.battleofkingdoms.game.player.Player
import java.util.*

class GameInPlay(id: UUID, val players: Set<Player>) : Game(players.size, id) {
    override fun state(): State = State.IN_PLAY

    override fun players(): Set<Player> = players
}