package org.battleofkingdoms.simulator

import org.battleofkingdoms.battle.Army
import org.battleofkingdoms.cards.CreatureType.BOW
import org.battleofkingdoms.cards.CreatureType.HORDE
import org.battleofkingdoms.cards.CreatureType.KNIGHT
import org.battleofkingdoms.cards.CreatureType.MAGE
import org.battleofkingdoms.cards.CreatureType.SWORD
import org.battleofkingdoms.cards.CreatureType.WARBOW
import org.battleofkingdoms.simulator.Tendency.AGGRESSIVE
import org.battleofkingdoms.simulator.Tendency.BALANCED
import org.battleofkingdoms.simulator.Tendency.CAUTIOUS
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals

class TendencyTests {

    @ParameterizedTest(name = "{0} of {1} units sends {2}")
    @CsvSource(
        "CAUTIOUS, 0, 0", "CAUTIOUS, 1, 1", "CAUTIOUS, 3, 1", "CAUTIOUS, 7, 3",
        "BALANCED, 0, 0", "BALANCED, 1, 1", "BALANCED, 3, 2", "BALANCED, 7, 4",
        "AGGRESSIVE, 0, 0", "AGGRESSIVE, 1, 1", "AGGRESSIVE, 3, 3", "AGGRESSIVE, 7, 7",
    )
    fun `each tendency sends its share of the pool, rounded up`(tendency: Tendency, poolSize: Int, sent: Int) {
        assertEquals(sent, tendency.pick(Army.of(HORDE to poolSize)).size)
    }

    @Test
    fun `the strongest units go first, ties in creature order`() {
        val pool = Army.of(HORDE to 3, BOW to 1, SWORD to 1, MAGE to 1, KNIGHT to 1, WARBOW to 1)

        assertEquals(Army.of(WARBOW to 1, KNIGHT to 1, BOW to 1), CAUTIOUS.pick(pool))
        assertEquals(Army.of(WARBOW to 1, KNIGHT to 1, BOW to 1, SWORD to 1), BALANCED.pick(pool))
        assertEquals(pool, AGGRESSIVE.pick(pool))
    }
}
