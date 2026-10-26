package org.battleofkingdoms.simulator

import org.battleofkingdoms.battle.Army
import org.battleofkingdoms.cards.CreatureType.HIGH_MAGE
import org.battleofkingdoms.game.Action
import org.battleofkingdoms.game.Take
import org.battleofkingdoms.server.PlayerView
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val GAME_SEED = 11L
private val BOT_SEEDS = listOf(1L, 2L, 3L, 4L, 5L)

/** Gets ready at once and never commits anything. */
private object Passive : Strategy {
    override fun buildUp(view: PlayerView, random: Random) = BuildUpMove.Ready
    override fun commit(view: PlayerView, random: Random) = Army()
    override fun claim(view: PlayerView, random: Random) = Take.VICTORY_POINT
}

/** Commits its whole hand every battle, so alone it wins every pot. */
private object AllIn : Strategy {
    override fun buildUp(view: PlayerView, random: Random) = BuildUpMove.Ready
    override fun commit(view: PlayerView, random: Random) = view.army()
    override fun claim(view: PlayerView, random: Random) = Take.VICTORY_POINT
}

/** Nobody holds a Mage on turn 1, so this upgrade is always illegal. */
private object Cheater : Strategy {
    override fun buildUp(view: PlayerView, random: Random) = BuildUpMove.Upgrade(HIGH_MAGE)
    override fun commit(view: PlayerView, random: Random) = Army()
    override fun claim(view: PlayerView, random: Random) = Take.VICTORY_POINT
}

/** Plays like [inner] and keeps every view it is given, by kind of call. */
private class Recorder(private val inner: Strategy) : Strategy {
    val views = mutableListOf<Pair<String, PlayerView>>()
    override fun buildUp(view: PlayerView, random: Random) = inner.buildUp(view, random).also { views += "buildUp" to view }
    override fun commit(view: PlayerView, random: Random) = inner.commit(view, random).also { views += "commit" to view }
    override fun claim(view: PlayerView, random: Random) = inner.claim(view, random).also { views += "claim" to view }
}

class SimulationTests {

    @Test
    fun `a lone committer wins every pot and the game finishes`() {
        val record = play("all-in" to AllIn, "passive" to Passive, "passive" to Passive)

        assertEquals("all-in-1", record.winner)
        assertTrue(record.finished)
        assertEquals(listOf("all-in-1", "passive-2", "passive-3"), record.seats.map { it.name })
        assertEquals(3, record.playerCount)
    }

    @Test
    fun `a game where nobody commits stops unfinished at the turn cap`() {
        val record = play("passive" to Passive, "passive" to Passive, "passive" to Passive)

        assertNull(record.winner)
        assertEquals(MAX_TURNS, record.turns)
    }

    @Test
    fun `an illegal move aborts with the strategy, game seed and turn`() {
        val error = assertFailsWith<BotError> { play("passive" to Passive, "cheater" to Cheater, "passive" to Passive) }

        val message = assertNotNull(error.message)
        assertContains(message, "cheater")
        assertContains(message, "game $GAME_SEED")
        assertContains(message, "turn 1")
        assertContains(message, "Upgrade(into=HIGH_MAGE)")
    }

    @Test
    fun `each call gets its own seat's current player view`() {
        val recorder = Recorder(AllIn)
        play("recorder" to recorder, "passive" to Passive, "passive" to Passive)

        assertTrue(recorder.views.all { (_, view) -> view.me.name == "recorder-1" })
        recorder.views.filter { it.first == "commit" }.forEach { (_, view) -> assertEquals(listOf(Action.COMMIT_ARMY), view.availableActions) }
        recorder.views.filter { it.first == "claim" }.forEach { (_, view) -> assertNotNull(view.me.drawnArtifact) }
        assertTrue(recorder.views.any { it.first == "claim" })
    }

    @Test
    fun `a seat committing after another sees that it acted, but not what it committed`() {
        val recorder = Recorder(Passive)
        play("all-in" to AllIn, "recorder" to recorder, "passive" to Passive)

        val (_, before) = recorder.views.first { it.first == "buildUp" }
        val (_, during) = recorder.views.first { it.first == "commit" }
        val committer = during.others.single { it.name == "all-in-1" }
        assertTrue(committer.acted)
        assertEquals(emptyList(), during.battleLog)
        // committed creatures stay in the hand until the battle resolves, so the hand size gives nothing away
        assertEquals(before.others.single { it.name == "all-in-1" }.handSize, committer.handSize)
    }

    @Test
    fun `the same seeds play the same game`() {
        val lineup = arrayOf("all-in" to AllIn, "passive" to Passive, "all-in" to AllIn)

        assertEquals(play(*lineup), play(*lineup))
    }

    @Test
    fun `a longer run with the same seed replays the shorter one first`() {
        val simulation = Simulation()

        assertEquals(simulation.run(runSeed = 3, games = 20), simulation.run(runSeed = 3, games = 40).take(20))
    }

    @Test
    fun `any game of a run can be replayed from its game seed`() {
        val simulation = Simulation()

        simulation.run(runSeed = 5, games = 10).forEach { assertEquals(it, simulation.play(it.gameSeed)) }
    }

    @Test
    fun `different run seeds share no games`() {
        val seeds = { runSeed: Long -> Seeds.games(runSeed, 2000).toSet() }

        assertTrue((seeds(1) intersect seeds(2)).isEmpty())
    }

    @Test
    fun `every table has four seats, never all the same strategy`() {
        val tables = (1L..1000L).map { Simulation(mapOf("a" to Passive, "b" to AllIn)).table(it) }

        assertTrue(tables.all { it.size == PLAYERS })
        assertTrue(tables.none { it.toSet().size == 1 })
    }

    @Test
    fun `no table seats a build plan twice, whatever the tendency`() {
        val tables = (1L..1000L).map { Simulation().table(it) }
        val hordeRushSeats = tables.map { table -> table.count { it.startsWith("horde-rush") } }

        assertTrue(hordeRushSeats.all { it <= 1 })
        assertTrue(hordeRushSeats.any { it == 1 })
    }

    @Test
    fun `the tables of a run depend on the strategy ids and plans, not on how the strategies play`() {
        val tables = { strategies: Map<String, Strategy> ->
            Simulation(strategies).run(runSeed = 9, games = 30).map { record -> record.seats.map { it.strategyId } }
        }
        val playingDifferently = registry.mapValues { (_, strategy) ->
            if (strategy is PlannedStrategy) PlannedStrategy(strategy.plan, Tendency.CAUTIOUS) else AllIn
        }

        assertEquals(tables(registry), tables(playingDifferently))
    }

    @Test
    fun `a simulation needs at least two strategies, and enough plans to fill a table`() {
        assertFailsWith<IllegalArgumentException> { Simulation(mapOf("passive" to Passive)) }
        val threePlans = registry.filterKeys { id -> listOf("horde-rush", "mono-line", "hoarder").any { id.startsWith(it) } }
        assertFailsWith<IllegalArgumentException> { Simulation(threePlans) }
    }

    private fun play(vararg lineup: Pair<String, Strategy>): GameRecord =
        playGame(lineup.toList(), GAME_SEED, engineSeed = 42L, botSeeds = BOT_SEEDS)
}
