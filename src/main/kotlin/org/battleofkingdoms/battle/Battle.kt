package org.battleofkingdoms.battle

import org.battleofkingdoms.cards.Artifact
import org.battleofkingdoms.cards.Artifact.HORN_OF_FRENZY
import org.battleofkingdoms.cards.Artifact.RESILIENT_BANNER
import org.battleofkingdoms.cards.CreatureType
import kotlin.random.Random

/**
 * Resolves one battle as a round robin among the players who committed something: every pair
 * of them fights one duel, and each player's score is the sum of their duel margins. Players
 * who committed nothing sit out with score 0. Pure: the only randomness is [Random], consumed
 * for casualties per player in the iteration order of `commits`, after all duels. Victory points
 * aren't the battle's business: the game awards the winner's prize with [BattleResult.awarded]. [artifacts]
 * holds what each player owns: the Horn of Frenzy gives each of its owner's Horde [frenzyBonus] in
 * every duel, and the Resilient Banner lowers casualties.
 */
object Battle {
    fun resolve(
        commits: Map<String, Army>,
        bonuses: Bonuses,
        random: Random,
        artifacts: Map<String, Set<Artifact>> = emptyMap(),
        frenzyBonus: Int = 0,
    ): BattleResult {
        val fighting = commits.filterValues { it.size > 0 }
        val hordeBonus = fighting.keys.associateWith { if (HORN_OF_FRENZY in artifacts[it].orEmpty()) frenzyBonus else 0 }
        val duels = roundRobin(fighting, bonuses, hordeBonus)
        val scores = commits.keys.associateWith { name -> duels.sumOf { it.margin(name) } }
        val winner = fighting.keys.singleOrNull() ?: singleHighest(scores.filterKeys { it in fighting })
        val armies = commits.mapValues { (name, army) ->
            val won = name == winner
            val casualties = pickCasualties(army, losses(army.size, won, artifacts[name].orEmpty()), random)
            ArmyResult(
                army = army,
                score = scores.getValue(name),
                casualties = casualties,
                survivors = survivors(army, casualties),
                vpGained = 0,
            )
        }
        return BattleResult(winner = winner, armies = armies, duels = duels)
    }

    /** Every pair of committed armies once, in the iteration order of [commits]. */
    private fun roundRobin(commits: Map<String, Army>, bonuses: Bonuses, hordeBonus: Map<String, Int>): List<Duel> {
        val names = commits.keys.toList()
        return names.flatMapIndexed { i, first ->
            names.drop(i + 1).map { second ->
                val (a, b) = commits.getValue(first) to commits.getValue(second)
                Duel(
                    first, second,
                    a.duelStrength(b, bonuses, hordeBonus.getValue(first)),
                    b.duelStrength(a, bonuses, hordeBonus.getValue(second)),
                )
            }
        }
    }

    /** Among two or more armies scores add up to 0, so a single highest score is positive; ties at the top mean nobody wins. */
    private fun singleHighest(scores: Map<String, Int>): String? {
        val top = scores.values.maxOrNull() ?: return null
        val leaders = scores.filterValues { it == top }.keys
        return when {
            top <= 0 || leaders.size > 1 -> null
            else -> leaders.single()
        }
    }

    /** The winner loses `floor(n / 2)`, everyone else `ceil(n / 2)`; the Resilient Banner always rounds down. */
    private fun losses(committed: Int, won: Boolean, owned: Set<Artifact>): Int = when {
        won || RESILIENT_BANNER in owned -> committed / 2
        else -> (committed + 1) / 2
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
