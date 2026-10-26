package org.battleofkingdoms.simulator

import org.battleofkingdoms.battle.Army
import org.battleofkingdoms.cards.CreatureType
import org.battleofkingdoms.game.Take
import org.battleofkingdoms.server.PlayerView
import kotlin.random.Random

/** What a strategy does apart from how much it commits. */
interface BuildPlan {
    val id: String

    fun buildUp(view: PlayerView, random: Random): BuildUpMove

    /** The units this plan is willing to send; the [Tendency] picks how many of them go. */
    fun commitPool(view: PlayerView): Army

    fun claim(view: PlayerView, random: Random): Take
}

/** How much of its commit pool a strategy sends: [numerator]/[denominator] of it, rounded up. */
enum class Tendency(val id: String, private val numerator: Int, private val denominator: Int) {
    CAUTIOUS("cautious", 1, 3),
    BALANCED("balanced", 1, 2),
    AGGRESSIVE("aggressive", 1, 1);

    /** The strongest units first: tier 2, then tier 1, then Horde; ties in [CreatureType] order. */
    fun pick(pool: Army): Army {
        var left = (pool.size * numerator + denominator - 1) / denominator
        val picked = STRONGEST_FIRST.associateWith { type ->
            minOf(pool.creatures[type] ?: 0, left).also { left -= it }
        }
        return Army(picked.filterValues { it > 0 })
    }

    private companion object {
        val STRONGEST_FIRST = CreatureType.entries.sortedWith(compareByDescending<CreatureType> { it.tier }.thenBy { it.ordinal })
    }
}

/** A [BuildPlan] with a [Tendency] is a [Strategy]. */
class PlannedStrategy(val plan: BuildPlan, val tendency: Tendency) : Strategy {
    override fun buildUp(view: PlayerView, random: Random) = plan.buildUp(view, random)

    override fun commit(view: PlayerView, random: Random) = tendency.pick(plan.commitPool(view))

    override fun claim(view: PlayerView, random: Random) = plan.claim(view, random)
}

/** Takes the first artifact offered, then always the VP. */
fun oneArtifact(view: PlayerView): Take = when {
    view.me.artifacts.isEmpty() -> Take.ARTIFACT
    else -> Take.VICTORY_POINT
}
