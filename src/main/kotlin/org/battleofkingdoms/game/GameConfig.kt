package org.battleofkingdoms.game

import org.battleofkingdoms.battle.Bonuses
import org.battleofkingdoms.cards.Artifact

data class GameConfig(
    val minPlayers: Int = 3,
    val maxPlayers: Int = 5,
    val startingHorde: Int = 0,
    val baseCreaturesPerTurn: Int = 1,
    val resourcesPerTurn: Int = 2,
    val tier1VsHorde: Int = 1,
    val tier1VsCountered: Int = 1,
    val tier2VsHorde: Int = 4,
    val tier2VsCounteredTier1: Int = 4,
    val tier2VsOtherTier1: Int = 2,
    val tier2VsCounteredTier2: Int = 2,
    val victoryPointsPerBattle: Int = 1,
    val victoryPointsToWin: Int = 3,
    val crownHorde: Int = 1,
    /** The starting artifact stack; empty means every win is an immediate VP. */
    val artifacts: List<Artifact> = Artifact.entries,
) {
    val bonuses: Bonuses
        get() = Bonuses(
            tier1VsHorde, tier1VsCountered, tier2VsHorde, tier2VsCounteredTier1, tier2VsOtherTier1, tier2VsCounteredTier2,
        )
}
