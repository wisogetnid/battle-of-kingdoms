package org.battleofkingdoms.game

import org.battleofkingdoms.cards.Card

data class GameSetup(
    val playerHands: Map<String, List<Card>>,
    val creatureDeck: List<Card>,
    val resourceDeck: List<Card>
)
