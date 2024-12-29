package org.fantasyquest.game.battle

import org.fantasyquest.cards.creatures.Bow
import org.fantasyquest.cards.creatures.Horde
import org.fantasyquest.cards.creatures.Shield
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class BattleTests {

    @ParameterizedTest
    @MethodSource("battles")
    fun shouldCalculateBattleResults(army: Army, opposingArmy: Army, expectedBattleResult: BattleResult) {
        val battleResult = Battle(army, opposingArmy).resolve()
        Assertions.assertEquals(expectedBattleResult.outcome, battleResult.outcome)
        Assertions.assertEquals(expectedBattleResult.armyRemaining, battleResult.armyRemaining)
        Assertions.assertEquals(expectedBattleResult.opposingArmyRemaining, battleResult.opposingArmyRemaining)
    }

    companion object {
        @JvmStatic
        fun battles() = listOf(
            Arguments.of(
                Army.of(Horde()), Army.of(Horde()),
                BattleResult(Army(), Army(), BattleResult.BattleOutcome.DRAW)
            ),
            Arguments.of(
                Army.of(Horde()), Army(),
                BattleResult(Army.of(Horde()), Army(), BattleResult.BattleOutcome.WIN)
            ),
            Arguments.of(
                Army(), Army.of(Horde()),
                BattleResult(Army(), Army.of(Horde()), BattleResult.BattleOutcome.LOSS)
            ),
            Arguments.of(
                Army.of(Horde(), Horde()), Army.of(Horde()),
                BattleResult(Army.of(Horde()), Army(), BattleResult.BattleOutcome.WIN)
            ),
            Arguments.of(
                Army.of(Horde()), Army.of(Horde(), Horde()),
                BattleResult(Army(), Army.of(Horde()), BattleResult.BattleOutcome.LOSS)
            ),
            Arguments.of(
                Army.of(Horde(), Bow()), Army.of(Horde()),
                BattleResult(Army.of(Bow()), Army(), BattleResult.BattleOutcome.WIN)
            ),
            Arguments.of(
                Army.of(Bow()), Army.of(Horde()),
                BattleResult(Army(), Army(), BattleResult.BattleOutcome.WIN)
            ),
            Arguments.of(
                Army.of(Bow(), Bow(), Horde()), Army.of(Shield()),
                BattleResult(Army.of(Bow(), Horde()), Army(), BattleResult.BattleOutcome.WIN)
            )
        )
    }
}