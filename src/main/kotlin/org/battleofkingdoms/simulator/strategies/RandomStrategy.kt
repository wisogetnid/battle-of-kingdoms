package org.battleofkingdoms.simulator.strategies

import org.battleofkingdoms.battle.Army
import org.battleofkingdoms.cards.CreatureType
import org.battleofkingdoms.cards.ResourceType
import org.battleofkingdoms.game.Action
import org.battleofkingdoms.game.Take
import org.battleofkingdoms.server.PlayerView
import org.battleofkingdoms.simulator.BuildUpMove
import org.battleofkingdoms.simulator.Strategy
import org.battleofkingdoms.simulator.count
import org.battleofkingdoms.simulator.resources
import kotlin.random.Random

/** The baseline: random legal build-up steps, a random part of the hand to battle, a coin-flip prize. */
object RandomStrategy : Strategy {
    override fun buildUp(view: PlayerView, random: Random): BuildUpMove {
        val held = ResourceType.entries.filter { view.resources(it) > 0 }
        val options = buildList {
            if (Action.TRADE in view.availableActions) held.forEach { add(BuildUpMove.Trade(it)) }
            if (Action.TRANSMUTE in view.availableActions) held.forEach { add(BuildUpMove.Transmute(it)) }
            view.affordableUpgrades.forEach { add(BuildUpMove.Upgrade(it)) }
        }
        return when {
            options.isEmpty() || random.nextInt(3) == 0 -> BuildUpMove.Ready
            else -> options[random.nextInt(options.size)]
        }
    }

    override fun commit(view: PlayerView, random: Random): Army =
        Army(CreatureType.entries.associateWith { random.nextInt(view.count(it) + 1) }.filterValues { it > 0 })

    override fun claim(view: PlayerView, random: Random): Take = if (random.nextBoolean()) Take.ARTIFACT else Take.VICTORY_POINT
}
