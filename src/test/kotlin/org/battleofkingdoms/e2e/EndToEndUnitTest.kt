package org.battleofkingdoms.e2e

import org.battleofkingdoms.game.Game
import org.battleofkingdoms.game.phases.GameInPlay
import org.battleofkingdoms.game.phases.GameWaitingForPlayers
import org.battleofkingdoms.player.Player
import org.battleofkingdoms.server.GameServer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val SOME_PLAYER_NAME = "player1"
private const val ANOTHER_PLAYER = "player2"

class EndToEndUnitTest {
    @Test
    fun testGameSetup_shouldCreateTwoPlayerGame() {
        val gameServer = GameServer()
        val somePlayer = Player(name = SOME_PLAYER_NAME)
        val anotherPlayer = Player(ANOTHER_PLAYER)
        validatePlayersWaiting(somePlayer, anotherPlayer)

        val gameId = gameServer.hostGame(numberOfPlayers = 2, somePlayer)

        val gameWaitingForPlayers = gameServer.getGame(gameId).let { it as GameWaitingForPlayers }
        validateGameWaitingForPlayers(gameWaitingForPlayers)

        gameServer.joinGame(gameId, anotherPlayer)

        val gameInPlay = gameServer.getGame(gameId).let { it as GameInPlay }
        validatePlayersActive(*gameInPlay.players.toTypedArray())
        validateGameInPlay(gameInPlay)

        gameServer.finishBuildUp(gameId, somePlayer)

        val gameStillInPlay = gameServer.getGame(gameId).let { it as GameInPlay }
        validateSomePlayersWaiting(gameStillInPlay.players)
        validateGameStillInPlay(gameStillInPlay)
    }

    private fun validateGameWaitingForPlayers(
        game: Game
    ) {
        assertEquals(Game.State.WAIT_FOR_PLAYERS_TO_JOIN, game.state())
        assertEquals(SOME_PLAYER_NAME, game.players().first().name)
    }

    private fun validateGameInPlay(
        game: Game
    ) {
        assertEquals(Game.State.IN_PLAY, game.state())
        assertEquals(52, game.resourceDeck.count())
        game.players().forEach {
            assertEquals(4, it.hand.count())
        }
    }

    private fun validateGameStillInPlay(game: Game) {
        assertEquals(Game.State.IN_PLAY, game.state())
    }

    private fun validatePlayersWaiting(vararg players: Player) {
        players.forEach { assertEquals(Player.State.WAITING, it.state) }
    }

    private fun validatePlayersActive(vararg players: Player) {
        players.forEach { assertEquals(Player.State.ACTIVE, it.state) }
    }

    private fun validateSomePlayersWaiting(players: Set<Player>) {
        assertTrue(
            players
                .filter { Player.State.WAITING == it.state }
                .count() >= 1)
    }
}
