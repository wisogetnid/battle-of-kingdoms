package org.battleofkingdoms.game

import org.battleofkingdoms.battle.Army
import org.battleofkingdoms.battle.BattleResult
import org.battleofkingdoms.cards.CreatureType
import org.battleofkingdoms.cards.CreatureType.HORDE
import org.battleofkingdoms.cards.Hand
import org.battleofkingdoms.game.Phase.GAME_OVER
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val SEED = 7L
private const val MAX_TURNS = 20

/**
 * A seeded 3-player game played through engine calls only, scripted against the seeded deals:
 * turn 1 everyone commits 1 Horde (tie, the pot rolls over). On later turns, if ben can build
 * k units of some kind and anna k of the unit that counters it, both do and commit exactly
 * those, while cleo commits 1 Horde: equal army sizes, so anna wins through her counter bonus.
 * Otherwise everyone commits 1 Horde again.
 */
class FullGameTests {

    @Test
    fun `a seeded 3-player game with a rollover and counter wins plays to a winner`() {
        val game = play()

        assertEquals(GAME_OVER, game.phase)
        assertEquals("anna", game.winner)
        assertTrue(game.player("anna").vp >= 3)

        val first = game.battleLog.first()
        assertNull(first.winner)
        assertEquals(1, first.pot)
        assertEquals(2, game.battleLog[1].pot)
        val won = game.battleLog.filter { it.winner != null }
        assertTrue(won.isNotEmpty() && won.all(::decidedByCounter), "every win should come from a counter")
    }

    @Test
    fun `replaying the same seed and actions gives an identical battle log`() {
        assertEquals(play().battleLog, play().battleLog)
    }

    private fun play(): Game {
        var game = Game.create(GameConfig(), SEED, 3, "anna").join("ben").join("cleo")
        while (game.phase != GAME_OVER) {
            check(game.turn <= MAX_TURNS) { "Game didn't end within $MAX_TURNS turns" }
            val duel = if (game.turn == 1) null else counterDuel(game.player("anna").hand, game.player("ben").hand)
            val armies = game.players.keys.associateWith { Army.of(HORDE to 1) }.toMutableMap()
            if (duel != null) {
                val (bensUnit, annasUnit, k) = duel
                repeat(k) { game = game.upgrade("ben", bensUnit) }
                repeat(k) { game = game.upgrade("anna", annasUnit) }
                armies["ben"] = Army.of(bensUnit to k)
                armies["anna"] = Army.of(annasUnit to k)
            }
            game = game.players.keys.fold(game) { g, name -> g.ready(name) }
            game = game.players.keys.fold(game) { g, name -> g.commitArmy(name, armies.getValue(name)) }
        }
        return game
    }

    /** The largest matchup where ben builds `k` of a unit and anna `k` of the unit countering it, if any. */
    private fun counterDuel(annasHand: Hand, bensHand: Hand): Triple<CreatureType, CreatureType, Int>? =
        CreatureType.entries.mapNotNull { bensUnit ->
            val annasUnit = CreatureType.entries.find { it.counters == bensUnit } ?: return@mapNotNull null
            val k = minOf(
                bensHand.count(bensUnit.upgradeResource!!),
                annasHand.count(annasUnit.upgradeResource!!),
                bensHand.count(HORDE),
                annasHand.count(HORDE),
            )
            Triple(bensUnit, annasUnit, k)
        }.filter { it.third > 0 }.maxByOrNull { it.third }

    /** Won through a counter: without bonuses the winner's army wasn't strictly the largest. */
    private fun decidedByCounter(result: BattleResult): Boolean {
        val winnerSize = result.armies.getValue(result.winner!!).army.size
        return result.armies.any { (name, army) -> name != result.winner && army.army.size >= winnerSize }
    }
}
