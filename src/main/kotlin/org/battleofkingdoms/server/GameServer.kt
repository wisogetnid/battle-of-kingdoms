package org.battleofkingdoms.server

import org.battleofkingdoms.battle.Army
import org.battleofkingdoms.game.GameSetup
import org.battleofkingdoms.game.GameState
import org.battleofkingdoms.game.Game
import org.battleofkingdoms.game.GameState.State
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

    fun createGame(gameSetup: GameSetup): UUID {
        val id = UUID.randomUUID()
        val game = Game(gameSetup, id)
        games[id] = game
        return id
    }

    fun createGame(): UUID {
        val game = Game()
        games[game.gameState.id] = game
        return game.gameState.id
    }

    fun commitArmy(gameId: UUID, playerName: String, army: Army) {
        val game = games[gameId]
        if (game?.state() == State.BATTLE) {
            games[gameId] = game.commitArmy(playerName, army)
        }
    }
}
