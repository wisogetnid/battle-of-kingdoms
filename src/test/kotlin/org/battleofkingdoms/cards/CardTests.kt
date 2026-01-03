package org.battleofkingdoms.cards

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CardTests {

    @Test
    fun `Horde has correct attributes`() {
        val horde = Horde()
        assertEquals(1, horde.attack)
        assertEquals(1, horde.defense)
        assertEquals(setOf(Trait.INFANTRY), horde.traits)
    }

    @Test
    fun `Shield has correct attributes`() {
        val shield = Shield()
        assertEquals(1, shield.attack)
        assertEquals(4, shield.defense)
        assertEquals(setOf(Trait.INFANTRY), shield.traits)
        assertEquals(mapOf(Wood() to 2, Iron() to 1), shield.upgradeCost)
    }

    @Test
    fun `Bow has correct attributes`() {
        val bow = Bow()
        assertEquals(3, bow.attack)
        assertEquals(1, bow.defense)
        assertEquals(setOf(Trait.RANGED, Trait.INFANTRY), bow.traits)
        assertEquals(mapOf(Wood() to 1, Iron() to 2), bow.upgradeCost)
    }
}
