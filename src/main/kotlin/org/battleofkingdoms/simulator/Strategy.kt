package org.battleofkingdoms.simulator

import org.battleofkingdoms.battle.Army
import org.battleofkingdoms.cards.CreatureType
import org.battleofkingdoms.cards.ResourceType
import org.battleofkingdoms.game.Take
import org.battleofkingdoms.server.PlayerView
import kotlin.random.Random

/** One build-up step: a game action, or [Ready] to end the build-up. */
sealed interface BuildUpMove {
    data class Trade(val resource: ResourceType) : BuildUpMove
    data class Upgrade(val into: CreatureType) : BuildUpMove
    data class Transmute(val resource: ResourceType) : BuildUpMove
    data object Ready : BuildUpMove
}

/** A bot. [PlayerView] is exactly what its seat would see over the API; the [Random] is its own. */
interface Strategy {
    /** Called repeatedly during build-up until it answers [BuildUpMove.Ready]. */
    fun buildUp(view: PlayerView, random: Random): BuildUpMove

    fun commit(view: PlayerView, random: Random): Army

    /** Only called on the battle's winner, while it chooses its prize (`view.me.drawnArtifact`). */
    fun claim(view: PlayerView, random: Random): Take
}
