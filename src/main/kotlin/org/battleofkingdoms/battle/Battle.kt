package org.battleofkingdoms.battle

import org.battleofkingdoms.cards.CreatureType
import kotlin.random.Random

/**
 * Resolves one battle between all committed armies. Pure: the only randomness is [Random],
 * consumed per player in the iteration order of `commits`.
 */
object Battle {
    fun resolve(commits: Map<String, Army>, pot: Int, random: Random): BattleResult {
        val strengths = commits.mapValues { (name, army) -> army.strength(otherArmies(commits, name)) }
        val winner = singleStrongest(strengths)
        val armies = commits.mapValues { (name, army) ->
            val won = name == winner
            val casualties = pickCasualties(army, losses(army.size, won), random)
            ArmyResult(
                army = army,
                strength = strengths.getValue(name),
                casualties = casualties,
                survivors = survivors(army, casualties),
                vpGained = if (won) pot else 0,
                gainedRenown = army.size > 0,
            )
        }
        return BattleResult(pot, winner, armies)
    }

    private fun otherArmies(commits: Map<String, Army>, name: String): List<Army> =
        commits.filterKeys { it != name }.values.toList()

    /** Ties at the top, including everyone at zero, mean nobody wins. */
    private fun singleStrongest(strengths: Map<String, Int>): String? {
        val top = strengths.values.maxOrNull() ?: return null
        val leaders = strengths.filterValues { it == top }.keys
        return when {
            top == 0 || leaders.size > 1 -> null
            else -> leaders.single()
        }
    }

    /** The winner loses `floor(n / 2)`, everyone else `ceil(n / 2)`. */
    private fun losses(committed: Int, won: Boolean): Int = when (won) {
        true -> committed / 2
        false -> (committed + 1) / 2
    }

    private fun pickCasualties(army: Army, count: Int, random: Random): Army {
        val creatures = army.creatures.toSortedMap().flatMap { (type, n) -> List(n) { type } }
        val picked = creatures.shuffled(random).take(count)
        return Army(picked.groupingBy { it }.eachCount())
    }

    private fun survivors(army: Army, casualties: Army): Army =
        Army(CreatureType.entries.associateWith { army.count(it) - casualties.count(it) }.filterValues { it > 0 })

    private fun Army.count(type: CreatureType): Int = creatures[type] ?: 0
}
