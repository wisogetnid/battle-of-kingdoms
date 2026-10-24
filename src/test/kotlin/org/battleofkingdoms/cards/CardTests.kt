package org.battleofkingdoms.cards

import org.battleofkingdoms.cards.CreatureType.BOW
import org.battleofkingdoms.cards.CreatureType.HORDE
import org.battleofkingdoms.cards.CreatureType.SHIELD
import org.battleofkingdoms.cards.ResourceType.IRON
import org.battleofkingdoms.cards.ResourceType.WOOD
import org.battleofkingdoms.cards.Trait.INFANTRY
import org.battleofkingdoms.cards.Trait.RANGED
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
        attack: Int,
        attackAgainstRanged: Int,
        traits: Set<Trait>,
        upgradeResource: ResourceType?,
    ) {
        assertEquals(attack, type.attack(facingRanged = false))
        assertEquals(attackAgainstRanged, type.attack(facingRanged = true))
        assertEquals(traits, type.traits)
        assertEquals(upgradeResource, type.upgradeResource)
    }

    @Test
    fun `only the bow is ranged`() {
        assertEquals(listOf(BOW), CreatureType.entries.filter { it.ranged })
    }

    @Test
    fun `a hand counts creatures and resources`() {
        val hand = Hand() + HORDE + HORDE + WOOD

        assertEquals(2, hand.count(HORDE))
        assertEquals(1, hand.count(WOOD))
        assertEquals(0, hand.count(IRON))
        assertTrue(hand.has(HORDE, 2))
        assertFalse(hand.has(HORDE, 3))
        assertFalse(hand.has(IRON))
    }

    @Test
    fun `removing the last card of a type drops it from the hand`() {
        val hand = Hand.of(HORDE to 1, WOOD to 1) - HORDE - WOOD + SHIELD

        assertEquals(Hand.of(SHIELD to 1), hand)
    }

    @Test
    fun `removing a card the hand doesn't have fails`() {
        assertFailsWith<IllegalArgumentException> { Hand.of(HORDE to 1) - IRON }
    }

    companion object {
        @JvmStatic
        fun creatures() = listOf(
            Arguments.of(HORDE, 1, 1, setOf(INFANTRY), null),
            Arguments.of(SHIELD, 1, 2, setOf(INFANTRY), WOOD),
            Arguments.of(BOW, 2, 2, setOf(INFANTRY, RANGED), IRON),
        )
    }
}
