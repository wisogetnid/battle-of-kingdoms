package org.battleofkingdoms.server

import org.battleofkingdoms.battle.Army
import org.battleofkingdoms.cards.creatures.Creature
import org.battleofkingdoms.game.Game
import org.battleofkingdoms.game.Game.State
import org.battleofkingdoms.player.Player
import java.util.*

class GameServer {
    private val games = mutableMapOf<UUID, Game>()

    fun getGame(gameId: UUID): Game? {
        return games.get(gameId)
    }

    // TODO updates games map
    fun hostGame(numberOfPlayers: Int, player: Player): UUID {
        val game = Game(numberOfPlayers, state = State.WAIT_FOR_PLAYERS_TO_JOIN).join(player)
        games[game.id] = game
        return game.id

    }

    // TODO updates games map
    fun joinGame(gameId: UUID, player: Player) {
        val game = games.get(gameId)
        if (game?.state() == State.WAIT_FOR_PLAYERS_TO_JOIN) {
            games[gameId] = game.join(player)
        }
    }

    fun finishBuildUp(gameId: UUID, playerName: String) {
        val game = games.get(gameId)
        if (game?.state() == State.IN_PLAY) {
            games[gameId] = game.finishBuildUp(playerName)
        }
    }

    fun commitArmy(gameId: UUID, playerName: String, army: Army) {
        val game = games.get(gameId)
        if (game?.state() == State.BATTLE) {
            //TODO player.commitArmy
            games[gameId] = game.commitArmy(playerName, army)
        }
    }
}