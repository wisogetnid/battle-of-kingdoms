package org.battleofkingdoms.cards

import org.battleofkingdoms.cards.Wood
import org.battleofkingdoms.cards.Iron

data class Bow(
    override val attack: Int = 4,
    override val defense: Int = 1,
    override val traits: Set<Trait> = setOf(Trait.RANGED),
    override val upgradeCost: Map<Card, Int> = mapOf(Horde() to 1, Food to 1, Wood to 1)
) : Creature
