package org.battleofkingdoms.cards

import org.battleofkingdoms.cards.Wood
import org.battleofkingdoms.cards.Iron

data class Shield(
    override val attack: Int = 1,
    override val defense: Int = 4,
    override val traits: Set<Trait> = setOf(Trait.INFANTRY),
    override val upgradeCost: Map<Resource, Int> = mapOf(Wood() to 2, Iron() to 1)
) : Creature
