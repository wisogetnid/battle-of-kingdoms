package org.battleofkingdoms.e2e

import org.battleofkingdoms.battle.Army
import org.battleofkingdoms.cards.creatures.Horde
import org.battleofkingdoms.cards.resources.Food
import org.battleofkingdoms.cards.resources.Iron
import org.battleofkingdoms.cards.resources.Wood
import org.battleofkingdoms.game.GameSetup
import org.battleofkingdoms.game.GameState
import org.battleofkingdoms.game.Game
import org.battleofkingdoms.player.Player
import org.battleofkingdoms.server.GameServer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val PLAYER_1_NAME = "Player 1"
private const val PLAYER_2_NAME = "Player 2"

class EndToEnd2PlayerUnitTest {

    @Test
    fun `a deterministic 2 player game runs until the end`() {
        val gameServer = GameServer()

        val player1Hand = listOf(Horde(), Horde(), Wood(), Iron(), Food())
        val player2Hand = listOf(Horde(), Horde(), Wood(), Iron(), Iron())
        val resourceDeck = listOf(Wood(), Iron(), Food(), Wood(), Iron(), Food())

        val gameSetup = GameSetup(
            playerHands = mapOf(
                PLAYER_1_NAME to player1Hand,
                PLAYER_2_NAME to player2Hand
            ),
            creatureDeck = emptyList(),
            resourceDeck = resourceDeck
        )

        val gameId = gameServer.createGame(gameSetup)
        var game = gameServer.getGame(gameId)!!

        assertEquals(player1Hand, game.players()[PLAYER_1_NAME]!!.hand)
        assertEquals(player2Hand, game.players()[PLAYER_2_NAME]!!.hand)

        gameServer.finishBuildUp(gameId, PLAYER_1_NAME)
        gameServer.finishBuildUp(gameId, PLAYER_2_NAME)

        game = gameServer.getGame(gameId)!!
        assertEquals(GameState.State.BATTLE, game.state())

        val player1 = game.players()[PLAYER_1_NAME]!!
        val player1Hordes = player1.hand.filterIsInstance<Horde>().take(2)
        val player1Army = Army(player1Hordes)
        gameServer.commitArmy(gameId, PLAYER_1_NAME, player1Army)

        game = gameServer.getGame(gameId)!!
        assertEquals(Player.State.WAITING, game.players()[PLAYER_1_NAME]!!.state)
        assertEquals(player1Hand.size - 2, game.players()[PLAYER_1_NAME]!!.hand.size)

        val player2 = game.players()[PLAYER_2_NAME]!!
        val player2Hordes = player2.hand.filterIsInstance<Horde>().take(1)
        val player2Army = Army(player2Hordes)
        gameServer.commitArmy(gameId, PLAYER_2_NAME, player2Army)

        game = gameServer.getGame(gameId)!!
        assertEquals(GameState.State.IN_PLAY, game.state())

        // Player 1: 5 (initial) - 2 (committed) + 1 (survivor) + 4 (new turn draw) = 8
        assertEquals(8, game.players()[PLAYER_1_NAME]!!.hand.size)
        // Player 2: 5 (initial) - 1 (committed) + 0 (survivors) + 2 (new turn draw from depleted deck) = 6
        assertEquals(6, game.players()[PLAYER_2_NAME]!!.hand.size)
    }

    private fun validateGameInPlay(
        game: Game
    ) {
        assertEquals(52, game.gameState.resourceDeck.size)
        game.players().forEach {
            assertEquals(4, it.value.hand.size)
        }
    }

    private fun validateGameInState(game: Game, state: GameState.State) {
        assertEquals(state, game.state())
    }

    private fun validateFirstCommittedArmyBattle(game: Game) {
        assertEquals(GameState.State.BATTLE, game.state())
        assertEquals(1, game.players().filter { Player.State.WAITING == it.value.state }.count())
        game.players()
            .filter { Player.State.WAITING == it.value.state }
            .forEach { assertEquals(2, it.value.hand.size) }

        game.players()
            .filter { Player.State.ACTIVE == it.value.state }
            .forEach { assertEquals(4, it.value.hand.size) }
    }

    private fun validateBattleResult(game: Game) {
        assertEquals(GameState.State.IN_PLAY, game.state())
        assertTrue(game.players().values.all { Player.State.ACTIVE == it.state })
        // These assertions are not valid anymore as we don't have SOME_PLAYER_NAME
        // assertEquals(2 + 1 + 4, game.players()[SOME_PLAYER_NAME]!!.hand.size)
        // assertEquals(3 + 0 + 4, game.players()[ANOTHER_PLAYER_NAME]!!.hand.size)
    }

    private fun validatePlayersWaiting(vararg players: Player) {
        players.forEach { assertEquals(Player.State.WAITING, it.state) }
    }

    private fun validatePlayersActive(vararg players: Player) {
        players.forEach { assertEquals(Player.State.ACTIVE, it.state) }
    }
}
