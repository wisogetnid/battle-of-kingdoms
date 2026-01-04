package org.battleofkingdoms.e2e

import org.battleofkingdoms.battle.Army
import org.battleofkingdoms.cards.Horde
import org.battleofkingdoms.cards.Food
import org.battleofkingdoms.cards.Iron
import org.battleofkingdoms.cards.Wood
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

        val player1Hand = listOf(Horde(), Horde(), Wood, Iron, Food)
        val player2Hand = listOf(Horde(), Horde(), Wood, Iron, Iron)
        val resourceDeck = listOf(Wood, Iron, Food, Wood, Iron, Food)

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
}
