package org.battleofkingdoms.e2e

import org.battleofkingdoms.cards.creatures.Horde
import org.battleofkingdoms.game.Game
import org.battleofkingdoms.game.phases.GameBattle
import org.battleofkingdoms.player.Player
import org.battleofkingdoms.server.GameServer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val SOME_PLAYER_NAME = "some player"
private const val ANOTHER_PLAYER = "another player"

class EndToEnd2PlayerUnitTest {
    @Test
    fun testGameSetup_shouldCreateTwoPlayerGame() {
        val gameServer = GameServer()
        val somePlayer = Player(name = SOME_PLAYER_NAME)
        val anotherPlayer = Player(ANOTHER_PLAYER)
        validatePlayersWaiting(somePlayer, anotherPlayer)

        val gameId = gameServer.hostGame(numberOfPlayers = 2, somePlayer)

        val gameWaitingForPlayers = gameServer.getGame(gameId).let { it as Game }
        validateGameWaitingForPlayers(gameWaitingForPlayers)

        gameServer.joinGame(gameId, anotherPlayer)

        val gameInPlay = gameServer.getGame(gameId).let { it as Game }
        validatePlayersActive(*gameInPlay.players().toTypedArray())
        validateGameInPlay(gameInPlay)

        gameServer.finishBuildUp(gameId, somePlayer.name)

        val gameStillInPlay = gameServer.getGame(gameId).let { it as Game }
        validateSomePlayersWaiting(gameStillInPlay.players())
        gameServer.finishBuildUp(gameId, anotherPlayer.name)

        val battleStart = gameServer.getGame(gameId).let { it as GameBattle }
        validatePlayersActive(*battleStart.players().toTypedArray())
        validateGameInState(battleStart, Game.State.BATTLE)

        gameServer.commitArmy(gameId, somePlayer.name, Horde(), Horde())
        val committedArmyBattle = gameServer.getGame(gameId).let { it as GameBattle }
        validateSomePlayersWaiting(gameStillInPlay.players())
        validateFirstCommittedArmyBattle(committedArmyBattle)
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
        assertEquals(52, game.board.resourceDeck.size)
        game.players().forEach {
            assertEquals(4, it.hand.size)
        }
    }

    private fun validateGameInState(game: GameBattle, state: Game.State) {
        assertEquals(state, game.state())
    }

    private fun validateFirstCommittedArmyBattle(game: GameBattle) {
        assertEquals(Game.State.BATTLE, game.state())
        assertEquals(1, game.players.filter { Player.State.WAITING == it.state }.count())
        game.players
            .filter { Player.State.WAITING == it.state }
            .forEach { assertEquals(2, it.hand.size) }
    }

    private fun validatePlayersWaiting(vararg players: Player) {
        players.forEach { assertEquals(Player.State.WAITING, it.state) }
    }

    private fun validatePlayersActive(vararg players: Player) {
        players.forEach { assertEquals(Player.State.ACTIVE, it.state) }
    }

    private fun validateSomePlayersWaiting(players: List<Player>) {
        assertTrue(
            players
                .filter { Player.State.WAITING == it.state }
                .count() >= 1)
    }
}
