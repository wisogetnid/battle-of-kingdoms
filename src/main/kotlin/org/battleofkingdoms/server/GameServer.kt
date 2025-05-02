package org.battleofkingdoms.server

import org.battleofkingdoms.battle.Army
import org.battleofkingdoms.game.Game
import org.battleofkingdoms.game.Game.State
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
        val game = Game(state = State.IN_PLAY)
        players.forEach { player ->
            game.playernameToPlayer = game.playernameToPlayer.plus(player.name to player)
        }
        games[game.id] = game.newTurn()
        return game.id
    }
}