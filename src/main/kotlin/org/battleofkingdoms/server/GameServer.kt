package org.battleofkingdoms.server

import org.battleofkingdoms.game.Game
import org.battleofkingdoms.game.phases.GameInPlay
import org.battleofkingdoms.game.phases.GameWaitingForPlayers
import org.battleofkingdoms.player.Player
import java.util.*

class GameServer {
    private val games = mutableMapOf<UUID, Game>()

    fun getGame(gameId: UUID): Game? {
        return games.get(gameId)
    }

    // TODO updates games map
    fun hostGame(numberOfPlayers: Int, player: Player): UUID {
        val game = GameWaitingForPlayers(numberOfPlayers).join(player)
        games[game.id] = game
        return game.id

    }

    // TODO updates games map
    fun joinGame(gameId: UUID, player: Player) {
        val game = games.get(gameId)
        if (game is GameWaitingForPlayers) {
            games[gameId] = game.join(player)
        }
    }

    fun finishBuildUp(gameId: UUID, playerName: String) {
        val game = games.get(gameId)
        if (game is GameInPlay) {
            games[gameId] = game.finishBuildUp(playerName)
        }
    }
}