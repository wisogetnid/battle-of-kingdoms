package org.battleofkingdoms.cards

interface Creature : Card {
    val attack: Int
    val defense: Int
    val traits: Set<Trait>
    val upgradeCost: Map<Card, Int>
}
