package org.battleofkingdoms.e2e

import org.battleofkingdoms.game.Game
import org.battleofkingdoms.game.player.Player
import org.battleofkingdoms.game.states.GameWaitingForPlayers
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private const val SOME_PLAYER_NAME = "player1"
private const val ANOTHER_PLAYER = "player2"

class EndToEndUnitTest {
    @Test
    fun testGameSetup_shouldCreateTwoPlayerGame() {
        val somePlayer = Player(name = SOME_PLAYER_NAME)
        val gameWaitingForPlayers = somePlayer.hostGame(numberOfPlayers = 2)
        validateGameWaitingForPlayers(gameWaitingForPlayers.id, gameWaitingForPlayers)

        val anotherPlayer = Player(ANOTHER_PLAYER)
        val gameInPlay = anotherPlayer.joinGame(gameWaitingForPlayers)
        validateGameInPlay(gameWaitingForPlayers.id, gameInPlay, somePlayer, anotherPlayer)
    }

    private fun validateGameWaitingForPlayers(
        gameId: UUID,
        gameWaitingForPlayers: GameWaitingForPlayers
    ) {
        assertNotNull(gameId)
        assertEquals(Game.State.WAITING_FOR_PLAYERS, gameWaitingForPlayers.state())
        assertEquals(SOME_PLAYER_NAME, gameWaitingForPlayers.players().first().name)
    }

    private fun validateGameInPlay(
        gameId: UUID,
        gameInPlay: Game,
        somePlayer: Player,
        anotherPlayer: Player
    ) {
        assertEquals(gameId, gameInPlay.id)
        assertEquals(Game.State.IN_PLAY, gameInPlay.state())
        assertEquals(setOf(somePlayer, anotherPlayer), gameInPlay.players())
    }
}