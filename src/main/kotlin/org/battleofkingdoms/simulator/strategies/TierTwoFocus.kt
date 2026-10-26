package org.battleofkingdoms.simulator.strategies

import org.battleofkingdoms.battle.Army
import org.battleofkingdoms.game.Take
import org.battleofkingdoms.server.PlayerView
import org.battleofkingdoms.simulator.BuildPlan
import org.battleofkingdoms.simulator.BuildUpMove
import org.battleofkingdoms.simulator.army
import org.battleofkingdoms.simulator.oneArtifact
import kotlin.random.Random

/**
 * Climbs to tier 2 first, then upgrades Horde into tier 1 to feed next turn's tier 2; never
 * trades. Only its tier-2 units go to battle, so it sends nothing until it has some.
 */
object TierTwoFocus : BuildPlan {
    override val id = "tier-2-focus"

    override fun buildUp(view: PlayerView, random: Random): BuildUpMove {
        val best = view.affordableUpgrades.groupBy { it.tier }.maxByOrNull { it.key }?.value
        return when (best) {
            null -> BuildUpMove.Ready
            else -> BuildUpMove.Upgrade(best[random.nextInt(best.size)])
        }
    }

    override fun commitPool(view: PlayerView): Army = Army(view.army().creatures.filterKeys { it.tier == 2 })

    override fun claim(view: PlayerView, random: Random): Take = oneArtifact(view)
}
