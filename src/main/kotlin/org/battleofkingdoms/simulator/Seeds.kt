package org.battleofkingdoms.simulator

import org.battleofkingdoms.game.GameConfig
import kotlin.random.Random

/**
 * Every random choice of a run comes from one run seed, split into independent streams:
 * `Random(runSeed)` yields the game seeds; `Random(gameSeed)` yields that game's table seed,
 * engine seed and one bot seed per seat. No two streams share a seed.
 */
object Seeds {
    fun games(runSeed: Long, count: Int): List<Long> {
        val random = Random(runSeed)
        return List(count) { random.nextLong() }
    }

    /** Bot seeds are drawn for the largest table, so the player count never shifts the others. */
    fun of(gameSeed: Long): GameSeeds {
        val random = Random(gameSeed)
        return GameSeeds(
            table = random.nextLong(),
            engine = random.nextLong(),
            bots = List(GameConfig().maxPlayers) { random.nextLong() },
        )
    }
}

data class GameSeeds(val table: Long, val engine: Long, val bots: List<Long>)
