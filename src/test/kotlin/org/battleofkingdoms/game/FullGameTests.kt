package org.battleofkingdoms.game

import org.battleofkingdoms.battle.Army
import org.battleofkingdoms.cards.CreatureType
import org.battleofkingdoms.cards.CreatureType.BOW
import org.battleofkingdoms.cards.CreatureType.HORDE
import org.battleofkingdoms.cards.CreatureType.SHIELD
import org.battleofkingdoms.cards.ResourceType.IRON
import org.battleofkingdoms.cards.ResourceType.WOOD
import org.battleofkingdoms.game.Phase.GAME_OVER
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val SEED = 7L
private const val MAX_TURNS = 20

/**
 * A seeded 3-player game played through engine calls only:
 * turn 1 everyone commits 1 Horde (tie, the pot rolls over); from turn 2 anna upgrades
 * whatever she can and commits everything, while ben and cleo commit 1 Horde each.
 */
class FullGameTests {

    @Test
    fun `a seeded 3-player game with a rollover plays to a winner`() {
        val game = play()

        assertEquals(GAME_OVER, game.phase)
        assertEquals("anna", game.winner)
        assertEquals(3, game.player("anna").vp)

        val first = game.battleLog.first()
        assertNull(first.winner)
        assertEquals(1, first.pot)
        assertEquals(2, game.battleLog[1].pot)
        assertTrue(game.battleLog.drop(1).all { it.winner == "anna" })
    }

    @Test
    fun `replaying the same seed and actions gives an identical battle log`() {
        assertEquals(play().battleLog, play().battleLog)
    }

    private fun play(): Game {
        var game = Game.create(GameConfig(), SEED, 3, "anna").join("ben").join("cleo")
        while (game.phase != GAME_OVER) {
            check(game.turn <= MAX_TURNS) { "Game didn't end within $MAX_TURNS turns" }
            game = buildUp(game)
            game = game.players.keys.fold(game) { g, name -> g.commitArmy(name, army(g, name)) }
        }
        return game
    }

    private fun buildUp(game: Game): Game {
        val upgraded = when (game.turn) {
            1 -> game
            else -> upgradeAll(game, "anna")
        }
        return upgraded.players.keys.fold(upgraded) { g, name -> g.ready(name) }
    }

    private fun upgradeAll(game: Game, name: String): Game {
        val hand = game.player(name).hand
        return when {
            !hand.has(HORDE) -> game
            hand.has(IRON) -> upgradeAll(game.upgrade(name, BOW), name)
            hand.has(WOOD) -> upgradeAll(game.upgrade(name, SHIELD), name)
            else -> game
        }
    }

    private fun army(game: Game, name: String): Army = when {
        game.turn == 1 || name != "anna" -> Army.of(HORDE to 1)
        else -> Army(CreatureType.entries.associateWith { game.player(name).hand.count(it) }.filterValues { it > 0 })
    }
}
