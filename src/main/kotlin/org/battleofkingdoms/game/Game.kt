package org.battleofkingdoms.game

import org.battleofkingdoms.battle.Army
import org.battleofkingdoms.battle.Battle
import org.battleofkingdoms.battle.BattleResult
import org.battleofkingdoms.battle.Prize
import org.battleofkingdoms.cards.Artifact
import org.battleofkingdoms.cards.Artifact.FAMOUS_CROWN
import org.battleofkingdoms.cards.CreatureType
import org.battleofkingdoms.cards.CreatureType.HORDE
import org.battleofkingdoms.cards.Hand
import org.battleofkingdoms.cards.ResourceType
import org.battleofkingdoms.game.Phase.BATTLE
import org.battleofkingdoms.game.Phase.BUILD_UP
import org.battleofkingdoms.game.Phase.CLAIM
import org.battleofkingdoms.game.Phase.GAME_OVER
import org.battleofkingdoms.game.Phase.WAITING_FOR_PLAYERS
import org.battleofkingdoms.player.Player
import kotlin.random.Random

enum class Phase { WAITING_FOR_PLAYERS, BUILD_UP, BATTLE, CLAIM, GAME_OVER }

enum class Action { TRADE, UPGRADE, READY, COMMIT_ARMY, CLAIM }

