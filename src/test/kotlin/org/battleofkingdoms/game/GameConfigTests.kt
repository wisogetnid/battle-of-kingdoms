package org.battleofkingdoms.game

import org.battleofkingdoms.battle.Bonuses
import org.battleofkingdoms.cards.Artifact
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/** Pins the defaults to the spec, so the bonus literals in the battle tests can't drift from the config. */
class GameConfigTests {
    @Test
    fun `the default bonuses are the spec's table`() {
        assertEquals(
            Bonuses(
                tier1VsHorde = 1, tier1VsCountered = 1,
                tier2VsHorde = 4, tier2VsCounteredTier1 = 4, tier2VsOtherTier1 = 2, tier2VsCounteredTier2 = 2,
            ),
            GameConfig().bonuses,
        )
    }

    @Test
    fun `a game runs to 3 VP, 1 per battle, by default`() {
        assertEquals(3, GameConfig().victoryPointsToWin)
        assertEquals(1, GameConfig().victoryPointsPerBattle)
    }

    @Test
    fun `the stack starts with one of each artifact by default`() {
        assertEquals(Artifact.entries, GameConfig().artifacts)
    }

    @Test
    fun `the Crown gives 1 extra Horde by default`() {
        assertEquals(1, GameConfig().crownHorde)
    }
}
