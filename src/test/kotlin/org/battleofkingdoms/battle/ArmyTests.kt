package org.battleofkingdoms.battle

import org.battleofkingdoms.cards.Bow
import org.battleofkingdoms.cards.Horde
import org.battleofkingdoms.cards.Shield
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class ArmyTests {
    @ParameterizedTest
    @MethodSource("armies")
    fun shouldHaveMandatoryFields(army: Army, opposingArmy: Army, expectedAttackStrengh: Int) {
        Assertions.assertEquals(expectedAttackStrengh, army.attackStrength(opposingArmy));
    }

    companion object {
        @JvmStatic
        fun armies() = listOf(
            Arguments.of(Army.of(Horde()), Army(), 1),
            Arguments.of(Army.of(Horde(), Horde()), Army(), 2),
            Arguments.of(Army.of(Horde(), Bow()), Army(), 4),
            Arguments.of(Army.of(Shield()), Army.of(Bow()), 1),
        )
    }
}
