package org.battleofkingdoms.simulator

import org.battleofkingdoms.cards.Artifact.FAMOUS_CROWN
import org.battleofkingdoms.cards.Artifact.HORN_OF_FRENZY
import org.battleofkingdoms.cards.Artifact.RESILIENT_BANNER
import org.battleofkingdoms.simulator.Tendency.AGGRESSIVE
import org.battleofkingdoms.simulator.Tendency.CAUTIOUS
import org.battleofkingdoms.simulator.strategies.HordeRush
import org.battleofkingdoms.simulator.strategies.RandomStrategy
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val STRATEGIES = mapOf(
    "random" to RandomStrategy,
    "horde-rush-cautious" to PlannedStrategy(HordeRush, CAUTIOUS),
    "horde-rush-aggressive" to PlannedStrategy(HordeRush, AGGRESSIVE),
)

private fun seat(id: String, n: Int) = Seat("$id-$n", id)

/**
 * Three games: aggressive wins a 3-player game holding the Horn; cautious wins a 4-player
 * game holding the Horn while random holds the Banner; a 5-player game is unfinished.
 */
private val RECORDS = listOf(
    GameRecord(
        gameSeed = 1,
        seats = listOf(seat("horde-rush-aggressive", 1), seat("random", 2), seat("horde-rush-cautious", 3)),
        winner = "horde-rush-aggressive-1",
        turns = 5,
        artifacts = mapOf("horde-rush-aggressive-1" to setOf(HORN_OF_FRENZY)),
    ),
    GameRecord(
        gameSeed = 2,
        seats = listOf(seat("horde-rush-aggressive", 1), seat("random", 2), seat("random", 3), seat("horde-rush-cautious", 4)),
        winner = "horde-rush-cautious-4",
        turns = 7,
        artifacts = mapOf("random-2" to setOf(RESILIENT_BANNER), "horde-rush-cautious-4" to setOf(HORN_OF_FRENZY)),
    ),
    GameRecord(
        gameSeed = 3,
        seats = listOf(seat("horde-rush-aggressive", 1), seat("horde-rush-cautious", 2), seat("random", 3), seat("random", 4), seat("random", 5)),
        winner = null,
        turns = MAX_TURNS,
        artifacts = mapOf("random-3" to setOf(FAMOUS_CROWN)),
    ),
)

class ReportTests {

    @Test
    fun `win and fair percentages count only finished games, fair by each table's player count`() {
        val rows = Report.rows(RECORDS.filter { it.finished }, STRATEGIES.keys) { it }.associateBy { it.label }

        val aggressive = rows.getValue("horde-rush-aggressive")
        assertEquals(2, aggressive.seats)
        assertEquals(1, aggressive.wins)
        assertEquals(50.0, aggressive.winPercent)
        assertEquals(100.0 * (1.0 / 3 + 1.0 / 4) / 2, aggressive.fairPercent, 1e-9)
        assertEquals(1.71, aggressive.ratio)

        val random = rows.getValue("random")
        assertEquals(3, random.seats)
        assertEquals(0, random.wins)
        assertEquals(0.0, random.ratio)
    }

    @Test
    fun `rows are sorted by ratio, rows without seats last`() {
        val rows = Report.rows(RECORDS.filter { it.finished }, listOf("cautious", "balanced", "aggressive")) {
            (STRATEGIES[it] as? PlannedStrategy)?.tendency?.id
        }

        assertEquals(listOf("aggressive", "cautious", "balanced"), rows.map { it.label })
        assertEquals(0, rows.last().seats)
    }

    @Test
    fun `the marker sits at a ratio of 1,3 or more and 0,7 or less`() {
        assertTrue(Row("x", seats = 40, wins = 13, fair = 10.0).marked)
        assertTrue(Row("x", seats = 40, wins = 7, fair = 10.0).marked)
        assertFalse(Row("x", seats = 40, wins = 12, fair = 10.0).marked)
        assertFalse(Row("x", seats = 40, wins = 8, fair = 10.0).marked)
        assertFalse(Row("x", seats = 0, wins = 0, fair = 0.0).marked)
    }

    @Test
    fun `the report shows the run, the game counts and turns per player count`() {
        val report = Report.render(RECORDS, RunInfo("abc1234 (dirty)", seed = 7, games = 3), STRATEGIES)
        val lines = report.lines()

        assertEquals("Battle of Kingdoms simulation", lines[0])
        assertEquals("commit abc1234 (dirty) · seed 7 · 3 games · 3–5 players · artifacts on", lines[1])
        assertEquals("finished 2 · unfinished 1 · avg 6.0 turns (3p 5.0 · 4p 7.0)", lines[2])
    }

    @Test
    fun `strategy, plan and tendency tables, with random only in the strategy table`() {
        val report = Report.render(RECORDS, RunInfo("abc1234", seed = 7, games = 3), STRATEGIES)
        val line = { label: String -> report.lines().filter { it.startsWith("$label ") } }

        assertEquals(1, line("random").size)
        assertContains(line("random").single(), "0.00  ◀")
        assertContains(line("horde-rush").single(), "     4      2")
        assertContains(line("balanced").single(), "—")
        assertContains(line("aggressive").single(), "1.71  ◀")
    }

    @Test
    fun `the artifact table counts finished games only`() {
        val report = Report.render(RECORDS, RunInfo("abc1234", seed = 7, games = 3), STRATEGIES)
        val line = { name: String -> report.lines().single { it.startsWith(name) }.split(Regex(" {2,}")) }

        assertEquals(listOf("HORN_OF_FRENZY", "100 %", "100 %"), line("HORN_OF_FRENZY"))
        assertEquals(listOf("RESILIENT_BANNER", "50 %", "0 %"), line("RESILIENT_BANNER"))
        assertEquals(listOf("FAMOUS_CROWN", "0 %", "—"), line("FAMOUS_CROWN"))
    }
}
