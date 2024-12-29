package org.battleofkingdoms.cards.resources

import org.battleofkingdoms.cards.Card

interface Resource : Card {
    fun value(): Int
}
