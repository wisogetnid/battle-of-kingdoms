package org.battleofkingdoms.game.battle

import org.battleofkingdoms.cards.creatures.Creature
import org.battleofkingdoms.game.battle.BattleResult.BattleOutcome

class Battle(val army: Army, val opposingArmy: Army) {
    fun resolve(): BattleResult {
        val strengthDifference = army.attackStrength(opposingArmy) - opposingArmy.attackStrength(army)
        return when {
            strengthDifference > 0 -> BattleResult(winningRemains(army, opposingArmy, strengthDifference), losingRemains(opposingArmy, army), BattleOutcome.WIN)
            strengthDifference < 0 -> BattleResult(losingRemains(army, opposingArmy), winningRemains(opposingArmy, army, Math.abs(strengthDifference)), BattleOutcome.LOSS)
            else -> BattleResult(Army(), Army(), BattleOutcome.DRAW)
        }
    }

    /*
        calculation: remaining attack strength defines surviving creatures
        most valuable creatures of each army get returned
    */
    private fun winningRemains(army: Army, opposingArmy: Army, remainingStrength: Int): Army {
        return Army(
            army.creatures
            .sortedByDescending { it.value() }
            .fold(Pair(remainingStrength, emptyList<Creature>())) { acc, creature ->
                when {
                    creature.attack(opposingArmy) > acc.first -> acc
                    else -> Pair(acc.first - creature.attack(opposingArmy), acc.second + creature)
                }
            }
            .second
        )
    }

    private fun losingRemains(army: Army, opposingArmy: Army): Army {
        return Army()
    }
}
