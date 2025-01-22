package org.battleofkingdoms.game.player

import org.battleofkingdoms.cards.Card
import org.battleofkingdoms.game.Game
import org.battleofkingdoms.game.phases.GameInPlay

class Player(val name: String, private var hand: List<Card> = emptyList()) {

    // TODO updates hand
    fun giveCards(cards: List<Card>) {
        hand = hand + cards
    }

    fun hand(): List<Card> = hand

    fun finishBuildUp(game: GameInPlay): Game {
        return game
    }
}
