package org.battleofkingdoms.cards

import org.battleofkingdoms.cards.CreatureType.BOW
import org.battleofkingdoms.cards.CreatureType.HIGH_MAGE
import org.battleofkingdoms.cards.CreatureType.HORDE
import org.battleofkingdoms.cards.CreatureType.KNIGHT
import org.battleofkingdoms.cards.CreatureType.MAGE
import org.battleofkingdoms.cards.CreatureType.SWORD
import org.battleofkingdoms.cards.CreatureType.WARBOW
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
        tier: Int,
        line: Line?,
        upgradeResource: ResourceType?,
        upgradesFrom: CreatureType?,
    ) {
        assertEquals(1, type.attack)
        assertEquals(tier, type.tier)
        assertEquals(line, type.line)
        assertEquals(upgradeResource, type.upgradeResource)
        assertEquals(upgradesFrom, type.upgradesFrom)
    }

    @Test
    fun `the lines form a counter triangle`() {
        assertEquals(Line.MAGE, Line.BOW.counters)
        assertEquals(Line.BOW, Line.SWORD.counters)
        assertEquals(Line.SWORD, Line.MAGE.counters)
        Line.entries.forEach { assertEquals(it, it.counters.counters.counters, "$it closes the cycle in three steps") }
    }

    @Test
    fun `every line has one creature per tier and uses one resource`() {
        Line.entries.forEach { line ->
            val creatures = CreatureType.entries.filter { it.line == line }
            assertEquals(listOf(1, 2), creatures.map { it.tier }.sorted(), "$line tiers")
            assertEquals(1, creatures.map { it.upgradeResource }.distinct().size, "$line resource")
        }
        assertEquals(ResourceType.entries.toSet(), CreatureType.entries.mapNotNull { it.upgradeResource }.toSet())
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
            Arguments.of(HORDE, 0, null, null, null),
            Arguments.of(BOW, 1, Line.BOW, WOOD, HORDE),
            Arguments.of(SWORD, 1, Line.SWORD, IRON, HORDE),
            Arguments.of(MAGE, 1, Line.MAGE, ESSENCE, HORDE),
            Arguments.of(WARBOW, 2, Line.BOW, WOOD, BOW),
            Arguments.of(KNIGHT, 2, Line.SWORD, IRON, SWORD),
            Arguments.of(HIGH_MAGE, 2, Line.MAGE, ESSENCE, MAGE),
        )
    }
}
