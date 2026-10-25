package org.battleofkingdoms.game

import org.battleofkingdoms.cards.CreatureType.HORDE
import org.battleofkingdoms.cards.Hand
import org.battleofkingdoms.cards.ResourceType
import org.battleofkingdoms.game.Phase.BUILD_UP
import org.battleofkingdoms.game.Phase.WAITING_FOR_PLAYERS
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private const val SEED = 42L

class LobbyTests {
    private val config = GameConfig()

    @ParameterizedTest
    @ValueSource(ints = [3, 4, 5])
    fun `a game for 3 to 5 players waits with the host joined`(playerCount: Int) {
        val game = Game.create(config, SEED, playerCount, "anna")

        assertEquals(WAITING_FOR_PLAYERS, game.phase)
        assertEquals(listOf("anna"), game.players.keys.toList())
        assertEquals(0, game.turn)
        assertEquals(0, game.pot)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 2, 6])
    fun `a game outside 3 to 5 players is a rule violation`(playerCount: Int) {
        assertFailsWith<RuleViolation> { Game.create(config, SEED, playerCount, "anna") }
    }

    @Test
    fun `player limits come from the config`() {
        val duel = config.copy(minPlayers = 2)

        assertEquals(WAITING_FOR_PLAYERS, Game.create(duel, SEED, 2, "anna").phase)
    }

    @Test
    fun `joining with a taken name is rejected`() {
        val game = Game.create(config, SEED, 3, "anna").join("ben")

        assertFailsWith<DuplicatePlayer> { game.join("ben") }
    }

    @Test
    fun `joining with a blank name is a rule violation`() {
        val game = Game.create(config, SEED, 3, "anna")

        assertFailsWith<RuleViolation> { game.join(" ") }
    }

    @Test
    fun `joining a full game is rejected`() {
        val game = Game.create(config, SEED, 3, "anna").join("ben").join("cleo")

        assertFailsWith<WrongPhase> { game.join("dora") }
    }

    @Test
    fun `the game starts turn 1 with a pot of 1 when the last player joins`() {
        val waiting = Game.create(config, SEED, 3, "anna").join("ben")
        val started = waiting.join("cleo")

        assertEquals(WAITING_FOR_PLAYERS, waiting.phase)
        assertEquals(BUILD_UP, started.phase)
        assertEquals(1, started.turn)
        assertEquals(1, started.pot)
        assertEquals(listOf("anna", "ben", "cleo"), started.players.keys.toList())
    }
}

class IncomeTests {
    @Test
    fun `turn 1 income is starting horde plus base creatures plus seeded resources`() {
        val game = startedGame(GameConfig())

        game.players.values.forEach { player ->
            assertEquals(3 + 1, player.hand.count(HORDE))
            assertEquals(2, resources(player.hand))
            assertEquals(0, player.renown)
            assertEquals(false, player.acted)
        }
    }

    @Test
    fun `income amounts come from the config`() {
        val config = GameConfig(startingHorde = 5, baseCreaturesPerTurn = 2, resourcesPerTurn = 4)
        val game = startedGame(config)

        game.players.values.forEach { player ->
            assertEquals(5 + 2, player.hand.count(HORDE))
            assertEquals(4, resources(player.hand))
        }
    }

    @Test
    fun `the same seed draws the same resources`() {
        val hands = { startedGame(GameConfig(resourcesPerTurn = 10)).players.mapValues { it.value.hand } }

        assertEquals(hands(), hands())
    }

    @Test
    fun `each resource is drawn about a third of the time`() {
        val hands = startedGame(GameConfig(resourcesPerTurn = 1000)).players.values.map { it.hand }

        ResourceType.entries.forEach { resource ->
            val drawn = hands.sumOf { it.count(resource) }
            assertTrue(drawn in 900..1100, "$resource drawn $drawn of 3000 times")
        }
    }

    private fun resources(hand: Hand) = ResourceType.entries.sumOf { hand.count(it) }

    private fun startedGame(config: GameConfig) =
        Game.create(config, SEED, 3, "anna").join("ben").join("cleo")
}
