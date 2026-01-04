package org.battleofkingdoms.cards

data class Horde(
    override val attack: Int = 2,
    override val defense: Int = 1,
    override val traits: Set<Trait> = setOf(Trait.INFANTRY),
    override val upgradeCost: Map<Card, Int> = emptyMap()
) : Creature
