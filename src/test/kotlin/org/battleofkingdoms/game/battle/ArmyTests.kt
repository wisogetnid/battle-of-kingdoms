package org.battleofkingdoms.game.battle

import org.battleofkingdoms.battle.Army
import org.battleofkingdoms.cards.creatures.Bow
import org.battleofkingdoms.cards.creatures.Horde
import org.battleofkingdoms.cards.creatures.Shield
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
            Arguments.of(Army.of(Horde(), Bow()), Army(), 3),
            Arguments.of(Army.of(Shield()), Army.of(Bow()), 2),
        )
    }
}