package org.battleofkingdoms.simulator.strategies

import org.battleofkingdoms.battle.Army
import org.battleofkingdoms.cards.ResourceType
import org.battleofkingdoms.game.Action
import org.battleofkingdoms.game.Take
import org.battleofkingdoms.server.PlayerView
import org.battleofkingdoms.simulator.BuildPlan
import org.battleofkingdoms.simulator.BuildUpMove
import org.battleofkingdoms.simulator.army
import org.battleofkingdoms.simulator.resources
import kotlin.random.Random

/**
 * The artifact collector: takes every artifact it's offered and only scores once the stack is
 * empty. Upgrades anything it can, tier 2 first, never trades, and when no upgrade is left it
 * uses the Transmuter Stone (if it owns it) on a resource it couldn't spend.
 */
object Hoarder : BuildPlan {
    override val id = "hoarder"

    override fun buildUp(view: PlayerView, random: Random): BuildUpMove {
        val upgrade = view.affordableUpgrades.maxByOrNull { it.tier }
        val unspent = ResourceType.entries.filter { view.resources(it) > 0 }
        return when {
            upgrade != null -> BuildUpMove.Upgrade(upgrade)
            Action.TRANSMUTE in view.availableActions && unspent.isNotEmpty() ->
                BuildUpMove.Transmute(unspent[random.nextInt(unspent.size)])
            else -> BuildUpMove.Ready
        }
    }

    override fun commitPool(view: PlayerView): Army = view.army()

    override fun claim(view: PlayerView, random: Random): Take = Take.ARTIFACT
}
