package org.fantasyquest.cards

import org.fantasyquest.cards.creatures.Bow
import org.fantasyquest.cards.creatures.Creature
import org.fantasyquest.cards.creatures.Horde
import org.fantasyquest.cards.creatures.Shield
import org.fantasyquest.game.battle.Army
import org.fantasyquest.game.battle.BattleResult
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

    @ParameterizedTest
    @MethodSource("battles")
    fun shouldCalculateBattleResults(army: Army, opposingArmy: Army, expectedBattleResult: BattleResult) {
        Assertions.assertEquals(expectedBattleResult, army.attack(opposingArmy))
    }

    companion object {
        @JvmStatic
        fun armies() = listOf(
            Arguments.of(Army(mapOf(Horde() to 1)), emptyArmy(), 1),
            Arguments.of(Army(mapOf(Horde() to 2)), emptyArmy(), 2),
            Arguments.of(Army(mapOf(Horde() to 1, Bow() to 1)), emptyArmy(), 3),
            Arguments.of(Army(mapOf(Shield() to 1)), Army(mapOf(Bow() to 1)), 2),
        )

        @JvmStatic
        fun battles() = listOf(
            Arguments.of(emptyArmy(), emptyArmy(), BattleResult(emptyArmy(), emptyArmy(), BattleResult.BattleOutcome.DRAW))
        )

        private fun emptyArmy() = Army(emptyMap<Creature, Int>())
    }
}