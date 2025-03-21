package org.battleofkingdoms.e2e

import org.battleofkingdoms.battle.Army
import org.battleofkingdoms.cards.creatures.Horde
import org.battleofkingdoms.game.Game
import org.battleofkingdoms.player.Player
import org.battleofkingdoms.server.GameServer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val SOME_PLAYER_NAME = "some player"
private const val ANOTHER_PLAYER_NAME = "another player"

class EndToEnd2PlayerUnitTest {
    @Test
    fun testGameSetup_shouldCreateAndPlayTwoPlayerGame() {
        val gameServer = GameServer()
        val somePlayer = Player(name = SOME_PLAYER_NAME)
        val anotherPlayer = Player(ANOTHER_PLAYER_NAME)
        validatePlayersWaiting(somePlayer, anotherPlayer)

        val gameId = gameServer.hostGame(numberOfPlayers = 2, somePlayer)

        val gameWaitingForPlayers = gameServer.getGame(gameId).let { it as Game }
        validateGameWaitingForPlayers(gameWaitingForPlayers)

        gameServer.joinGame(gameId, anotherPlayer)

        val gameInPlay = gameServer.getGame(gameId).let { it as Game }
        validatePlayersActive(*gameInPlay.players().values.toTypedArray())
        validateGameInPlay(gameInPlay)

        gameServer.finishBuildUp(gameId, somePlayer.name)

        val gameStillInPlay = gameServer.getGame(gameId).let { it as Game }
        validatePlayersWaiting(gameStillInPlay.players()[SOME_PLAYER_NAME]!!)
        gameServer.finishBuildUp(gameId, anotherPlayer.name)

        val battleStart = gameServer.getGame(gameId).let { it as Game }
        validatePlayersActive(*battleStart.players().values.toTypedArray())
        validateGameInState(battleStart, Game.State.BATTLE)

        gameServer.commitArmy(gameId, somePlayer.name, Army(listOf(Horde(), Horde())))
        val committedArmyBattle = gameServer.getGame(gameId).let { it as Game }
        validateFirstCommittedArmyBattle(committedArmyBattle)

        gameServer.commitArmy(gameId, anotherPlayer.name, Army(listOf(Horde())))
        val battleResult = gameServer.getGame(gameId).let { it as Game }
        validateBattleResult(battleResult)
    }

    private fun validateGameWaitingForPlayers(
        game: Game
    ) {
        assertEquals(Game.State.WAIT_FOR_PLAYERS_TO_JOIN, game.state())
        assertEquals(SOME_PLAYER_NAME, game.players()[SOME_PLAYER_NAME]!!.name)
    }

    private fun validateGameInPlay(
        game: Game
    ) {
        assertEquals(Game.State.IN_PLAY, game.state())
        assertEquals(52, game.board.resourceDeck.size)
        game.players().forEach {
            assertEquals(4, it.value.hand.size)
        }
    }

    private fun validateGameInState(game: Game, state: Game.State) {
        assertEquals(state, game.state())
    }

    private fun validateFirstCommittedArmyBattle(game: Game) {
        assertEquals(Game.State.BATTLE, game.state())
        assertEquals(1, game.players().filter { Player.State.WAITING == it.value.state }.count())
        game.players()
            .filter { Player.State.WAITING == it.value.state }
            .forEach { assertEquals(2, it.value.hand.size) }

        game.players()
            .filter { Player.State.ACTIVE == it.value.state }
            .forEach{ assertEquals(4, it.value.hand.size)}
    }

    private fun validateBattleResult(game: Game) {
        assertEquals(Game.State.IN_PLAY, game.state())
        assertTrue(game.players().values.all { Player.State.ACTIVE == it.state })
        assertEquals(2 + 1 + 4, game.players()[SOME_PLAYER_NAME]!!.hand.size)
        assertEquals(3 + 0 + 4, game.players()[ANOTHER_PLAYER_NAME]!!.hand.size)
    }

    private fun validatePlayersWaiting(vararg players: Player) {
        players.forEach { assertEquals(Player.State.WAITING, it.state) }
    }

    private fun validatePlayersActive(vararg players: Player) {
        players.forEach { assertEquals(Player.State.ACTIVE, it.state) }
    }
}
