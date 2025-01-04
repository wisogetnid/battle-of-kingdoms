package org.battleofkingdoms.e2e

import org.battleofkingdoms.game.Game
import org.battleofkingdoms.game.phases.GameInPlay
import org.battleofkingdoms.game.player.Player
import org.battleofkingdoms.game.phases.GameWaitingForPlayers
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

        if (gameInPlay is GameInPlay) {
            val gameStillInPlay = somePlayer.finishBuildUp(gameInPlay)
            validateGameStillInPlay(gameStillInPlay)
        }
    }

    private fun validateGameWaitingForPlayers(
        gameId: UUID,
        game: GameWaitingForPlayers
    ) {
        assertNotNull(gameId)
        assertEquals(Game.State.WAIT_FOR_PLAYERS_TO_JOIN, game.state())
        assertEquals(SOME_PLAYER_NAME, game.players().first().name)
    }
    private fun validateGameInPlay(
        gameId: UUID,
        game: Game,
        somePlayer: Player,
        anotherPlayer: Player
    ) {
        assertEquals(gameId, game.id)
        assertEquals(Game.State.IN_PLAY, game.state())
        assertEquals(setOf(somePlayer, anotherPlayer), game.players())
        assertEquals(52, game.resourceDeck.count())
        assertEquals(4, somePlayer.hand().count())
        assertEquals(4, anotherPlayer.hand().count())
    }

    private fun validateGameStillInPlay(game: Game) {
        assertEquals(Game.State.IN_PLAY, game.state())
    }
}