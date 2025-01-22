package org.battleofkingdoms.e2e

import org.battleofkingdoms.game.Game
import org.battleofkingdoms.game.phases.GameInPlay
import org.battleofkingdoms.player.Player
import org.battleofkingdoms.server.GameServer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

private const val SOME_PLAYER_NAME = "player1"
private const val ANOTHER_PLAYER = "player2"

class EndToEndUnitTest {
    @Test
    fun testGameSetup_shouldCreateTwoPlayerGame() {
        val gameServer = GameServer()
        val somePlayer = Player(name = SOME_PLAYER_NAME)
        val gameId = gameServer.hostGame(numberOfPlayers = 2, somePlayer)
        val gameWaitingForPlayers = gameServer.getGame(gameId)
        gameWaitingForPlayers?.let { validateGameWaitingForPlayers(it) }

        val anotherPlayer = Player(ANOTHER_PLAYER)
        validatePlayersWaiting(somePlayer, anotherPlayer)

        gameServer.joinGame(gameId, anotherPlayer)
        validatePlayersActive(somePlayer, anotherPlayer)

        val gameInPlay = gameServer.getGame(gameId).let { it as GameInPlay }
        gameInPlay.let { validateGameInPlay(it, somePlayer, anotherPlayer) }

        gameServer.finishBuildUp(gameId, somePlayer)
        validatePlayersWaiting(somePlayer)
        validatePlayersActive(anotherPlayer)

        val gameStillInPlay = gameServer.getGame(gameId).let { it as GameInPlay }
        validateGameStillInPlay(gameStillInPlay)
    }

    private fun validateGameWaitingForPlayers(
        game: Game
    ) {
        assertEquals(Game.State.WAIT_FOR_PLAYERS_TO_JOIN, game.state())
        assertEquals(SOME_PLAYER_NAME, game.players().first().name)
    }

    private fun validateGameInPlay(
        game: Game,
        somePlayer: Player,
        anotherPlayer: Player
    ) {
        assertEquals(Game.State.IN_PLAY, game.state())
        assertEquals(setOf(somePlayer, anotherPlayer), game.players())
        assertEquals(52, game.resourceDeck.count())
        assertEquals(4, somePlayer.hand().count())
        assertEquals(4, anotherPlayer.hand().count())
    }

    private fun validateGameStillInPlay(game: Game) {
        assertEquals(Game.State.IN_PLAY, game.state())
    }

    private fun validatePlayersWaiting(vararg players: Player) {
        players.forEach { assertEquals(Player.State.WAITING, it.state()) }
    }

    private fun validatePlayersActive(vararg players: Player) {
        players.forEach { assertEquals(Player.State.ACTIVE, it.state()) }
    }
}
