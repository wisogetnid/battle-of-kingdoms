package org.battleofkingdoms.battle

import org.battleofkingdoms.cards.Artifact

/** One player's part in a battle; [army] is empty if they committed nothing. */
data class ArmyResult(
    val army: Army,
    val score: Int,
    val casualties: Army,
    val survivors: Army,
    val vpGained: Int,
)

/** One 1-vs-1 duel within a battle, with each side's strength against the other. */
data class Duel(
    val first: String,
    val second: String,
    val firstStrength: Int,
    val secondStrength: Int,
) {
    /** [player]'s strength minus their opponent's; 0 if they aren't in this duel. */
    fun margin(player: String): Int = when (player) {
        first -> firstStrength - secondStrength
        second -> secondStrength - firstStrength
        else -> 0
    }
}

/** What a battle's winner took: the VP, or an artifact. */
sealed interface Prize {
    data object VictoryPoint : Prize

    data class Taken(val artifact: Artifact) : Prize
}

/**
 * The outcome of one battle, which is also its battle log entry: [pot] is the VP it was worth,
 * [prize] what the winner took (`null` with no winner or while they're still choosing).
 */
data class BattleResult(
    val pot: Int = 0,
    val winner: String?,
    val armies: Map<String, ArmyResult>,
    val duels: List<Duel>,
    val prize: Prize? = null,
) {
    fun worth(pot: Int): BattleResult = copy(pot = pot)

    /** Records the winner's [prize]; only the VP counts towards their [ArmyResult.vpGained]. */
    fun awarded(prize: Prize): BattleResult = copy(
        prize = prize,
        armies = armies.mapValues { (name, army) ->
            if (name == winner && prize == Prize.VictoryPoint) army.copy(vpGained = pot) else army
        },
    )
}
