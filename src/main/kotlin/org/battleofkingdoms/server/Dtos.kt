package org.battleofkingdoms.server

import org.battleofkingdoms.battle.Army
import org.battleofkingdoms.battle.ArmyResult
import org.battleofkingdoms.battle.BattleResult
import org.battleofkingdoms.battle.Duel
import org.battleofkingdoms.cards.CreatureType
import org.battleofkingdoms.cards.Hand
import org.battleofkingdoms.game.Action
import org.battleofkingdoms.game.Game
import org.battleofkingdoms.game.Phase
import org.battleofkingdoms.game.RuleViolation
import org.battleofkingdoms.player.Player
import java.util.UUID

data class CreateGameRequest(val players: Int, val host: String, val seed: Long? = null)

data class CreateGameResponse(val gameId: UUID)

data class JoinRequest(val name: String)

data class UpgradeRequest(val into: CreatureType)

/** `{ "HORDE": 2, "BOW": 1 }`; zero counts are ignored, `{}` commits nothing. */
fun armyOf(counts: Map<CreatureType, Int>): Army = when {
    counts.values.any { it < 0 } -> throw RuleViolation("Creature counts must not be negative: $counts")
    else -> Army(counts.filterValues { it > 0 })
}

data class ErrorResponse(val error: String)

/**
 * One player's view of a game. Built per player so other players' hands and the current
 * turn's commits are never part of it.
 */
data class PlayerView(
    val gameId: UUID,
    val phase: Phase,
    val turn: Int,
    val pot: Int,
    val victoryPointsToWin: Int,
    val me: MyView,
    val others: List<OtherPlayerView>,
    val availableActions: List<Action>,
    val battleLog: List<BattleLogEntryView>,
    val winner: String?,
) {
    companion object {
        fun of(gameId: UUID, game: Game, playerName: String): PlayerView = PlayerView(
            gameId = gameId,
            phase = game.phase,
            turn = game.turn,
            pot = game.pot,
            victoryPointsToWin = game.config.victoryPointsToWin,
            me = MyView.of(game.player(playerName)),
            others = game.players.values.filter { it.name != playerName }.map(OtherPlayerView::of),
            availableActions = game.availableActions(playerName),
            battleLog = game.battleLog.map(BattleLogEntryView::of),
            winner = game.winner,
        )
    }
}

data class MyView(val name: String, val hand: Map<String, Int>, val vp: Int, val renown: Int, val acted: Boolean) {
    companion object {
        fun of(player: Player) = MyView(player.name, counts(player.hand), player.vp, player.renown, player.acted)
    }
}

data class OtherPlayerView(val name: String, val vp: Int, val renown: Int, val handSize: Int, val acted: Boolean) {
    companion object {
        fun of(player: Player) =
            OtherPlayerView(player.name, player.vp, player.renown, player.hand.counts.values.sum(), player.acted)
    }
}

data class BattleLogEntryView(
    val pot: Int,
    val winner: String?,
    val armies: Map<String, ArmyResultView>,
    val duels: List<DuelView>,
) {
    companion object {
        fun of(result: BattleResult) = BattleLogEntryView(
            pot = result.pot,
            winner = result.winner,
            armies = result.armies.mapValues { (_, army) -> ArmyResultView.of(army) },
            duels = result.duels.map(DuelView::of),
        )
    }
}

data class DuelView(val players: List<String>, val strengths: Map<String, Int>) {
    companion object {
        fun of(duel: Duel) = DuelView(
            players = listOf(duel.first, duel.second),
            strengths = linkedMapOf(duel.first to duel.firstStrength, duel.second to duel.secondStrength),
        )
    }
}

data class ArmyResultView(
    val army: Map<CreatureType, Int>,
    val score: Int,
    val casualties: Map<CreatureType, Int>,
    val survivors: Map<CreatureType, Int>,
    val vpGained: Int,
    val renownGained: Boolean,
) {
    companion object {
        fun of(result: ArmyResult) = ArmyResultView(
            army = counts(result.army),
            score = result.score,
            casualties = counts(result.casualties),
            survivors = counts(result.survivors),
            vpGained = result.vpGained,
            renownGained = result.gainedRenown,
        )
    }
}

private fun counts(hand: Hand): Map<String, Int> = hand.counts.mapKeys { (card, _) -> card.name }.toSortedMap()

private fun counts(army: Army): Map<CreatureType, Int> = army.creatures.toSortedMap()
