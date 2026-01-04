package org.battleofkingdoms.cards

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class CardTests {

    @Test
    fun `food resource should be a singleton`() {
        val food1 = Food
        val food2 = Food
        assertSame(food1, food2, "Food should be a singleton object")
    }

    @Test
    fun `horde stats are correct`() {
        val horde = Horde()
        assertEquals(2, horde.attack)
        assertEquals(1, horde.defense)
        assertEquals(setOf(Trait.INFANTRY), horde.traits)
        assertEquals(emptyMap(), horde.upgradeCost)
    }

    @Test
    fun `shield stats and cost are correct`() {
        val shield = Shield()
        assertEquals(2, shield.attack)
        assertEquals(5, shield.defense)
        assertEquals(setOf(Trait.INFANTRY), shield.traits)
        val expectedCost = mapOf(Horde() to 1, Food to 1, Iron to 1)
        assertEquals(expectedCost, shield.upgradeCost)
    }

    @Test
    fun `bow stats and cost are correct`() {
        val bow = Bow()
        assertEquals(4, bow.attack)
        assertEquals(1, bow.defense)
        assertEquals(setOf(Trait.RANGED), bow.traits)
        val expectedCost = mapOf(Horde() to 1, Food to 1, Wood to 1)
        assertEquals(expectedCost, bow.upgradeCost)
    }
}
