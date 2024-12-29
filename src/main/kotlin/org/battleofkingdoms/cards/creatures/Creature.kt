package org.battleofkingdoms.cards.creatures

import org.battleofkingdoms.cards.Card
import org.battleofkingdoms.cards.resources.Resource
import org.battleofkingdoms.game.battle.Army

interface Creature : Card {
    fun attack(opposingArmy: Army) : Int
    fun cost() : Map<Resource, Int>
    fun value() : Int
}
