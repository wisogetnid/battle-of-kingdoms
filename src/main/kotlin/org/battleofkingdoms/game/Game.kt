package org.battleofkingdoms.game

import org.battleofkingdoms.battle.Army
import org.battleofkingdoms.battle.Battle
import org.battleofkingdoms.battle.BattleResult
import org.battleofkingdoms.cards.CreatureType
import org.battleofkingdoms.cards.CreatureType.HORDE
import org.battleofkingdoms.cards.Hand
import org.battleofkingdoms.cards.ResourceType.IRON
import org.battleofkingdoms.cards.ResourceType.WOOD
import org.battleofkingdoms.game.Phase.BATTLE
import org.battleofkingdoms.game.Phase.BUILD_UP
import org.battleofkingdoms.game.Phase.GAME_OVER
import org.battleofkingdoms.game.Phase.WAITING_FOR_PLAYERS
import org.battleofkingdoms.player.Player
import kotlin.random.Random

enum class Phase { WAITING_FOR_PLAYERS, BUILD_UP, BATTLE, GAME_OVER }

enum class Action { UPGRADE, READY, COMMIT_ARMY }

/**
 * Immutable game state; every action returns a new [Game] or throws a [GameException].
 * The only source of randomness is [random], seeded at creation, so the same config, seed,
 * and actions always produce the same game.
 *
 * [commits] are hidden information: they stay out of every player view until the battle
 * resolves into a [battleLog] entry. Committed creatures stay in the hand until then, so
 * hand sizes don't reveal them either.
 */
data class Game(
    val config: GameConfig,
    val playerCount: Int,
    val random: Random,
    val phase: Phase = WAITING_FOR_PLAYERS,
    val turn: Int = 0,
    val pot: Int = 0,
    val players: Map<String, Player> = emptyMap(),
    val commits: Map<String, Army> = emptyMap(),
    val battleLog: List<BattleResult> = emptyList(),
    val winner: String? = null,
) {
    fun join(name: String): Game = when {
        name.isBlank() -> throw RuleViolation("Player name must not be blank")
        phase != WAITING_FOR_PLAYERS -> throw WrongPhase(phase)
        name in players -> throw DuplicatePlayer(name)
        else -> copy(players = players + (name to Player(name))).startWhenFull()
    }

    fun upgrade(playerName: String, into: CreatureType): Game {
        val player = player(playerName)
        val resource = into.upgradeResource ?: throw RuleViolation("Nothing can be upgraded into $into")
        return when {
            phase != BUILD_UP -> throw WrongPhase(phase)
            player.acted -> throw AlreadyActed(playerName)
            !player.hand.has(HORDE) || !player.hand.has(resource) ->
                throw RuleViolation("$playerName needs 1 Horde and 1 $resource to upgrade into $into")
            else -> updatePlayer(player.copy(hand = player.hand - HORDE - resource + into))
        }
    }

    fun ready(playerName: String): Game {
        val player = player(playerName)
        return when {
            phase != BUILD_UP -> throw WrongPhase(phase)
            player.acted -> throw AlreadyActed(playerName)
            else -> updatePlayer(player.copy(acted = true)).startBattleWhenAllReady()
        }
    }

    fun commitArmy(playerName: String, army: Army): Game {
        val player = player(playerName)
        return when {
            phase != BATTLE -> throw WrongPhase(phase)
            player.acted -> throw AlreadyActed(playerName)
            !owns(player, army) -> throw RuleViolation("$playerName doesn't have $army")
            else -> updatePlayer(player.copy(acted = true))
                .copy(commits = commits + (playerName to army))
                .resolveWhenAllCommitted()
        }
    }

    fun player(name: String): Player = players[name] ?: throw UnknownPlayer(name)

    /** What [playerName] may do right now; [Action.UPGRADE] is listed by phase, not by what they can afford. */
    fun availableActions(playerName: String): List<Action> {
        val player = player(playerName)
        return when {
            player.acted -> emptyList()
            phase == BUILD_UP -> listOf(Action.UPGRADE, Action.READY)
            phase == BATTLE -> listOf(Action.COMMIT_ARMY)
            else -> emptyList()
        }
    }

    private fun owns(player: Player, army: Army): Boolean =
        army.creatures.all { (type, count) -> player.hand.has(type, count) }

    private fun resolveWhenAllCommitted(): Game = when {
        players.values.all { it.acted } -> resolveBattle()
        else -> this
    }

    /** Commits are resolved in join order, so the casualty draws don't depend on who committed first. */
    private fun resolveBattle(): Game {
        val result = Battle.resolve(players.keys.associateWith { commits.getValue(it) }, pot, random)
        val resolved = copy(
            pot = if (result.rolledOver) pot else 0,
            players = players.mapValues { (name, player) -> afterBattle(player, commits.getValue(name), result) },
            commits = emptyMap(),
            battleLog = battleLog + result,
        )
        return when (val champion = resolved.players.values.find { it.vp >= config.victoryPointsToWin }) {
            null -> resolved.startTurn()
            else -> resolved.copy(phase = GAME_OVER, winner = champion.name)
        }
    }

    private fun afterBattle(player: Player, committed: Army, result: BattleResult): Player {
        val armyResult = result.armies.getValue(player.name)
        val withoutArmy = committed.creatures.entries.fold(player.hand) { hand, (type, n) -> hand.remove(type, n) }
        val withSurvivors = armyResult.survivors.creatures.entries.fold(withoutArmy) { hand, (type, n) -> hand.add(type, n) }
        return player.copy(
            hand = withSurvivors,
            vp = player.vp + armyResult.vpGained,
            renown = player.renown + if (armyResult.gainedRenown) config.renownPerBattle else 0,
            acted = false,
        )
    }

    private fun updatePlayer(player: Player): Game = copy(players = players + (player.name to player))

    private fun startBattleWhenAllReady(): Game = when {
        players.values.all { it.acted } -> copy(
            phase = BATTLE,
            players = players.mapValues { (_, player) -> player.copy(acted = false) },
        )
        else -> this
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
