package org.fantasyquest.cards.resources

import org.fantasyquest.cards.Card

interface Resource : Card {
    fun value(): Int
}
