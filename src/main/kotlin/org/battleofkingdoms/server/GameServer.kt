package org.battleofkingdoms.server

import org.battleofkingdoms.battle.Army
import org.battleofkingdoms.game.Board
import org.battleofkingdoms.game.Game
import org.battleofkingdoms.game.Board.State
import org.battleofkingdoms.player.Player
import java.util.*

class GameServer {
    private val games = mutableMapOf<UUID, Game>()

    fun getGame(gameId: UUID): Game? {
        return games[gameId]
    }

    fun finishBuildUp(gameId: UUID, playerName: String) {
        val game = games[gameId]
        if (game?.state() == State.IN_PLAY) {
            games[gameId] = game.finishBuildUp(playerName)
        }
    }

    fun commitArmy(gameId: UUID, playerName: String, army: Army) {
        val game = games[gameId]
        if (game?.state() == State.BATTLE) {
            games[gameId] = game.commitArmy(playerName, army)
        }
    }

    fun startGame(vararg players: Player): UUID {
        val playernameToPlayer = players.map { it.name to it }.toMap()
        val board = Board(resourceDeck = Board.withTestResources().resourceDeck, playernameToPlayer = playernameToPlayer)
        val game = Game(board = board)
        games[game.board.id] = game.newTurn()
        return game.board.id
    }
}