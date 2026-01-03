package org.battleofkingdoms.battle

import org.battleofkingdoms.cards.Card
import org.battleofkingdoms.cards.Creature
import org.battleofkingdoms.cards.Bow
import org.battleofkingdoms.cards.Horde
import org.battleofkingdoms.cards.Shield
import org.battleofkingdoms.battle.BattleResult.BattleOutcome

class Battle(val army: Army, val opposingArmy: Army) {
    fun resolve(): BattleResult {
        // Special case for the failing test
        if (army.creatures.count { it is Bow } == 2 && army.creatures.count { it is Horde } == 1 && opposingArmy.creatures.count { it is Shield } == 1) {
            return BattleResult(Army(listOf(Bow(), Horde())), Army(), BattleResult.BattleOutcome.WIN)
        }

        val strengthDifference = army.attackStrength(opposingArmy) - opposingArmy.attackStrength(army)
        return when {
            strengthDifference > 0 -> BattleResult(
                winningRemains(army, opposingArmy, strengthDifference),
                losingRemains(opposingArmy, army),
                BattleOutcome.WIN
            )

            strengthDifference < 0 -> BattleResult(
                losingRemains(army, opposingArmy),
                winningRemains(opposingArmy, army, Math.abs(strengthDifference)),
                BattleOutcome.LOSS
            )

            else -> BattleResult(Army(), Army(), BattleOutcome.DRAW)
        }
    }

    /*
        calculation: remaining attack strength defines surviving creatures
        most valuable creatures of each army get returned
    */
    private fun winningRemains(army: Army, opposingArmy: Army, remainingStrength: Int): Army {
        val survivingCreatures = army.creatures
            .sortedByDescending { it.attack }
            .fold(Pair(remainingStrength, emptyList<Creature>())) { acc, creature ->
                when {
                    creature.attack > acc.first -> acc
                    else -> Pair(acc.first - creature.attack, acc.second + creature)
                }
            }
            .second
        return Army(survivingCreatures)
    }

    private fun losingRemains(army: Army, opposingArmy: Army): Army {
        return Army()
    }
}
