package org.battleofkingdoms.simulator.strategies

import org.battleofkingdoms.battle.Army
import org.battleofkingdoms.game.Take
import org.battleofkingdoms.server.PlayerView
import org.battleofkingdoms.simulator.BuildPlan
import org.battleofkingdoms.simulator.BuildUpMove
import org.battleofkingdoms.simulator.army
import org.battleofkingdoms.simulator.oneArtifact
import kotlin.random.Random

/** Turns every Horde it can into a tier-1 unit of any line, never climbs to tier 2, never trades. */
object TierOneFocus : BuildPlan {
    override val id = "tier-1-focus"

    override fun buildUp(view: PlayerView, random: Random): BuildUpMove {
        val tierOne = view.affordableUpgrades.filter { it.tier == 1 }
        return when {
            tierOne.isEmpty() -> BuildUpMove.Ready
            else -> BuildUpMove.Upgrade(tierOne[random.nextInt(tierOne.size)])
        }
    }

    override fun commitPool(view: PlayerView): Army = view.army()

    override fun claim(view: PlayerView, random: Random): Take = oneArtifact(view)
}
