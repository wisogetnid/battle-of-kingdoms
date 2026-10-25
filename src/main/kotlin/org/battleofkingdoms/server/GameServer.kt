package org.battleofkingdoms.server

import org.battleofkingdoms.game.Game
import org.battleofkingdoms.game.GameConfig
import org.battleofkingdoms.game.UnknownGame
import java.util.UUID
import kotlin.random.Random

/** In-memory registry of running games; restarting the server loses them. */
class GameServer(private val config: GameConfig = GameConfig()) {
    private val games = mutableMapOf<UUID, Game>()

    @Synchronized
    fun getGame(gameId: UUID): Game = games[gameId] ?: throw UnknownGame(gameId.toString())

    @Synchronized
    fun createGame(playerCount: Int, host: String, seed: Long = Random.nextLong()): UUID {
        val gameId = UUID.randomUUID()
        games[gameId] = Game.create(config, seed, playerCount, host)
        return gameId
    }

    fun joinGame(gameId: UUID, name: String): Game = update(gameId) { it.join(name) }

    @Synchronized
    private fun update(gameId: UUID, action: (Game) -> Game): Game =
        action(getGame(gameId)).also { games[gameId] = it }
}
