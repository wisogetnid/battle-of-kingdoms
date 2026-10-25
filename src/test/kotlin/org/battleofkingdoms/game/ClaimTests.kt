package org.battleofkingdoms.game

import org.battleofkingdoms.battle.Army
import org.battleofkingdoms.battle.Prize
import org.battleofkingdoms.cards.Artifact
import org.battleofkingdoms.battle.Duel
import org.battleofkingdoms.cards.Artifact.FAMOUS_CROWN
import org.battleofkingdoms.cards.Artifact.HORN_OF_FRENZY
import org.battleofkingdoms.cards.Artifact.RESILIENT_BANNER
import org.battleofkingdoms.cards.CreatureType.HORDE
import org.battleofkingdoms.game.Phase.BUILD_UP
import org.battleofkingdoms.game.Phase.CLAIM
import org.battleofkingdoms.game.Phase.GAME_OVER
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val SEED = 42L
private val ALL_ARTIFACTS = GameConfig(artifacts = Artifact.entries)

class ClaimTests {

    @Test
    fun `a winner draws an artifact from the stack and the game waits for their claim`() {
        val game = annaWins(started(ALL_ARTIFACTS))

        assertEquals(CLAIM, game.phase)
        assertNotNull(game.drawnArtifact)
        assertEquals(Artifact.entries.toSet(), game.artifactStack)
        assertNull(game.battleLog.single().prize)
        assertEquals(listOf(Action.CLAIM), game.availableActions("anna"))
        assertEquals(emptyList(), game.availableActions("ben"))
        assertEquals(emptyList(), game.availableActions("cleo"))
    }

    @Test
    fun `taking the artifact gives it to the winner and no VP`() {
        val claiming = annaWins(started(ALL_ARTIFACTS))
        val drawn = claiming.drawnArtifact!!

        val game = claiming.claim("anna", Take.ARTIFACT)

        assertEquals(setOf(drawn), game.player("anna").artifacts)
        assertEquals(0, game.player("anna").vp)
        assertEquals(Artifact.entries.toSet() - drawn, game.artifactStack)
        assertEquals(Prize.Taken(drawn), game.battleLog.single().prize)
        assertEquals(0, game.battleLog.single().armies.getValue("anna").vpGained)
        assertNull(game.drawnArtifact)
        assertEquals(BUILD_UP, game.phase)
        assertEquals(2, game.turn)
    }

    @Test
    fun `taking the VP leaves the artifact in the stack`() {
        val game = annaWins(started(ALL_ARTIFACTS)).claim("anna", Take.VICTORY_POINT)

        assertEquals(1, game.player("anna").vp)
        assertEquals(emptySet(), game.player("anna").artifacts)
        assertEquals(Artifact.entries.toSet(), game.artifactStack)
        assertEquals(Prize.VictoryPoint, game.battleLog.single().prize)
        assertEquals(1, game.battleLog.single().armies.getValue("anna").vpGained)
        assertNull(game.drawnArtifact)
        assertEquals(BUILD_UP, game.phase)
    }

    @Test
    fun `a declined artifact can be drawn again`() {
        val config = GameConfig(artifacts = listOf(RESILIENT_BANNER))
        val first = annaWins(started(config))
        assertEquals(RESILIENT_BANNER, first.drawnArtifact)

        val second = annaWins(first.claim("anna", Take.VICTORY_POINT))

        assertEquals(RESILIENT_BANNER, second.drawnArtifact)
    }

    @Test
    fun `a tie draws nothing and the next turn starts`() {
        val game = everyoneCommits(started(ALL_ARTIFACTS), "anna" to 1, "ben" to 1)

        assertNull(game.battleLog.single().winner)
        assertNull(game.drawnArtifact)
        assertEquals(BUILD_UP, game.phase)
        assertEquals(Artifact.entries.toSet(), game.artifactStack)
    }

    @Test
    fun `with an empty stack the winner gains the VP straight away`() {
        val game = annaWins(started(GameConfig(artifacts = emptyList())))

        assertEquals(BUILD_UP, game.phase)
        assertEquals(1, game.player("anna").vp)
        assertEquals(Prize.VictoryPoint, game.battleLog.single().prize)
    }

