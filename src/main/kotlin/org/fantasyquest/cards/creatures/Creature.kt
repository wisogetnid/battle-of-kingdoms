package org.fantasyquest.cards.creatures

import org.fantasyquest.cards.Card
import org.fantasyquest.cards.resources.Resource
import org.fantasyquest.game.battle.Army

interface Creature : Card {
    fun attack(opposingArmy: Army) : Int
    fun cost() : Map<Resource, Int>
    fun value() : Int
}
