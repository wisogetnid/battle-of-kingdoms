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
    fun `duel strength is the sum of attacks with the shield bonus against a ranged opponent`(
        army: Army,
        opponent: Army,
        expectedStrength: Int,
        case: String,
    ) {
        Assertions.assertEquals(expectedStrength, army.duelStrength(opponent))
    }

    companion object {
        @JvmStatic
        fun armies() = listOf(
            Arguments.of(Army(), Army.of(HORDE to 1), 0, "empty army"),
            Arguments.of(Army.of(HORDE to 1), Army(), 1, "one horde"),
            Arguments.of(Army.of(HORDE to 2, BOW to 1), Army(), 4, "hordes and bow"),
            Arguments.of(Army.of(SHIELD to 2), Army.of(HORDE to 3), 2, "shields against a non-ranged opponent"),
            Arguments.of(Army.of(SHIELD to 2), Army.of(HORDE to 1, BOW to 1), 4, "shields against a bow"),
            Arguments.of(Army.of(SHIELD to 1, BOW to 1), Army.of(HORDE to 1), 3, "own bow gives no shield bonus"),
            Arguments.of(Army.of(SHIELD to 1), Army(), 1, "empty opponent"),
        )
    }
}
