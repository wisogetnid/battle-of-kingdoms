package org.battleofkingdoms.game

import org.battleofkingdoms.battle.Army
import org.battleofkingdoms.cards.CreatureType.BOW
import org.battleofkingdoms.cards.CreatureType.HORDE
import org.battleofkingdoms.cards.Hand
import org.battleofkingdoms.cards.ResourceType
import org.battleofkingdoms.cards.ResourceType.WOOD
import org.battleofkingdoms.game.Phase.BATTLE
import org.battleofkingdoms.game.Phase.BUILD_UP
import org.battleofkingdoms.game.Phase.GAME_OVER
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val SEED = 42L

/** No artifacts, so every win pays its VP straight away; the claim is covered in [ClaimTests]. */
private val NO_ARTIFACTS = GameConfig(artifacts = emptyList())

class BattleTurnTests {

    @Test
    fun `committing more creatures than owned is a rule violation`() {
        val game = battle(anna = Hand.of(HORDE to 2))

        assertFailsWith<RuleViolation> { game.commitArmy("anna", Army.of(HORDE to 3)) }
        assertFailsWith<RuleViolation> { game.commitArmy("anna", Army.of(BOW to 1)) }
    }

    @Test
    fun `committing twice is rejected`() {
        val game = battle().commitArmy("anna", Army())

        assertFailsWith<AlreadyActed> { game.commitArmy("anna", Army()) }
    }

    @Test
    fun `committing outside the battle is rejected`() {
        val buildUp = started()

        assertFailsWith<WrongPhase> { buildUp.commitArmy("anna", Army()) }
    }

    @Test
    fun `committing as an unknown player is rejected`() {
        assertFailsWith<UnknownPlayer> { battle().commitArmy("zed", Army()) }
    }

    @Test
    fun `a commit stays hidden until everyone has committed`() {
        val before = battle(anna = Hand.of(HORDE to 3))
        val game = before.commitArmy("anna", Army.of(HORDE to 2)).commitArmy("ben", Army())

        assertEquals(BATTLE, game.phase)
        assertEquals(Hand.of(HORDE to 3), game.player("anna").hand)
        assertTrue(game.battleLog.isEmpty())
    }

    @Test
    fun `the winner gains the battle's VP`() {
        val game = battle(anna = Hand.of(HORDE to 3), ben = Hand.of(HORDE to 1), cleo = Hand.of(HORDE to 1))
            .commitArmy("anna", Army.of(HORDE to 3))
            .commitArmy("ben", Army.of(HORDE to 1))
            .commitArmy("cleo", Army())

        val entry = game.battleLog.single()
        assertEquals("anna", entry.winner)
        assertEquals(1, game.player("anna").vp)
        assertEquals(0, game.player("ben").vp)
    }

    @Test
    fun `survivors return to the hand and casualties are gone`() {
        val game = battle(anna = Hand.of(HORDE to 4, WOOD to 1), ben = Hand.of(HORDE to 1))
            .commitArmy("anna", Army.of(HORDE to 3))
            .commitArmy("ben", Army.of(HORDE to 1))
            .commitArmy("cleo", Army())

        // turn 2 income: 1 Horde, 2 resources
        val anna = game.player("anna")
        assertEquals(1 + 2 + 1, anna.hand.count(HORDE)) // 1 kept back, 3 - floor(3/2) = 2 survivors, 1 income
        assertEquals(1 + 2, ResourceType.entries.sumOf { anna.hand.count(it) })
        assertEquals(0 + 1, game.player("ben").hand.count(HORDE)) // 1 - ceil(1/2) = 0 survivors, 1 income
    }

    @Test
    fun `a tie gives nothing and the next battle is still worth 1`() {
        val game = battle(anna = Hand.of(HORDE to 1), ben = Hand.of(HORDE to 1))
            .commitArmy("anna", Army.of(HORDE to 1))
            .commitArmy("ben", Army.of(HORDE to 1))
            .commitArmy("cleo", Army())

        assertNull(game.battleLog.single().winner)
        assertEquals(0, game.players.values.sumOf { it.vp })
        assertEquals(1, game.battleLog.single().pot)
        assertEquals(1, game.pot)
    }

    @Test
    fun `after a win the next battle is worth 1 again`() {
        val game = battle(anna = Hand.of(HORDE to 1))
            .commitArmy("anna", Army.of(HORDE to 1))
            .commitArmy("ben", Army())
            .commitArmy("cleo", Army())

        assertEquals(1, game.pot)
    }

    @Test
    fun `a lone committer's uncontested win scores the battle's VP`() {
        val game = battle(anna = Hand.of(HORDE to 1))
            .commitArmy("anna", Army.of(HORDE to 1))
            .commitArmy("ben", Army())
            .commitArmy("cleo", Army())

        assertEquals("anna", game.battleLog.single().winner)
        assertEquals(1, game.player("anna").vp)
    }

    @Test
    fun `the VP per battle comes from the config`() {
        val game = battle(anna = Hand.of(HORDE to 1), config = NO_ARTIFACTS.copy(victoryPointsPerBattle = 2))
            .commitArmy("anna", Army.of(HORDE to 1))
            .commitArmy("ben", Army())
            .commitArmy("cleo", Army())

        assertEquals(2, game.player("anna").vp)
        assertEquals(2, game.battleLog.single().pot)
    }

    @Test
    fun `the next turn starts build-up with the same income for everyone`() {
        val game = battle(anna = Hand.of(HORDE to 1), ben = Hand())
            .commitArmy("anna", Army.of(HORDE to 1))
            .commitArmy("ben", Army())
            .commitArmy("cleo", Army())

        assertEquals(BUILD_UP, game.phase)
        assertEquals(2, game.turn)
        assertTrue(game.players.values.none { it.acted })
        assertEquals(1 + 1, game.player("anna").hand.count(HORDE)) // survivor + income: fighting earns nothing extra
        assertEquals(1, game.player("ben").hand.count(HORDE)) // income only
        assertTrue(game.players.values.all { player -> ResourceType.entries.sumOf { player.hand.count(it) } >= 2 })
    }

    @Test
    fun `reaching victoryPointsToWin ends the game with a winner`() {
        val game = battle(anna = Hand.of(HORDE to 1), config = NO_ARTIFACTS.copy(victoryPointsToWin = 1))
            .commitArmy("anna", Army.of(HORDE to 1))
            .commitArmy("ben", Army())
            .commitArmy("cleo", Army())

        assertEquals(GAME_OVER, game.phase)
        assertEquals("anna", game.winner)
        assertFailsWith<WrongPhase> { game.ready("anna") }
    }

    private fun started(config: GameConfig = NO_ARTIFACTS) =
        Game.create(config, SEED, 3, "anna").join("ben").join("cleo")

    /** A game in the battle of turn 1 with the given hands (others keep their income). */
    private fun battle(
        anna: Hand? = null,
        ben: Hand? = null,
        cleo: Hand? = null,
        config: GameConfig = NO_ARTIFACTS,
    ): Game {
        val game = started(config).let { g -> g.players.keys.fold(g) { acc, name -> acc.ready(name) } }
        val hands = mapOf("anna" to anna, "ben" to ben, "cleo" to cleo)
        return game.copy(players = game.players.mapValues { (name, player) ->
            hands[name]?.let { player.copy(hand = it) } ?: player
        })
    }
}
