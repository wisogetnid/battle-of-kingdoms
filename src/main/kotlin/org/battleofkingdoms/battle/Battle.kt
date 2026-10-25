package org.battleofkingdoms.battle

import org.battleofkingdoms.cards.CreatureType
import kotlin.random.Random

/**
 * Resolves one battle as a round robin: every pair of players fights one duel, and each
 * player's score is the sum of their duel margins. Pure: the only randomness is [Random],
 * consumed for casualties per player in the iteration order of `commits`, after all duels.
 */
object Battle {
    fun resolve(commits: Map<String, Army>, pot: Int, counterBonus: Int, random: Random): BattleResult {
        val duels = roundRobin(commits, counterBonus)
        val scores = commits.keys.associateWith { name -> duels.sumOf { it.margin(name) } }
        val winner = singleHighest(scores)
        val armies = commits.mapValues { (name, army) ->
            val won = name == winner
            val casualties = pickCasualties(army, losses(army.size, won), random)
            ArmyResult(
                army = army,
                score = scores.getValue(name),
                casualties = casualties,
                survivors = survivors(army, casualties),
                vpGained = if (won) pot else 0,
            )
        }
        return BattleResult(pot, winner, armies, duels)
    }

    /** Every pair once, in the iteration order of [commits]; empty armies fight at strength 0. */
    private fun roundRobin(commits: Map<String, Army>, counterBonus: Int): List<Duel> {
        val names = commits.keys.toList()
        return names.flatMapIndexed { i, first ->
            names.drop(i + 1).map { second ->
                val (a, b) = commits.getValue(first) to commits.getValue(second)
                Duel(first, second, a.duelStrength(b, counterBonus), b.duelStrength(a, counterBonus))
            }
        }
    }

    /** Scores add up to 0, so a single highest score is positive; ties at the top mean nobody wins. */
    private fun singleHighest(scores: Map<String, Int>): String? {
        val top = scores.values.maxOrNull() ?: return null
        val leaders = scores.filterValues { it == top }.keys
        return when {
            top <= 0 || leaders.size > 1 -> null
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
