package org.battleofkingdoms.game.phases

import org.battleofkingdoms.game.Game
import org.battleofkingdoms.player.Player
import java.util.*

private const val CARD_DRAW_ON_NEW_TURN = 4

class GameInPlay(id: UUID, val players: Set<Player>) : Game(players.size, id) {
    override fun state(): State = State.IN_PLAY
    override fun players(): Set<Player> = players

    fun newTurn(): GameInPlay {
        players.forEach { this.drawCards(it, CARD_DRAW_ON_NEW_TURN) }
        return this
    }

    // TODO updates resourceDeck
    private fun drawCards(player: Player, amount: Int) {
        player.giveCards(this.resourceDeck.take(amount))
        resourceDeck = resourceDeck.drop(CARD_DRAW_ON_NEW_TURN)
    }
}