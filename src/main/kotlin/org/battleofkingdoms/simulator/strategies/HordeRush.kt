package org.battleofkingdoms.simulator.strategies

import org.battleofkingdoms.battle.Army
import org.battleofkingdoms.cards.ResourceType
import org.battleofkingdoms.game.Take
import org.battleofkingdoms.server.PlayerView
import org.battleofkingdoms.simulator.BuildPlan
import org.battleofkingdoms.simulator.BuildUpMove
import org.battleofkingdoms.simulator.army
import org.battleofkingdoms.simulator.oneArtifact
import org.battleofkingdoms.simulator.resources
import kotlin.random.Random

/** Trades every resource for Horde; its whole army is up for battle. */
object HordeRush : BuildPlan {
    override val id = "horde-rush"

    override fun buildUp(view: PlayerView, random: Random): BuildUpMove =
        ResourceType.entries.firstOrNull { view.resources(it) > 0 }
            ?.let { BuildUpMove.Trade(it) }
            ?: BuildUpMove.Ready

    override fun commitPool(view: PlayerView): Army = view.army()

    override fun claim(view: PlayerView, random: Random): Take = oneArtifact(view)
}
