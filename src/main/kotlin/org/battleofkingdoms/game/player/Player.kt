package org.battleofkingdoms.game.player

import org.battleofkingdoms.cards.Card
import org.battleofkingdoms.game.Game
import org.battleofkingdoms.game.phases.GameWaitingForPlayers

class Player(val name: String, private var hand: List<Card> = emptyList()) {

    fun hostGame(numberOfPlayers: Int): GameWaitingForPlayers {
        return GameWaitingForPlayers(numberOfPlayers, this)
    }

    fun joinGame(game: GameWaitingForPlayers): Game {
        return game.join(this)
    }

    fun giveCards(cards: List<Card>) {
        hand = hand + cards
    }

    fun hand(): List<Card> = hand
}
