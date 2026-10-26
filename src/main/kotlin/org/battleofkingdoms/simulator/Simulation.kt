package org.battleofkingdoms.simulator

import org.battleofkingdoms.cards.Artifact
import org.battleofkingdoms.game.Game
import org.battleofkingdoms.game.GameConfig
import org.battleofkingdoms.game.Phase.BATTLE
import org.battleofkingdoms.game.Phase.BUILD_UP
import org.battleofkingdoms.game.Phase.CLAIM
import org.battleofkingdoms.game.Phase.GAME_OVER
import org.battleofkingdoms.server.PlayerView
import java.util.UUID
import kotlin.random.Random

/** A game still running after this many turns stops and counts as unfinished. */
const val MAX_TURNS = 100

/** A build-up that hasn't answered Ready after this many moves is a bot bug. */
const val MAX_BUILD_UP_MOVES = 50

/** One seat at a table: its player name in the game and the strategy playing it. */
data class Seat(val name: String, val strategyId: String)

/**
 * The outcome of one simulated game. [seats] are in join order; [winner] is a seat name,
 * `null` when the game hit [MAX_TURNS]; [artifacts] are what each seat held at the end.
 */
data class GameRecord(
    val gameSeed: Long,
    val seats: List<Seat>,
    val winner: String?,
    val turns: Int,
    val artifacts: Map<String, Set<Artifact>>,
) {
    val finished: Boolean get() = winner != null
    val playerCount: Int get() = seats.size
}

/** Every simulated table has this many players. */
const val PLAYERS = 4

/** Plays seeded games between random tables of [strategies]; see [Seeds] for where every seed comes from. */
class Simulation(private val strategies: Map<String, Strategy> = registry) {
    private val ids = strategies.keys.toList()
    private val planOf: Map<String, String?> = strategies.mapValues { (it.value as? PlannedStrategy)?.plan?.id }

    init {
        require(ids.size >= 2) { "A simulation needs at least two strategies, not ${ids.size}" }
        require(planOf.values.any { it == null } || planOf.values.toSet().size >= PLAYERS) {
            "Without a strategy outside the build plans, a table needs $PLAYERS different plans"
        }
    }

    fun run(runSeed: Long, games: Int): List<GameRecord> = Seeds.games(runSeed, games).map(::play)

    /** Replays exactly the game [gameSeed] stands for in any run. */
    fun play(gameSeed: Long): GameRecord {
        val seeds = Seeds.of(gameSeed)
        val lineup = table(seeds.table).map { it to strategies.getValue(it) }
        return playGame(lineup, gameSeed, seeds.engine, seeds.bots)
    }

    /**
     * A strategy id per seat, uniformly. A table is drawn again when every seat has the same
     * strategy, or when two seats share a build plan: two copies of a plan build alike and
     * can tie each other forever. Depends only on the seed, the ids and their plans.
     */
    fun table(tableSeed: Long): List<String> {
        val random = Random(tableSeed)
        return generateSequence { List(PLAYERS) { ids[random.nextInt(ids.size)] } }.first(::valid)
    }

    private fun valid(table: List<String>): Boolean {
        val plans = table.mapNotNull { planOf[it] }
        return table.toSet().size > 1 && plans.size == plans.toSet().size
    }
}

/** A strategy broke the rules: a bug in the bot, so the run stops. */
class BotError(message: String, cause: Throwable) : RuntimeException(message, cause)

private class Bot(val seat: Seat, val strategy: Strategy, val random: Random)

/**
 * Plays one game between [lineup] (strategy id to strategy, in seat order) on the default
 * [GameConfig]. Every strategy call gets a fresh [PlayerView] of its own seat, never the
 * [Game] itself, and its own `Random(botSeeds[seat])`.
 */
fun playGame(lineup: List<Pair<String, Strategy>>, gameSeed: Long, engineSeed: Long, botSeeds: List<Long>): GameRecord {
    val bots = lineup.mapIndexed { i, (id, strategy) -> Bot(Seat("$id-${i + 1}", id), strategy, Random(botSeeds[i])) }
    val gameId = UUID(gameSeed, 0)
    val host = bots.first().seat.name
    var game = bots.drop(1).fold(Game.create(GameConfig(), engineSeed, bots.size, host)) { g, bot -> g.join(bot.seat.name) }

    while (game.phase != GAME_OVER && game.turn <= MAX_TURNS) {
        game = when (game.phase) {
            BUILD_UP -> bots.fold(game) { g, bot -> buildUp(g, bot, gameId, gameSeed) }
            BATTLE -> bots.fold(game) { g, bot ->
                bot.act(g, gameSeed, "commit") { g.commitArmy(bot.seat.name, bot.strategy.commit(view(g, bot, gameId), bot.random)) }
            }
            CLAIM -> {
                val winner = bots.single { it.seat.name == game.battleLog.last().winner }
                winner.act(game, gameSeed, "claim") { game.claim(winner.seat.name, winner.strategy.claim(view(game, winner, gameId), winner.random)) }
            }
            else -> error("Unexpected phase ${game.phase}")
        }
    }

    return GameRecord(
        gameSeed = gameSeed,
        seats = bots.map { it.seat },
        winner = game.winner,
        turns = if (game.phase == GAME_OVER) game.turn else MAX_TURNS,
        artifacts = game.players.mapValues { it.value.artifacts },
    )
}

private fun buildUp(start: Game, bot: Bot, gameId: UUID, gameSeed: Long): Game {
    var game = start
    repeat(MAX_BUILD_UP_MOVES) {
        val move = bot.act(game, gameSeed, "build-up") { bot.strategy.buildUp(view(game, bot, gameId), bot.random) }
        game = bot.act(game, gameSeed, move.toString()) { apply(game, bot.seat.name, move) }
        if (move == BuildUpMove.Ready) return game
    }
    throw BotError(bot.describe(game, gameSeed, "no Ready after $MAX_BUILD_UP_MOVES build-up moves"), IllegalStateException())
}

private fun apply(game: Game, name: String, move: BuildUpMove): Game = when (move) {
    is BuildUpMove.Trade -> game.trade(name, move.resource)
    is BuildUpMove.Upgrade -> game.upgrade(name, move.into)
    is BuildUpMove.Transmute -> game.transmute(name, move.resource)
    BuildUpMove.Ready -> game.ready(name)
}

private fun view(game: Game, bot: Bot, gameId: UUID): PlayerView = PlayerView.of(gameId, game, bot.seat.name)

/** Runs [action]; any failure in the strategy or the move it chose is a [BotError] naming the bot. */
private fun <T> Bot.act(game: Game, gameSeed: Long, what: String, action: () -> T): T = try {
    action()
} catch (e: RuntimeException) {
    throw BotError(describe(game, gameSeed, "$what: ${e.message}"), e)
}

private fun Bot.describe(game: Game, gameSeed: Long, problem: String): String =
    "Strategy ${seat.strategyId} (seat ${seat.name}) failed in game $gameSeed, turn ${game.turn}: $problem"
