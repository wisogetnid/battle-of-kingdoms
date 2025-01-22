package org.battleofkingdoms.game.server

import org.battleofkingdoms.game.Game
import org.battleofkingdoms.game.phases.GameWaitingForPlayers
import org.battleofkingdoms.game.player.Player
import java.util.*

class GameServer {
    private val games = mutableMapOf<UUID, Game>()

    fun getGame(gameId: UUID): Game? {
        return games.get(gameId)
    }

    // TODO updates games map
    fun hostGame(numberOfPlayers: Int, player: Player): UUID {
        val game = GameWaitingForPlayers(numberOfPlayers, player)
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
}