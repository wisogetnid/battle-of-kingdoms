package org.battleofkingdoms.game

import org.battleofkingdoms.battle.Bonuses
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/** Pins the defaults to the spec, so the bonus literals in the battle tests can't drift from the config. */
class GameConfigTests {
    @Test
    fun `the default bonuses are the spec's table`() {
        assertEquals(
            Bonuses(tier1VsHorde = 2, tier2VsHorde = 5, sameTierCounter = 1, higherTierCounter = 3),
            GameConfig().bonuses,
        )
    }

    @Test
    fun `a game runs to 3 VP by default`() {
        assertEquals(3, GameConfig().victoryPointsToWin)
    }
}
