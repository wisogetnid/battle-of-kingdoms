package org.battleofkingdoms.battle

import org.battleofkingdoms.cards.CreatureType.BOW
import org.battleofkingdoms.cards.CreatureType.HORDE
import org.battleofkingdoms.cards.CreatureType.SHIELD
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class ArmyTests {
    @ParameterizedTest(name = "{3}")
    @MethodSource("armies")
    fun `strength is the sum of attacks with the shield bonus against ranged`(
        army: Army,
        otherArmies: List<Army>,
        expectedStrength: Int,
        case: String,
    ) {
        Assertions.assertEquals(expectedStrength, army.strength(otherArmies))
    }

    companion object {
        @JvmStatic
        fun armies() = listOf(
            Arguments.of(Army(), emptyList<Army>(), 0, "empty army"),
            Arguments.of(Army.of(HORDE to 1), emptyList<Army>(), 1, "one horde"),
            Arguments.of(Army.of(HORDE to 2, BOW to 1), emptyList<Army>(), 4, "hordes and bow"),
            Arguments.of(Army.of(SHIELD to 2), listOf(Army.of(HORDE to 3)), 2, "shields without ranged opponents"),
            Arguments.of(Army.of(SHIELD to 2), listOf(Army.of(HORDE to 1), Army.of(BOW to 1)), 4, "shields facing a bow"),
            Arguments.of(Army.of(SHIELD to 1, BOW to 1), listOf(Army.of(HORDE to 1)), 3, "own bow gives no shield bonus"),
            Arguments.of(Army.of(SHIELD to 1), listOf(Army()), 1, "empty other army"),
        )
    }
}
