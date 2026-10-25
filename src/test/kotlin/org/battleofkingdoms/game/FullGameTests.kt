package org.battleofkingdoms.game

import org.battleofkingdoms.battle.Army
import org.battleofkingdoms.cards.CreatureType
import org.battleofkingdoms.cards.CreatureType.HORDE
import org.battleofkingdoms.cards.ResourceType
import org.battleofkingdoms.game.Phase.GAME_OVER
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val SEED = 7L
private const val MAX_TURNS = 30

/**
 * A seeded 3-player game played through engine calls only, scripted against the seeded deals:
 * every turn ben and cleo trade all their resources for Horde and commit 1 Horde. anna upgrades
 * every Horde she can into tier-1 units and keeps them home, upgrades settled tier-1 units into
 * tier 2 when she draws the resource, and commits only her tier-2 units. While she has none she
 * commits nothing, so ben and cleo tie and the pot rolls over (always on turn 1).
 */
class FullGameTests {

    @Test
    fun `a seeded 3-player game with a rollover, trades and tier-2 units plays to 3 VP`() {
        val (game, trades) = play()

        assertEquals(GAME_OVER, game.phase)
        assertEquals("anna", game.winner)
        assertTrue(game.player("anna").vp >= 3)
        assertTrue(trades > 0, "ben and cleo should have traded")

        val first = game.battleLog.first()
        assertNull(first.winner)
        assertEquals(1, first.pot)
        assertEquals(2, game.battleLog[1].pot)
        val annasArmies = game.battleLog.map { it.armies.getValue("anna").army }
        assertTrue(annasArmies.any { army -> army.creatures.keys.any { it.tier == 2 } }, "anna should field a tier-2 unit")
    }

    @Test
    fun `replaying the same seed and actions gives an identical battle log`() {
        assertEquals(play().first.battleLog, play().first.battleLog)
    }

    private fun play(): Pair<Game, Int> {
        var game = Game.create(GameConfig(), SEED, 3, "anna").join("ben").join("cleo")
        var trades = 0
        while (game.phase != GAME_OVER) {
            check(game.turn <= MAX_TURNS) { "Game didn't end within $MAX_TURNS turns" }
            game = upgradeAll(game, "anna")
            listOf("ben", "cleo").forEach { name ->
                val resources = ResourceType.entries.flatMap { r -> List(game.player(name).hand.count(r)) { r } }
                resources.forEach { game = game.trade(name, it) }
                trades += resources.size
            }
            game = game.players.keys.fold(game) { g, name -> g.ready(name) }
            val armies = mapOf("anna" to annasArmy(game), "ben" to Army.of(HORDE to 1), "cleo" to Army.of(HORDE to 1))
            game = game.players.keys.fold(game) { g, name -> g.commitArmy(name, armies.getValue(name)) }
        }
        return game to trades
    }

    /** Tier 2 first for every settled tier-1 unit with its resource, then every Horde into tier 1. */
    private tailrec fun upgradeAll(game: Game, name: String): Game {
        val player = game.player(name)
        val tier2 = CreatureType.entries.firstOrNull { it.tier == 2 && player.settled(it.upgradesFrom!!) > 0 && player.hand.has(it.upgradeResource!!) }
        val tier1 = CreatureType.entries.firstOrNull { it.tier == 1 && player.hand.has(it.upgradeResource!!) && player.hand.has(HORDE) }
        return when (val into = tier2 ?: tier1) {
            null -> game
            else -> upgradeAll(game.upgrade(name, into), name)
        }
    }

    /** anna's tier-2 units; nothing while she has none. */
    private fun annasArmy(game: Game): Army {
        val hand = game.player("anna").hand
        return Army(CreatureType.entries.filter { it.tier == 2 && hand.has(it) }.associateWith { hand.count(it) })
    }
}
