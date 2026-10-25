package org.battleofkingdoms.cards

import org.battleofkingdoms.cards.CreatureType.BOW
import org.battleofkingdoms.cards.CreatureType.HORDE
import org.battleofkingdoms.cards.CreatureType.MAGE
import org.battleofkingdoms.cards.CreatureType.SWORD
import org.battleofkingdoms.cards.ResourceType.ESSENCE
import org.battleofkingdoms.cards.ResourceType.IRON
import org.battleofkingdoms.cards.ResourceType.WOOD
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CardTests {

    @ParameterizedTest
    @MethodSource("creatures")
    fun `creature stats match the rules table`(
        type: CreatureType,
        upgradeResource: ResourceType?,
        counters: CreatureType?,
    ) {
        assertEquals(1, type.attack)
        assertEquals(upgradeResource, type.upgradeResource)
        assertEquals(counters, type.counters)
    }

    @Test
    fun `the upgraded creatures form a counter triangle`() {
        val upgraded = CreatureType.entries.filter { it.counters != null }

        assertEquals(setOf(BOW, SWORD, MAGE), upgraded.toSet())
        assertEquals(upgraded.toSet(), upgraded.map { it.counters }.toSet(), "each upgraded creature is countered exactly once")
        upgraded.forEach { assertEquals(it, it.counters!!.counters!!.counters, "$it closes the cycle in three steps") }
    }

    @Test
    fun `the horde is never countered`() {
        assertTrue(CreatureType.entries.none { it.counters == HORDE })
    }

    @Test
    fun `each resource upgrades into exactly one creature`() {
        assertEquals(ResourceType.entries.toSet(), CreatureType.entries.mapNotNull { it.upgradeResource }.toSet())
        assertEquals(ResourceType.entries.size, CreatureType.entries.count { it.upgradeResource != null })
    }

    @Test
    fun `a hand counts creatures and resources`() {
        val hand = Hand() + HORDE + HORDE + ESSENCE

        assertEquals(2, hand.count(HORDE))
        assertEquals(1, hand.count(ESSENCE))
        assertEquals(0, hand.count(IRON))
        assertTrue(hand.has(HORDE, 2))
        assertFalse(hand.has(HORDE, 3))
        assertFalse(hand.has(IRON))
    }

    @Test
    fun `removing the last card of a type drops it from the hand`() {
        val hand = Hand.of(HORDE to 1, WOOD to 1) - HORDE - WOOD + BOW

        assertEquals(Hand.of(BOW to 1), hand)
    }

    @Test
    fun `removing a card the hand doesn't have fails`() {
        assertFailsWith<IllegalArgumentException> { Hand.of(HORDE to 1) - IRON }
    }

    companion object {
        @JvmStatic
        fun creatures() = listOf(
            Arguments.of(HORDE, null, null),
            Arguments.of(BOW, WOOD, MAGE),
            Arguments.of(SWORD, IRON, BOW),
            Arguments.of(MAGE, ESSENCE, SWORD),
        )
    }
}
