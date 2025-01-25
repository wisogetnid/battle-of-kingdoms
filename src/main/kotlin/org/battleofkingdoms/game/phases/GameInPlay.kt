package org.battleofkingdoms.game.phases

import org.battleofkingdoms.game.Game
import org.battleofkingdoms.player.Player
import java.util.*

private const val CARD_DRAW_ON_NEW_TURN = 4

class GameInPlay(id: UUID, var players: Set<Player>) : Game(players.size, id) {
    override fun state(): State = State.IN_PLAY
    override fun players(): Set<Player> = players

    // TODO updates resourceDeck and players
    fun newTurn(): GameInPlay {
        players.forEach {
            resourceDeck.drop(CARD_DRAW_ON_NEW_TURN).also { resourceDeck = it }
        }
        players
            .map {
                it.copy(
                    hand = it.hand + resourceDeck.take(CARD_DRAW_ON_NEW_TURN),
                    state = Player.State.ACTIVE
                )
            }
            .toSet()
            .also { players = it }
        return this
    }

}