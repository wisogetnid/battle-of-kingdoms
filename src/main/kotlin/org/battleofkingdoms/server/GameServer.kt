package org.battleofkingdoms.server

import org.battleofkingdoms.battle.Army
import org.battleofkingdoms.cards.CreatureType
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

    fun upgrade(gameId: UUID, name: String, into: CreatureType): Game = update(gameId) { it.upgrade(name, into) }

    fun ready(gameId: UUID, name: String): Game = update(gameId) { it.ready(name) }

    fun commitArmy(gameId: UUID, name: String, army: Army): Game = update(gameId) { it.commitArmy(name, army) }

    @Synchronized
    private fun update(gameId: UUID, action: (Game) -> Game): Game =
        action(getGame(gameId)).also { games[gameId] = it }
}
