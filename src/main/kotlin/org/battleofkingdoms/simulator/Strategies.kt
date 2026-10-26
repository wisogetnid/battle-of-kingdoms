package org.battleofkingdoms.simulator

import org.battleofkingdoms.simulator.strategies.HordeRush
import org.battleofkingdoms.simulator.strategies.RandomStrategy

/** Every strategy the simulator seats, by id: `random`, then each build plan in every [Tendency]. */
val registry: Map<String, Strategy> =
    mapOf<String, Strategy>("random" to RandomStrategy) +
        listOf<BuildPlan>(
            HordeRush,
            // adding a build plan = one class + one entry here
        ).flatMap { plan ->
            Tendency.entries.map { "${plan.id}-${it.id}" to PlannedStrategy(plan, it) }
        }
