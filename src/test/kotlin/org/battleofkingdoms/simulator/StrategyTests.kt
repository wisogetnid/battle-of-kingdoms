package org.battleofkingdoms.simulator

import org.battleofkingdoms.simulator.strategies.RandomStrategy
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

/** Every registered strategy plays whole games against `random` without breaking a rule; how well it plays isn't tested. */
class StrategyTests {

    @ParameterizedTest(name = "{0}")
    @MethodSource("strategies")
    fun `plays full seeded games against random without errors`(id: String, strategy: Strategy) {
        (1L..5L).forEach { seed ->
            playGame(
                listOf(id to strategy) + List(PLAYERS - 1) { "random" to RandomStrategy },
                gameSeed = seed,
                engineSeed = seed * 31,
                botSeeds = List(PLAYERS) { seed + it },
            )
        }
    }

    companion object {
        @JvmStatic
        fun strategies() = registry.map { (id, strategy) -> Arguments.of(id, strategy) }
    }
}
