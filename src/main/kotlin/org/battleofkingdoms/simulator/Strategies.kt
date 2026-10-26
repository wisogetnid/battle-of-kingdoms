package org.battleofkingdoms.simulator

import org.battleofkingdoms.cards.Line
import org.battleofkingdoms.simulator.strategies.HordeRush
import org.battleofkingdoms.simulator.strategies.MonoLine
import org.battleofkingdoms.simulator.strategies.RandomStrategy
import org.battleofkingdoms.simulator.strategies.TierOneFocus

/** Every strategy the simulator seats, by id: `random`, then each build plan in every [Tendency]. */
val registry: Map<String, Strategy> =
    mapOf<String, Strategy>("random" to RandomStrategy) +
        listOf<BuildPlan>(
            HordeRush,
            MonoLine(Line.BOW),
            TierOneFocus,
            // adding a build plan = one class + one entry here
        ).flatMap { plan ->
            Tendency.entries.map { "${plan.id}-${it.id}" to PlannedStrategy(plan, it) }
        }
