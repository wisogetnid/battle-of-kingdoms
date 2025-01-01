package org.battleofkingdoms.game

import org.battleofkingdoms.game.player.Player
import java.util.*

open class Game(val numberOfPlayers: Int, val id: UUID = UUID.randomUUID()) {
    open fun state(): State = State.NOT_INITIALIZED
    open fun players(): Set<Player> = emptySet()

    enum class State { WAITING_FOR_PLAYERS, IN_PLAY, NOT_INITIALIZED }
}