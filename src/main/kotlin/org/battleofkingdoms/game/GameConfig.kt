package org.battleofkingdoms.game

import org.battleofkingdoms.battle.Bonuses

data class GameConfig(
    val minPlayers: Int = 3,
    val maxPlayers: Int = 5,
    val startingHorde: Int = 0,
    val baseCreaturesPerTurn: Int = 1,
    val resourcesPerTurn: Int = 2,
    val tier1VsHorde: Int = 2,
    val tier2VsHorde: Int = 3,
    val sameTierCounter: Int = 1,
    val higherTierCounter: Int = 2,
    val victoryPointsToWin: Int = 5,
) {
    val bonuses: Bonuses get() = Bonuses(tier1VsHorde, tier2VsHorde, sameTierCounter, higherTierCounter)
}
