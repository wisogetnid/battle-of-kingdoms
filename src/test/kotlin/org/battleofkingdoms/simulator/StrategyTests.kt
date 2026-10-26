package org.battleofkingdoms.simulator

import org.battleofkingdoms.simulator.strategies.HordeRush
import org.battleofkingdoms.simulator.strategies.RandomStrategy
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

/** Every strategy plays whole games against `random` without breaking a rule; how well it plays isn't tested. */
class StrategyTests {

    @ParameterizedTest(name = "{0}")
    @MethodSource("strategies")
    fun `plays full seeded games against random without errors`(id: String, strategy: Strategy) {
        (1L..5L).forEach { seed ->
            playGame(
                listOf(id to strategy, "random" to RandomStrategy, "random" to RandomStrategy),
                gameSeed = seed,
                engineSeed = seed * 31,
                botSeeds = listOf(seed, seed + 1, seed + 2),
            )
        }
    }

    companion object {
        @JvmStatic
        fun strategies() = (
            listOf("random" to RandomStrategy) +
                Tendency.entries.map { "${HordeRush.id}-${it.id}" to PlannedStrategy(HordeRush, it) }
            ).map { (id, strategy) -> Arguments.of(id, strategy) }
    }
}
