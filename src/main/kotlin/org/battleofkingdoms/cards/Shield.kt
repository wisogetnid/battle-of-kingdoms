package org.battleofkingdoms.cards

import org.battleofkingdoms.cards.Wood
import org.battleofkingdoms.cards.Iron

data class Shield(
    override val attack: Int = 2,
    override val defense: Int = 5,
    override val traits: Set<Trait> = setOf(Trait.INFANTRY),
    override val upgradeCost: Map<Card, Int> = mapOf(Horde() to 1, Food to 1, Iron to 1)
) : Creature
