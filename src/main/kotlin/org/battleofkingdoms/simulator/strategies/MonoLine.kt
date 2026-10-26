package org.battleofkingdoms.simulator.strategies

import org.battleofkingdoms.battle.Army
import org.battleofkingdoms.cards.CreatureType
import org.battleofkingdoms.cards.Line
import org.battleofkingdoms.cards.ResourceType
import org.battleofkingdoms.game.Take
import org.battleofkingdoms.server.PlayerView
import org.battleofkingdoms.simulator.BuildPlan
import org.battleofkingdoms.simulator.BuildUpMove
import org.battleofkingdoms.simulator.army
import org.battleofkingdoms.simulator.oneArtifact
import org.battleofkingdoms.simulator.resources
import kotlin.random.Random

/**
 * Builds only along [line], tier 2 first, and trades every other resource for Horde.
 * One line is enough: the three lines counter each other in a symmetric triangle.
 */
class MonoLine(private val line: Line) : BuildPlan {
    override val id = "mono-line"

    private val lineResource: ResourceType = CreatureType.entries.first { it.line == line }.upgradeResource!!

    override fun buildUp(view: PlayerView, random: Random): BuildUpMove {
        val upgrade = view.affordableUpgrades.filter { it.line == line }.maxByOrNull { it.tier }
        val spare = ResourceType.entries.firstOrNull { it != lineResource && view.resources(it) > 0 }
        return when {
            upgrade != null -> BuildUpMove.Upgrade(upgrade)
            spare != null -> BuildUpMove.Trade(spare)
            else -> BuildUpMove.Ready
        }
    }

    override fun commitPool(view: PlayerView): Army = view.army()

    override fun claim(view: PlayerView, random: Random): Take = oneArtifact(view)
}