/** What a battle's winner chooses to take: the drawn artifact or the VP. */
enum class Take { ARTIFACT, VICTORY_POINT }

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
    val artifactStack: Set<Artifact> = emptySet(),
    val drawnArtifact: Artifact? = null,
) {
    fun join(name: String): Game = when {
        name.isBlank() -> throw RuleViolation("Player name must not be blank")
        phase != WAITING_FOR_PLAYERS -> throw WrongPhase(phase)
        name in players -> throw DuplicatePlayer(name)
        else -> copy(players = players + (name to Player(name))).startWhenFull()
    }

    /** Swaps one resource of any kind for one Horde. */
    fun trade(playerName: String, resource: ResourceType): Game {
        val player = player(playerName)
        return when {
            phase != BUILD_UP -> throw WrongPhase(phase)
            player.acted -> throw AlreadyActed(playerName)
            !player.hand.has(resource) -> throw RuleViolation("$playerName has no $resource to trade")
            else -> updatePlayer(player.copy(hand = player.hand - resource + HORDE))
        }
    }

    /** Upgrades one unit by one tier: a Horde into a tier-1 unit, or a settled tier-1 unit into its tier-2 unit. */
    fun upgrade(playerName: String, into: CreatureType): Game {
        val player = player(playerName)
        upgradeViolation(player, into)?.let { throw it }
        return updatePlayer(
            player.copy(
                hand = player.hand - into.upgradesFrom!! - into.upgradeResource!! + into,
                upgradedThisTurn = player.upgradedThisTurn + (into to (player.upgradedThisTurn[into] ?: 0) + 1),
            )
        )
    }

    /** What [playerName] could upgrade into right now: exactly the creatures [upgrade] would accept. */
    fun affordableUpgrades(playerName: String): List<CreatureType> {
        val player = player(playerName)
        return CreatureType.entries.filter { upgradeViolation(player, it) == null }
    }

    /** Why [player] can't upgrade into [into] right now, or `null` if they can. */
    private fun upgradeViolation(player: Player, into: CreatureType): GameException? {
        val from = into.upgradesFrom ?: return RuleViolation("Nothing can be upgraded into $into")
        val resource = into.upgradeResource!!
        return when {
            phase != BUILD_UP -> WrongPhase(phase)
            player.acted -> AlreadyActed(player.name)
            !player.hand.has(from) || !player.hand.has(resource) ->
                RuleViolation("${player.name} needs 1 $from and 1 $resource to upgrade into $into")
            player.settled(from) < 1 ->
                RuleViolation("${player.name}'s $from was upgraded this turn and can't become $into until next turn")
            else -> null
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

    /** The battle's winner takes the drawn artifact or the VP; a declined artifact stays in the stack. */
    fun claim(playerName: String, take: Take): Game {
        player(playerName)
        return when {
            phase != CLAIM -> throw WrongPhase(phase)
            playerName != battleLog.last().winner -> throw NotTheWinner(playerName)
            else -> award(
                when (take) {
                    Take.ARTIFACT -> Prize.Taken(drawnArtifact!!)
                    Take.VICTORY_POINT -> Prize.VictoryPoint
                }
            )
        }
    }

    /** The drawn artifact as [playerName] may see it: only the winner, only while they choose. */
    fun drawnArtifactFor(playerName: String): Artifact? = when {
        phase == CLAIM && playerName == battleLog.last().winner -> drawnArtifact
        else -> null
    }

    fun player(name: String): Player = players[name] ?: throw UnknownPlayer(name)

    /** What [playerName] may do right now; [Action.TRADE] and [Action.UPGRADE] are listed by phase, not by what they can afford. */
    fun availableActions(playerName: String): List<Action> {
        val player = player(playerName)
        return when {
            player.acted -> emptyList()
            phase == BUILD_UP -> listOf(Action.TRADE, Action.UPGRADE, Action.READY)
            phase == BATTLE -> listOf(Action.COMMIT_ARMY)
            phase == CLAIM && playerName == battleLog.last().winner -> listOf(Action.CLAIM)
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
        val armies = players.keys.associateWith { commits.getValue(it) }
        val owned = players.mapValues { it.value.artifacts }
        val result = Battle.resolve(armies, config.bonuses, random, owned, config.frenzyBonus).worth(pot)
        val resolved = copy(
            players = players.mapValues { (name, player) -> afterBattle(player, commits.getValue(name), result) },
            commits = emptyMap(),
            battleLog = battleLog + result,
        )
        return when {
            result.winner == null -> resolved.startTurn()
            artifactStack.isEmpty() -> resolved.award(Prize.VictoryPoint)
            else -> resolved.copy(phase = CLAIM, drawnArtifact = resolved.drawArtifact())
        }
    }

    /** Uniformly from the stack, in [Artifact] order so the draw only depends on the seed. */
    private fun drawArtifact(): Artifact {
        val stack = Artifact.entries.filter { it in artifactStack }
        return stack[random.nextInt(stack.size)]
    }

    /** Gives the last battle's winner their [prize]; the game ends once they reach the VP target. */
    private fun award(prize: Prize): Game {
        val entry = battleLog.last().awarded(prize)
        val champion = player(entry.winner!!)
        val rewarded = when (prize) {
            Prize.VictoryPoint -> champion.copy(vp = champion.vp + pot)
            is Prize.Taken -> champion.copy(artifacts = champion.artifacts + prize.artifact)
        }
        val awarded = updatePlayer(rewarded).copy(
            battleLog = battleLog.dropLast(1) + entry,
            artifactStack = if (prize is Prize.Taken) artifactStack - prize.artifact else artifactStack,
            drawnArtifact = null,
        )
        return when {
            rewarded.vp >= config.victoryPointsToWin -> awarded.copy(phase = GAME_OVER, winner = rewarded.name)
            else -> awarded.startTurn()
        }
    }

    private fun afterBattle(player: Player, committed: Army, result: BattleResult): Player {
        val armyResult = result.armies.getValue(player.name)
        val withoutArmy = committed.creatures.entries.fold(player.hand) { hand, (type, n) -> hand.remove(type, n) }
        val withSurvivors = armyResult.survivors.creatures.entries.fold(withoutArmy) { hand, (type, n) -> hand.add(type, n) }
        return player.copy(hand = withSurvivors, acted = false)
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
            pot = config.victoryPointsPerBattle,
            players = players.mapValues { (_, player) -> payIncome(player, nextTurn) },
        )
    }

    private fun payIncome(player: Player, turn: Int): Player {
        val horde = config.baseCreaturesPerTurn + startingHorde(turn) + crownHorde(player)
        val hand = (1..config.resourcesPerTurn)
            .fold(player.hand.add(HORDE, horde)) { hand, _ -> hand + drawResource() }
        return player.copy(hand = hand, acted = false, upgradedThisTurn = emptyMap())
    }

    private fun startingHorde(turn: Int): Int = when (turn) {
        1 -> config.startingHorde
        else -> 0
    }

    private fun crownHorde(player: Player): Int = when (FAMOUS_CROWN) {
        in player.artifacts -> config.crownHorde
        else -> 0
    }

    /** Each resource is equally likely. */
    private fun drawResource(): ResourceType = ResourceType.entries[random.nextInt(ResourceType.entries.size)]

    companion object {
        fun create(config: GameConfig, seed: Long, playerCount: Int, host: String): Game = when (playerCount) {
            in config.minPlayers..config.maxPlayers ->
                Game(config, playerCount, Random(seed), artifactStack = config.artifacts.toSet()).join(host)
            else -> throw RuleViolation("A game needs ${config.minPlayers}–${config.maxPlayers} players, not $playerCount")
        }
    }
}
