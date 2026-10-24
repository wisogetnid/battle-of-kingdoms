package org.battleofkingdoms.game

import org.battleofkingdoms.cards.CreatureType.HORDE
import org.battleofkingdoms.cards.Hand
import org.battleofkingdoms.cards.ResourceType.IRON
import org.battleofkingdoms.cards.ResourceType.WOOD
import org.battleofkingdoms.game.Phase.BUILD_UP
import org.battleofkingdoms.game.Phase.WAITING_FOR_PLAYERS
import org.battleofkingdoms.player.Player
import kotlin.random.Random

enum class Phase { WAITING_FOR_PLAYERS, BUILD_UP, BATTLE, GAME_OVER }

/**
 * Immutable game state; every action returns a new [Game] or throws a [GameException].
 * The only source of randomness is [random], seeded at creation, so the same config, seed,
 * and actions always produce the same game.
 */
data class Game(
    val config: GameConfig,
    val playerCount: Int,
    val random: Random,
    val phase: Phase = WAITING_FOR_PLAYERS,
    val turn: Int = 0,
    val pot: Int = 0,
    val players: Map<String, Player> = emptyMap(),
) {
    fun join(name: String): Game = when {
        name.isBlank() -> throw RuleViolation("Player name must not be blank")
        phase != WAITING_FOR_PLAYERS -> throw WrongPhase(phase)
        name in players -> throw DuplicatePlayer(name)
        else -> copy(players = players + (name to Player(name))).startWhenFull()
    }

    private fun startWhenFull(): Game = when (players.size) {
        playerCount -> startTurn()
        else -> this
    }

    private fun startTurn(): Game {
        val nextTurn = turn + 1
        return copy(
            phase = BUILD_UP,
            turn = nextTurn,
            pot = pot + 1,
            players = players.mapValues { (_, player) -> payIncome(player, nextTurn) },
        )
    }

    private fun payIncome(player: Player, turn: Int): Player {
        val horde = config.baseCreaturesPerTurn + player.renown + startingHorde(turn)
        val hand = (1..config.resourcesPerTurn)
            .fold(player.hand.add(HORDE, horde)) { hand, _ -> hand + drawResource() }
        return player.copy(hand = hand, acted = false)
    }

    private fun startingHorde(turn: Int): Int = when (turn) {
        1 -> config.startingHorde
        else -> 0
    }

    private fun drawResource() = when (random.nextBoolean()) {
        true -> WOOD
        false -> IRON
    }

    companion object {
        fun create(config: GameConfig, seed: Long, playerCount: Int, host: String): Game = when (playerCount) {
            in config.minPlayers..config.maxPlayers -> Game(config, playerCount, Random(seed)).join(host)
            else -> throw RuleViolation("A game needs ${config.minPlayers}–${config.maxPlayers} players, not $playerCount")
        }
    }
}
