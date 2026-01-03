package org.battleofkingdoms.cards

import org.battleofkingdoms.cards.Wood
import org.battleofkingdoms.cards.Iron

data class Bow(
    override val attack: Int = 3,
    override val defense: Int = 1,
    override val traits: Set<Trait> = setOf(Trait.RANGED, Trait.INFANTRY),
    override val upgradeCost: Map<Resource, Int> = mapOf(Wood() to 1, Iron() to 2)
) : Creature