    @Test
    fun `the stack runs out once every artifact is owned`() {
        val config = GameConfig(artifacts = listOf(FAMOUS_CROWN))
        val owned = annaWins(started(config)).claim("anna", Take.ARTIFACT)

        val game = annaWins(owned)

        assertEquals(emptySet(), game.artifactStack)
        assertEquals(BUILD_UP, game.phase)
        assertEquals(1, game.player("anna").vp)
    }

    @Test
    fun `the game only ends after a VP prize`() {
        val config = GameConfig(artifacts = listOf(FAMOUS_CROWN), victoryPointsToWin = 1)
        val claiming = annaWins(started(config))
        assertEquals(CLAIM, claiming.phase)

        val tookArtifact = claiming.claim("anna", Take.ARTIFACT)
        assertEquals(BUILD_UP, tookArtifact.phase)

        val game = annaWins(tookArtifact)
        assertEquals(GAME_OVER, game.phase)
        assertEquals("anna", game.winner)
    }

    @Test
    fun `taking the VP can end the game`() {
        val config = GameConfig(artifacts = Artifact.entries, victoryPointsToWin = 1)

        val game = annaWins(started(config)).claim("anna", Take.VICTORY_POINT)

        assertEquals(GAME_OVER, game.phase)
        assertEquals("anna", game.winner)
    }

    @Test
    fun `a Famous Crown taken in battle 1 pays from turn 2`() {
        val claiming = annaWins(started(GameConfig(artifacts = listOf(FAMOUS_CROWN))))
        val hordeBefore = claiming.player("anna").hand.count(HORDE)

        val game = claiming.claim("anna", Take.ARTIFACT)

        assertEquals(hordeBefore + 1 + 1, game.player("anna").hand.count(HORDE))
        assertEquals(claiming.player("ben").hand.count(HORDE) + 1, game.player("ben").hand.count(HORDE))
    }

    @Test
    fun `a Horn of Frenzy taken in battle 1 strengthens its owner's Horde from battle 2`() {
        val game = annaWins(started(GameConfig(artifacts = listOf(HORN_OF_FRENZY)))).claim("anna", Take.ARTIFACT)

        val battle2 = everyoneCommits(game, "anna" to 1, "ben" to 1)

        assertEquals(listOf(Duel("anna", "ben", 2, 1)), battle2.battleLog.last().duels)
        assertEquals("anna", battle2.battleLog.last().winner)
    }

    @Test
    fun `only the winner can claim, and only during CLAIM`() {
        val claiming = annaWins(started(ALL_ARTIFACTS))

        assertFailsWith<NotTheWinner> { claiming.claim("ben", Take.VICTORY_POINT) }
        assertFailsWith<UnknownPlayer> { claiming.claim("zed", Take.VICTORY_POINT) }
        assertFailsWith<WrongPhase> { started(ALL_ARTIFACTS).claim("anna", Take.VICTORY_POINT) }
        assertFailsWith<WrongPhase> { claiming.ready("anna") }
    }

    @Test
    fun `the draw comes from the seeded random`() {
        val draws = (1L..30L).map { seed -> annaWins(started(ALL_ARTIFACTS, seed)).drawnArtifact }

        assertEquals(draws, (1L..30L).map { seed -> annaWins(started(ALL_ARTIFACTS, seed)).drawnArtifact })
        assertTrue(draws.toSet() == Artifact.entries.toSet(), "every artifact should be drawn for some seed: $draws")
    }

    private fun started(config: GameConfig, seed: Long = SEED) =
        Game.create(config, seed, 3, "anna").join("ben").join("cleo")

    /** Everyone readies; anna commits 1 Horde and the others nothing, so she wins uncontested. */
    private fun annaWins(game: Game) = everyoneCommits(game, "anna" to 1)

    private fun everyoneCommits(game: Game, vararg horde: Pair<String, Int>): Game {
        val ready = game.players.keys.fold(game) { g, name -> g.ready(name) }
        val armies = horde.toMap()
        return ready.players.keys.fold(ready) { g, name ->
            g.commitArmy(name, armies[name]?.let { Army.of(HORDE to it) } ?: Army())
        }
    }
}
